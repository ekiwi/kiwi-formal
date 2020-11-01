package dank.formal.test.keepmax

import dank.formal._
import org.scalatest._
import chisel3._

/* This module is tested because it uses past values. */
class KeepMax(width: Int) extends CoveredFormalModule {
    val io = IO(new Bundle {
        val in = Input(UInt(width.W))
        val out = Output(UInt(width.W))
    })

    val maxReg = RegInit(0.U(width.W))
    io.out := maxReg

    when (io.in > maxReg) {
        maxReg := io.in
    }

    afterReset() {
        assert(io.out >= past(io.out))
    }
}

class KeepMaxTest extends FlatSpec with FormalTester {
    behavior of "KeepMax"

    cover(new KeepMax(16))
    prove(new KeepMax(16))
    bmc(new KeepMax(16))
}
