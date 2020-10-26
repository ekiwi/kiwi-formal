package dank.formal

import dank.formal.transforms._

import firrtl.Transform
import chisel3.experimental.ChiselAnnotation
import chisel3.experimental.RunFirrtlTransform

/** Add cover statements to branches.
  * 
  * Cover statements will not be added to branches that contain only Verification, Print, and Stop
  * statements to avoid covering normal Chisel assertsion.
  * See DoNotCoverBranches, which can be used to disable this.
  */
trait CoverBranches { m: chisel3.Module =>
    chisel3.experimental.annotate(new ChiselAnnotation with RunFirrtlTransform {
        def toFirrtl = CoverBranchesAnnotation(m.toNamed)
        def transformClass: Class[_ <: Transform] = classOf[CoverBranchesTransform]
    })
}

/** Do not add cover statements to branches.
 * 
 * This trait disables CoverBranches. If a module is annotated with both, cover statements will not
 * be added.
 */
trait DoNotCoverBranches { m: chisel3.Module =>
    chisel3.experimental.annotate(new ChiselAnnotation {
        def toFirrtl = DoNotCoverBranchesAnnotation(m.toNamed)
    })
}
