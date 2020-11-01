package dank.formal.test.assertionfailure

import dank.formal._
import dank.formal.sby._
import org.scalatest._
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

class AssertionFailureTest extends FlatSpec {
    behavior of "SbyRun"

    it should "detect assertion failures when proving" in {
        try {
            new SbyRun(new ModuleWithBadAssertion, "prove").throwErrors()
            assert(false)
        } catch {
            case e: SbyException => assert(e.message.contains("assertionfailure.scala"))
        }
    }

    it should "detect assertion failures when covering" in {
        try {
            new SbyRun(new ModuleWithBadAssertion, "cover").throwErrors()
            assert(false)
        } catch {
            case e: SbyException => assert(e.message.contains("assertionfailure.scala"))
        }
    }

    it should "detect assertion failures when running bmc" in {
        try {
            new SbyRun(new ModuleWithBadAssertion, "bmc").throwErrors()
            assert(false)
        } catch {
            case e: SbyException => assert(e.message.contains("assertionfailure.scala"))
        }
    }
}