// Copyright 2020 The Regents of the University of California
// released under BSD 3-Clause License
// author: Kevin Laeufer <laeufer@cs.berkeley.edu>

package kiwi.formal

import kiwi.formal.backends._
import org.scalatest._
import chisel3._
import chiseltest.experimental.sanitizeFileName
import kiwi.formal.transforms.{CoverAllBranchesAnnotation, CoverBranchesTransform}
import firrtl.AnnotationSeq
import firrtl.options.{Dependency, TargetDirAnnotation}
import firrtl.stage.RunFirrtlTransformAnnotation

import java.io.File
import scala.util.DynamicVariable

/** uses symbi yosys as default backend */
trait SymbiYosysTester extends FormalTester { this: TestSuite =>
    override val defaultBackend: Option[() => Backend] = Some(Backend.symbiYosys)
}

trait BtormcTester extends FormalTester { this: TestSuite =>
    override val defaultBackend: Option[() => Backend] = Some(Backend.botrmc)
}

/** FormalTester trait for scalatest, based on code copied from chisel-test */
trait FormalTester extends Assertions with TestSuiteMixin {
    this: TestSuite =>

    /** override with Some backend to use by default */
    val defaultBackend: Option[() => Backend] = None

    /** convenience method to run bmc with default options and backend */
    def bmc(gen: => RawModule, depth: Int, annos: AnnotationSeq = Seq()): BoundedCheckResult = {
        run(BoundedCheck(depth), VerificationOptions(), gen, annos).asInstanceOf[BoundedCheckResult]
    }
    /** convenience method to run a bounded cover with default options and backend */
    def cover(gen: => RawModule, depth: Int, annos: AnnotationSeq = Seq()): BoundedCoverResult = {
        run(BoundedCover(depth), VerificationOptions(), gen, annos).asInstanceOf[BoundedCoverResult]
    }
    /** convenience method to run an unbounded proof with default options and backend */
    def prove(gen: => RawModule, annos: AnnotationSeq = Seq()): ProveResult = {
        run(Prove(), VerificationOptions(), gen, annos).asInstanceOf[ProveResult]
    }

    /** run a verification task */
    def run(op: VerificationOp, opts: VerificationOptions, gen: => RawModule, annos: AnnotationSeq = Seq(), backend: Option[() => Backend] = None): VerificationResult = {
        val engine = backend.getOrElse(defaultBackend.getOrElse(throw new RuntimeException("Need to specify a backend or defaultBackend!")))()
        val finalAnnos = addDefaultTargetDir(getTestName, annos)
        val dir = finalAnnos.collectFirst { case TargetDirAnnotation(d) => d }.get
        engine.run(dir, op, opts, () => gen, finalAnnos)
    }

    /** Annotation that runs the auto branch coverage transformation */
    val BranchCoverage = List(RunFirrtlTransformAnnotation(Dependency[CoverBranchesTransform]), CoverAllBranchesAnnotation)

    protected def getTestName: String = {
        val ctx = scalaTestContext.value.getOrElse(
            throw new RuntimeException("No test context found! Make sure you are in a unittest.")
        )
        sanitizeFileName(ctx.name)
    }

    // Provide test fixture data as part of 'global' context during test runs
    private val scalaTestContext = new DynamicVariable[Option[NoArgTest]](None)

    abstract override def withFixture(test: NoArgTest): Outcome = {
        require(scalaTestContext.value.isEmpty)
        scalaTestContext.withValue(Some(test)) {
            super.withFixture(test)
        }
    }

    /**
      * Will add a TargetDirAnnotation with defaultDir with "test_run_dir" path prefix to the annotations
      * if there is not a TargetDirAnnotation already present
      *
      * @param defaultDir     a default directory
      * @param annotationSeq  annotations to add it to, unless one is already there
      * @return
      */
    private def addDefaultTargetDir(defaultDir: String, annotationSeq: AnnotationSeq): AnnotationSeq = {
        if (annotationSeq.exists { x => x.isInstanceOf[TargetDirAnnotation] }) {
            annotationSeq
        } else {
            val target = TargetDirAnnotation("test_run_dir" + File.separator + defaultDir)
            AnnotationSeq(annotationSeq ++ Seq(target))
        }
    }
}
