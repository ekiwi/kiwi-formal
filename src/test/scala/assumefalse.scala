package dank.formal.test.assumefalse

import dank.formal._
import org.scalatest._
import chisel3._

class AssumeFalseModule extends CoveredFormalModule {
    val io = IO(new Bundle {
        val in = Input(Bool())
        val a = Output(Bool())
    })

    io.a := DontCare
    when (io.in) {
        io.a := true.B
    }.otherwise {
        assume(false.B)
    }
}

class Smoketest extends FlatSpec with FormalTester {
    behavior of "AssumeFalseModule"

    cover(new AssumeFalseModule)
}