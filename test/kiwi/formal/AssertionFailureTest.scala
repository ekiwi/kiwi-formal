// Copyright 2020 The Regents of the University of California
// released under BSD 3-Clause License
// author: Kevin Laeufer <laeufer@cs.berkeley.edu>
// based on code from dank-formal:
// Copyright 2020 Daniel Kasza
// released under Apache License 2.0

package kiwi.formal

import kiwi.formal.backends._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3._

class ModuleAB extends Module {
    val io = IO(new Bundle {
        val in = Input(Bool())
        val a = Output(Bool())
        val b = Output(Bool())
    })

    val aReg = RegInit(true.B)
    val bReg = RegInit(false.B)
    io.a := aReg
    io.b := bReg

    when (io.in) {
        aReg := true.B
        bReg := false.B
    }.otherwise {
        aReg := false.B
        bReg := true.B
    }
}

class FailingAssertions(dut: ModuleAB) extends Spec(dut) {
    cover(dut.aReg)
    cover(dut.bReg)
    assert(dut.aReg && dut.bReg)
}

class AssertionFailureTest extends AnyFlatSpec with SymbiYosysTester {
    behavior of "SbyRun"

    val annos = List(VerificationAspect[ModuleAB](new FailingAssertions(_)))

    it should "detect assertion failures when proving" in {
        try {
            prove(new ModuleAB, annos).throwErrors()
            assert(false)
        } catch {
            case e: VerificationException => assert(e.message.contains("AssertionFailureTest.scala"))
        }
    }

    it should "detect assertion failures when covering" in {
        try {
            cover(new ModuleAB, 20, annos).throwErrors()
            assert(false)
        } catch {
            case e: VerificationException => assert(e.message.contains("AssertionFailureTest.scala"))
        }
    }

    it should "detect assertion failures when running bmc" in {
        try {
            bmc(new ModuleAB, 20, annos).throwErrors()
            assert(false)
        } catch {
            case e: VerificationException => assert(e.message.contains("AssertionFailureTest.scala"))
        }
    }
}
