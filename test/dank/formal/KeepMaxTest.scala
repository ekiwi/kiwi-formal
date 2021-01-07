package dank.formal

import org.scalatest.flatspec.AnyFlatSpec
import chisel3._
import dank.formal.transforms.CoverAllBranchesAnnotation

/* This module is tested because it uses past values. */
class KeepMax(width: Int) extends Module {
    val io = IO(new Bundle {
        val in = Input(UInt(width.W))
        val out = Output(UInt(width.W))
    })

    val maxReg = RegInit(0.U(width.W))
    io.out := maxReg

    when (io.in > maxReg) {
        maxReg := io.in
    }
}

class KeepMaxSpec(dut: KeepMax) extends Spec(dut) {
    afterReset() {
        assert(dut.io.out >= past(dut.io.out))
    }
}

class KeepMaxTest extends AnyFlatSpec with SymbiYosysTester {
    behavior of "KeepMax"

    val annos = List(VerificationAspect[KeepMax](new KeepMaxSpec(_))) ++ BranchCoverage

    /* Run multiple tests as a regression test for #1. */
    for (i <- 1 until 16) {
        it should s"work for i = $i" in {
            cover(new KeepMax(i), 20, annos).throwErrors()
            prove(new KeepMax(i), annos).throwErrors()
            bmc(new KeepMax(i), 20, annos).throwErrors()
        }
    }
}
