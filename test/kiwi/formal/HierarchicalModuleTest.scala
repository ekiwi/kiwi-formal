// Copyright 2020 The Regents of the University of California
// released under BSD 3-Clause License
// author: Kevin Laeufer <laeufer@cs.berkeley.edu>

package kiwi.formal

import chisel3._
import kiwi.formal.backends.{VerificationFail, VerificationSuccess}
import org.scalatest.flatspec.AnyFlatSpec

class FsmWrapper extends Module {
  val in = IO(Input(Bool()))
  val out = IO(Output(Bool()))
  val fsm = Module(new SimpleFSM)
  fsm.in := in
  out := fsm.out
}

class FsmWrapperInputZeroAssumption(dut: FsmWrapper) extends Spec(dut) {
  assume(!dut.in)
}

/** Makes sure that assert and cover statements are not affected when a module is wrapped in another module.
  * Assumes in non-toplevel module will be automatically turned into asserts by the firrtl compiler.
  */
class HierarchicalModuleTest extends AnyFlatSpec with SymbiYosysTester {
  behavior of "SimpleFSM wrapped in a Module"

  def withSpec(spec: SimpleFSM => Spec[SimpleFSM]) = List(VerificationAspect[SimpleFSM](spec(_)))

  it should "cover all but one state" in {
    cover(new FsmWrapper(), 20, withSpec(new FSMStateCover(_))) match {
      case v : VerificationFail =>
        assert(v.errors.size == 1, "expected exactly one error")
      case VerificationSuccess =>
        assert(false, "expected this test to fail!")
    }
  }

  it should "only get stuck in state 2" in {
    cover(new FsmWrapper(), 20, withSpec(new FSMStateSequenceCover(_))) match {
      case v : VerificationFail =>
        assert(v.errors.size == 1, "expected exactly one error")
      case VerificationSuccess =>
        assert(false, "expected this test to fail!")
    }
  }

  it should "ensure that a and b are never true for two consecutive cycles" in {
    bmc(new FsmWrapper(), depth = 20, withSpec(new FSMInputOutputAssert(_))).throwErrors()
  }

  it should "allow the output to be 0" in {
    bmc(new FsmWrapper(), 20, withSpec(new FSMOutputAlwaysOne(_))) match {
      case v : VerificationFail =>
        assert(v.errors.size == 1, "expected exactly one error")
      case VerificationSuccess =>
        assert(false, "expected this test to fail!")
    }
  }

  it should "always output one if the input is held low" in {
    // here the assumption in the submodule will be automatically converted,
    // thus we need to add an assumption to the toplevel
    val annos = withSpec(new FSMOutputAlwaysOneAssumingInputIsZero(_)) :+
      VerificationAspect[FsmWrapper](new FsmWrapperInputZeroAssumption(_))
    bmc(new FsmWrapper(), depth = 20, annos).throwErrors()
  }
}