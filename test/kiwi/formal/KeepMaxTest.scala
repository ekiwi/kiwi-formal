// Copyright 2020 The Regents of the University of California
// released under BSD 3-Clause License
// author: Kevin Laeufer <laeufer@cs.berkeley.edu>
// based on code from dank-formal:
// Copyright 2020 Daniel Kasza
// released under Apache License 2.0

package kiwi.formal

import org.scalatest.flatspec.AnyFlatSpec
import chisel3._

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
