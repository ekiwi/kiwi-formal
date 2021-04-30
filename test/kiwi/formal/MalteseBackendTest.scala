package kiwi.formal

import org.scalatest.flatspec.AnyFlatSpec
import chisel3._
import kiwi.formal.backends.VerificationException


class SimpleCombinatorial extends Module {
  val a = IO(Input(UInt(8.W)))
  val b = IO(Input(UInt(8.W)))
  val sum = a + b
  val aGreaterB = a > b
}

class SimpleCombinatorialSpec(dut: SimpleCombinatorial) extends Spec(dut) {
  // working around async reset bug in smt backend for now...
  chisel3.experimental.verification.assume(dut.aGreaterB)
  chisel3.experimental.verification.assert(dut.sum > dut.b)
}

class MalteseBackendTest extends AnyFlatSpec with BtormcTester {
  behavior of "SimpleCombinatorialSpec"

  val annos = List(VerificationAspect[SimpleCombinatorial](new SimpleCombinatorialSpec(_)))

  it should "fail" in {
    assertThrows[VerificationException] {
      bmc(new SimpleCombinatorial(), 20, annos).throwErrors()
    }
  }
}
