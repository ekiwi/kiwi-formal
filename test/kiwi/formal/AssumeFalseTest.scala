// Copyright 2020 The Regents of the University of California
// released under BSD 3-Clause License
// author: Kevin Laeufer <laeufer@cs.berkeley.edu>
// based on code from dank-formal:
// Copyright 2020 Daniel Kasza
// released under Apache License 2.0

package kiwi.formal

import org.scalatest.flatspec.AnyFlatSpec
import chisel3._
import chisel3.experimental.verification
import kiwi.formal.transforms.CoverAllBranchesAnnotation

class AssumeFalseModule extends Module {
    val io = IO(new Bundle {
        val in = Input(Bool())
        val a = Output(Bool())
    })

    io.a := DontCare
    when (io.in) {
        io.a := true.B
    }.otherwise {
        verification.assume(false.B)
    }
}

class AssumeFalseTest extends AnyFlatSpec with SymbiYosysTester {
    behavior of "AssumeFalseModule"

    val annos = BranchCoverage

    it should "cover" in {
        cover(new AssumeFalseModule, 20, annos)
    }
}
