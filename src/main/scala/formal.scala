package dank.formal

import dank.formal.transforms._

import firrtl.Transform
import chisel3._
import chisel3.util.HasBlackBoxInline
import chisel3.experimental.verification
import chisel3.experimental.ChiselAnnotation
import chisel3.experimental.RunFirrtlTransform

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
    // TODO: add blackbox with _reset_done register.
    val _reset_detector = Module(new ResetDetector)
    _reset_detector.io.clock := clock
    _reset_detector.io.reset := reset
    val _reset_done = dontTouch(WireInit(_reset_detector.io.reset_done))

    def assert(predicate: Bool, msg: String = ""): Unit = {
        when (_reset_done) {
            verification.assert(predicate, msg)
        }
    }

    def cover(predicate: Bool, msg: String = ""): Unit = {
        when (_reset_done) {
            verification.cover(predicate, msg)
        }
    }

    def assume(predicate: Bool, msg: String = ""): Unit = {
        when (_reset_done) {
            verification.assume(predicate, msg)
        }
    }

    def afterReset(n: Int = 1)(block: => Any) {
        val counter = RegInit(n.U)
        when (_reset_done) {
            when (counter > 0.U) {
                counter := counter - 1.U;
            }.otherwise(block)
        }
    }

    // TODO: past
}

class ResetDetectorIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Reset())
  val reset_done = Output(Bool())
}

class ResetDetector extends BlackBox with HasBlackBoxInline {
  val io = IO(new ResetDetectorIO)
  setInline("ResetDetector.sv",
    s"""module ResetCounter(
       |  input clock,
       |  input reset,
       |  output reset_done
       |);
       |  reg _reset_done = 0;
       |  reset_done = _reset_done;
       |
       |  always @(posedge clock) begin
       |    if (reset) begin
       |      _reset_done <= 1;
       |    end
       |  end
       |endmodule
       |""".stripMargin)
}

abstract class CoveredFormalModule extends FormalModule with CoverBranches