package dank.formal

import dank.formal.transforms._
import firrtl.Transform
import chisel3._
import chisel3.util.ShiftRegister
import chisel3.util.HasBlackBoxInline
import chisel3.experimental.{ChiselAnnotation, RunFirrtlTransform, annotate, verification}
import chisel3.internal.sourceinfo.SourceInfo
import firrtl.annotations.PresetAnnotation

/** Add cover statements to branches.
  * 
  * Cover statements will not be added to branches that contain only Verification, Print, and Stop
  * statements to avoid covering normal Chisel assertsion.
  * See DoNotCoverBranches, which can be used to disable this.
  */
trait CoverBranches { m: chisel3.Module =>
    chisel3.experimental.annotate(new ChiselAnnotation with RunFirrtlTransform {
        def toFirrtl = CoverBranchesAnnotation(m.toNamed)
        def transformClass: Class[_ <: Transform] = classOf[CoverBranchesTransform]
    })
}

/** Do not add cover statements to branches.
  * 
  * This trait disables CoverBranches. If a module is annotated with both, cover statements will not
  * be added.
  */
trait DoNotCoverBranches { m: chisel3.Module =>
    chisel3.experimental.annotate(new ChiselAnnotation {
        def toFirrtl = DoNotCoverBranchesAnnotation(m.toNamed)
    })
}

/** Chisel Module with formal verification. */
abstract class FormalModule extends Module {
    val _reset_detector = Module(new ResetDetector)
    val _reset_done = dontTouch(WireInit(_reset_detector.resetDone))

    /** Add an assert verification statement. */
    def assert(predicate: Bool, msg: String = "")(implicit sourceInfo: SourceInfo, compileOptions: CompileOptions): Unit = {
        when (_reset_done) {
            verification.assert(predicate, msg)
        }
    }

    /** Add a cover verification statement. */
    def cover(predicate: Bool, msg: String = "")(implicit sourceInfo: SourceInfo, compileOptions: CompileOptions): Unit = {
        when (_reset_done) {
            verification.cover(predicate, msg)
        }
    }

    /** Add an assume verification statement. */
    def assume(predicate: Bool, msg: String = "")(implicit sourceInfo: SourceInfo, compileOptions: CompileOptions): Unit = {
        when (_reset_done) {
            verification.assume(predicate, msg)
        }
    }

    /** Create a block that will only apply after at least n cycles passed after a reset. */
    def afterReset(n: Int = 1)(block: => Any)(implicit sourceInfo: SourceInfo, compileOptions: CompileOptions): WhenContext = {
        val counter = RegInit(n.U)
        when (_reset_done && (counter > 0.U)) {
            counter := counter - 1.U;
        }

        when(_reset_done && (counter === 0.U))(block)
    }

    /** Get a past value of a signal.
      * This method ignores the reset, so if cycles is more than the cycles since the reset, you may get old or
      * undefined values. Use with caution
      */
    def past[T <: Data](signal: T, cycles: Int = 1)(implicit sourceInfo: SourceInfo, compileOptions: CompileOptions): T = {
        ShiftRegister(signal, cycles)
    }

    /** Get a pas value of a signal, or the default value.
      * Unlike the other "past" method, this one checks the number of cycles since reset and returns a default value if
      * there was a reset between now and the past value requested.
      */
    def past[T <: Data](signal: T, default: T, cycles: Int)(implicit sourceInfo: SourceInfo, compileOptions: CompileOptions): T = {
        val r = WireInit(default)
        afterReset(cycles) {
            r := ShiftRegister(signal, cycles)
        }
        r
    }
}


class ResetDetector extends MultiIOModule {
  val resetDone = IO(Output(Bool()))
  val preset = WireInit(false.B.asAsyncReset())
  annotate(new ChiselAnnotation {
    override def toFirrtl = PresetAnnotation(preset.toTarget)
  })
  withReset(preset) {
    val resetDoneReg = RegInit(false.B)
    resetDone := resetDoneReg
    when(reset.asBool()) {
      resetDoneReg := true.B
    }
  }
}

abstract class CoveredFormalModule extends FormalModule with CoverBranches