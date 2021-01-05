package dank.formal

import org.scalatest._
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

class Smoketest extends FlatSpec with FormalTester {
    behavior of "SmoketestModule"

    cover(new SmoketestModule)
    prove(new SmoketestModule)
    bmc(new SmoketestModule)
}