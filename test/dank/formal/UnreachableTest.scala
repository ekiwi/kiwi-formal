package dank.formal

import org.scalatest.flatspec.AnyFlatSpec
import chisel3._
import dank.formal.backends.SbyException

class ModuleWithUnreachableStatements extends CoveredFormalModule {
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

    it should "detect uncovered branches" in {
        try {
            cover(new ModuleWithUnreachableStatements, 20).throwErrors()
            assert(false)
        } catch {
            case e: SbyException => assert(e.message.contains("UnreachableTest.scala"))
        }
    }
}