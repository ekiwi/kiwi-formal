package dank.formal

import dank.formal.sby._
import org.scalatest._
import scala.reflect._

/** Trait used to simplify running formal verification. */
trait FormalTester { this: FlatSpec =>
    /** Generate traces to reach all cover statement. */
    def cover[T<:FormalModule](gen: => T, depth: Int = 20)(implicit c: ClassTag[T]) {
        it must "be covered" in {
            new SbyRun(gen, "cover", depth, this.suiteName+"_").throwErrors()
        }
    }

    /** Unbounded model check to verify assertions. */
    def prove[T<:FormalModule](gen: => T, depth: Int = 20)(implicit c: ClassTag[T]) {
        it must "be proven" in {
            new SbyRun(gen, "prove", depth, this.suiteName+"_").throwErrors()
        }
    }

    /** Bounded model check to verify assertions. */
    def bmc[T<:FormalModule](gen: => T, depth: Int = 20)(implicit c: ClassTag[T]) {
        it must ("pass bounded model check with depth " + depth.toString) in {
            new SbyRun(gen, "bmc", depth, this.suiteName+"_").throwErrors()
        }
    }
}