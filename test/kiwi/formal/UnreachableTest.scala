// Copyright 2020 The Regents of the University of California
// released under BSD 3-Clause License
// author: Kevin Laeufer <laeufer@cs.berkeley.edu>
// based on code from dank-formal:
// Copyright 2020 Daniel Kasza
// released under Apache License 2.0

package kiwi.formal

import org.scalatest.flatspec.AnyFlatSpec
import chisel3._
import kiwi.formal.backends.VerificationException


class ModuleWithUnreachableStatements extends Module {
    val io = IO(new Bundle {
        val in = Input(Bool())
        val out = Output(Bool())
    })

    when (io.in) {
        io.out := false.B
    }.elsewhen(!io.in) {
        io.out := true.B
    }.otherwise {
        /* Unreachable. */
        io.out := false.B
    }
}

class UnreachableTest extends AnyFlatSpec with SymbiYosysTester {
    behavior of "SbyRun"

    val annos = BranchCoverage

    it should "detect uncovered branches" in {
        try {
            cover(new ModuleWithUnreachableStatements, 20, annos).throwErrors()
            assert(false)
        } catch {
            case e: VerificationException => assert(e.message.contains("UnreachableTest.scala"))
        }
    }
}
