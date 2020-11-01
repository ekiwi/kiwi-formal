package dank.formal

import dank.formal.sby._
import org.scalatest._
import scala.reflect._

trait FormalTester { this: FlatSpec =>
    def cover[T<:FormalModule](gen: => T, depth: Int = 20)(implicit c: ClassTag[T]) {
        it must "be covered" in {
            new SbyRun(gen, "cover", depth, this.suiteName).throwErrors()
        }
    }

    def prove[T<:FormalModule](gen: => T, depth: Int = 20)(implicit c: ClassTag[T]) {
        it must "be proven" in {
            new SbyRun(gen, "prove", depth, this.suiteName).throwErrors()
        }
    }

    def bmc[T<:FormalModule](gen: => T, depth: Int = 20)(implicit c: ClassTag[T]) {
        it must ("pass bounded model check with depth " + depth.toString) in {
            new SbyRun(gen, "bmc", depth, this.suiteName).throwErrors()
        }
    }
}