package dank.formal

import dank.formal.sby._
import org.scalatest._
import scala.reflect._

/** Trait used to simplify running formal verification. */
trait FormalTester { this: FlatSpec =>
    /** Counter used to give each test a unique name. */
    private var counter = 0

    /** Generate basename for new test. */
    private def getTestName() = {
        val name = this.suiteName + ":" + counter.toString
        counter += 1
        name
    }

    /** Generate traces to reach all cover statement. */
    def cover[T<:FormalModule](gen: => T, depth: Int = 20)(implicit c: ClassTag[T]) {
        val name = getTestName()
        it must ("be covered - " + name) in {
            new SbyRun(gen, "cover", depth, name + "_").throwErrors()
        }
    }

    /** Unbounded model check to verify assertions. */
    def prove[T<:FormalModule](gen: => T, depth: Int = 20)(implicit c: ClassTag[T]) {
        val name = getTestName()
        it must ("be proven - " + name) in {
            new SbyRun(gen, "prove", depth, name + "_").throwErrors()
        }
    }

    /** Bounded model check to verify assertions. */
    def bmc[T<:FormalModule](gen: => T, depth: Int = 20)(implicit c: ClassTag[T]) {
        val name = getTestName()
        it must ("pass bounded model check with depth " + depth.toString + " - " + name) in {
            new SbyRun(gen, "bmc", depth, name + "_").throwErrors()
        }
    }
}