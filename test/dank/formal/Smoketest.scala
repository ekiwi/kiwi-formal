package dank.formal

import org.scalatest.flatspec.AnyFlatSpec
import chisel3._

class SmoketestModule extends FormalModule {
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
    assert(aReg ^ bReg)
}

class Smoketest extends AnyFlatSpec with SymbiYosysTester {
    behavior of "SmoketestModule"

    it should "not smoke" in {
        cover(new SmoketestModule, 20)
        prove(new SmoketestModule)
        bmc(new SmoketestModule, 20)
    }
}