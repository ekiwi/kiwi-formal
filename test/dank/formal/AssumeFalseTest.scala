package dank.formal

import org.scalatest.flatspec.AnyFlatSpec
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

class AssumeFalseTest extends AnyFlatSpec with SymbiYosysTester {
    behavior of "AssumeFalseModule"

    it should "cover" in {
        cover(new AssumeFalseModule, 20)
    }
}