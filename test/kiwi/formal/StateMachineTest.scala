package kiwi.formal

import chisel3._
import chisel3.internal.sourceinfo.SourceInfo
import chisel3.util._
import kiwi.formal.backends.{VerificationFail, VerificationSuccess}
import org.scalatest.flatspec.AnyFlatSpec

class SimpleFSM extends MultiIOModule {
  val a = IO(Input(Bool()))
  val b = IO(Output(Bool()))
  b := false.B

  val state = RegInit(0.U(2.W))

  switch(state) {
    is(0.U) {
      state := Mux(a, 1.U, 0.U)
      b := !a
    }
    is(1.U) {
      state := Mux(a, 2.U, 0.U)
      b := a
    }
  }
}

class FSMStateCover(f: SimpleFSM) extends Spec(f) {
  cover(f.state === 0.U)
  cover(f.state === 1.U)
  cover(f.state === 2.U)
  cover(f.state === 3.U)
}

class FSMStateSequenceCover(f: SimpleFSM) extends Spec(f) {
  val hasVisited = Seq.tabulate(3) { ii =>
    val r = RegInit(false.B)
    when(f.state === ii.U) { r := true.B }
    r
  }
  def visitAfter(a: Int, b: Int)(implicit sourceInfo: SourceInfo): Unit = {
    cover((f.state === a.U) && hasVisited(b), s"state == $a after $b has been visited")
  }
  visitAfter(0, 1)
  visitAfter(1, 0)
  visitAfter(2, 0)
  visitAfter(2, 1)
  visitAfter(0, 2) // this is expected to fail since the FSM is stuck in state 2
}
class FSMInputOutputAssert(f: SimpleFSM) extends Spec(f) {
  // a && b may never be true for two consecutive cycles
  assert(!(past(f.a && f.b) && (f.a && f.b)))
}

class FSMOutputAlwaysOne(f: SimpleFSM) extends Spec(f) {
  assert(f.b)
}

class FSMOutputAlwaysOneAssumingInputIsZero(f: SimpleFSM) extends Spec(f) {
  assume(!f.a)
  assert(f.b)
}

class StateMachineTest extends AnyFlatSpec with SymbiYosysTester {
  behavior of "SimpleFSM"

  def withSpec(spec: SimpleFSM => Spec[SimpleFSM]) = List(VerificationAspect[SimpleFSM](spec(_)))

  it should "cover all but one state" in {
    cover(new SimpleFSM(), 20, withSpec(new FSMStateCover(_))) match {
      case v : VerificationFail =>
        assert(v.errors.size == 1, "expected exactly one error")
      case VerificationSuccess =>
        assert(false, "expected this test to fail!")
    }
  }

  it should "only get stuck in state 2" in {
    cover(new SimpleFSM(), 20, withSpec(new FSMStateSequenceCover(_))) match {
      case v : VerificationFail =>
        println(v.report)
        assert(v.errors.size == 1, "expected exactly one error")
      case VerificationSuccess =>
        assert(false, "expected this test to fail!")
    }
  }

  it should "ensure that a and b are never true for two consecutive cycles" in {
    bmc(new SimpleFSM(), depth = 20, withSpec(new FSMInputOutputAssert(_))).throwErrors()
  }

  it should "allow the output to be 0" in {
    bmc(new SimpleFSM(), 20, withSpec(new FSMOutputAlwaysOne(_))) match {
      case v : VerificationFail =>
        println(v.report)
        assert(v.errors.size == 1, "expected exactly one error")
      case VerificationSuccess =>
        assert(false, "expected this test to fail!")
    }
  }

  it should "always output one if the input is held low" in {
    bmc(new SimpleFSM(), depth = 20, withSpec(new FSMOutputAlwaysOneAssumingInputIsZero(_))).throwErrors()
  }
}
