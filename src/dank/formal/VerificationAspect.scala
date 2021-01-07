// Copyright 2020 The Regents of the University of California
// released under BSD 3-Clause License
// author: Kevin Laeufer <laeufer@cs.berkeley.edu>

package dank.formal

import chisel3._
import chisel3.aop.injecting._
import chisel3.experimental.{ChiselAnnotation, annotate, verification}
import chisel3.internal.sourceinfo.SourceInfo
import chisel3.util.ShiftRegister
import firrtl.annotations.PresetAnnotation


abstract class Spec[M <: MultiIOModule](dut: M) {
  /** signal that indicates when the circuit has gone through reset */
  lazy val resetDone: Bool = {
    val preset = WireInit(false.B.asAsyncReset())
    annotate(new ChiselAnnotation {
      override def toFirrtl = PresetAnnotation(preset.toTarget)
    })
    withReset(preset) {
      val resetDoneReg = RegInit(false.B)
      when(dut.reset.asBool()) {
        resetDoneReg := true.B
      }
      resetDoneReg
    }
  }

  /** Add an assert verification statement. */
  def assert(predicate: Bool, msg: String = "")(implicit sourceInfo: SourceInfo, compileOptions: CompileOptions): Unit = {
    when (resetDone) {
      verification.assert(predicate, msg)
    }
  }

  /** Add a cover verification statement. */
  def cover(predicate: Bool, msg: String = "")(implicit sourceInfo: SourceInfo, compileOptions: CompileOptions): Unit = {
    when (resetDone) {
      verification.cover(predicate, msg)
    }
  }

  /** Add an assume verification statement. */
  def assume(predicate: Bool, msg: String = "")(implicit sourceInfo: SourceInfo, compileOptions: CompileOptions): Unit = {
    when (resetDone) {
      verification.assume(predicate, msg)
    }
  }

  /** Create a block that will only apply after at least n cycles passed after a reset. */
  def afterReset(n: Int = 1)(block: => Any)(implicit sourceInfo: SourceInfo, compileOptions: CompileOptions): WhenContext = {
    val counter = RegInit(n.U)
    when (resetDone && (counter > 0.U)) {
      counter := counter - 1.U;
    }

    when(resetDone && (counter === 0.U))(block)
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



/** Adds assertions and assumptions to a Chisel circuit
  *
  * This is a very basic implementation that only supports writing assertions over a single module at a time.
  * It also lacks any checks to ensure that the circuit is only observed and not modified.
  * */
case class VerificationAspect[M <: MultiIOModule](spec: M => Spec[M]) extends InjectorAspect[M, M](
  { dut: M => List(dut) },
  { dut: M => spec(dut) }
)