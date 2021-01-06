package dank.formal

import dank.formal.backends._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3._

class ModuleWithBadAssertion extends FormalModule {
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

    cover(aReg)
    cover(bReg)
    assert(aReg && bReg)
}

class AssertionFailureTest extends AnyFlatSpec with SymbiYosysTester {
    behavior of "SbyRun"

    it should "detect assertion failures when proving" in {
        try {
            prove(new ModuleWithBadAssertion).throwErrors()
            assert(false)
        } catch {
            case e: SbyException => assert(e.message.contains("AssertionFailureTest.scala"))
        }
    }

    it should "detect assertion failures when covering" in {
        try {
            cover(new ModuleWithBadAssertion, 20).throwErrors()
            assert(false)
        } catch {
            case e: SbyException => assert(e.message.contains("AssertionFailureTest.scala"))
        }
    }

    it should "detect assertion failures when running bmc" in {
        try {
            bmc(new ModuleWithBadAssertion, 20).throwErrors()
            assert(false)
        } catch {
            case e: SbyException => assert(e.message.contains("AssertionFailureTest.scala"))
        }
    }
}