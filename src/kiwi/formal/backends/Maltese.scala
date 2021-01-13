// Copyright 2020 The Regents of the University of California
// released under BSD 3-Clause License
// author: Kevin Laeufer <laeufer@cs.berkeley.edu>

package kiwi.formal.backends

import chisel3.RawModule
import firrtl.AnnotationSeq
import firrtl.backends.experimental.smt.ExpressionConverter
import firrtl.options.Dependency
import firrtl.passes.InlineInstances
import firrtl.stage.RunFirrtlTransformAnnotation
import maltese.mc._

import java.io.File

/** Formal Verification based on the firrtl compiler's SMT backend and the maltese SMT libraries solver bindings.  */
class Maltese(checker: IsModelChecker) extends Backend {
  override def name = checker.name
  override def prefix = checker.prefix

  /** run a verification operation (non reentrant!) */
  override def run(directory: String, op: VerificationOp, opts: VerificationOptions, gen: () => RawModule, annos: AnnotationSeq) = {
    op match {
      case BoundedCover(_) => throw new NotImplementedError("Maltese provided backends currently do not support coverage!")
      case _ =>
    }

    // The firrtl SMT backend expects all submodules that are part of the implementation to be inlined.
    // Any DoNotInline annotation from the `state.annotations` will prevent that particular module to be inlined.
    val doFlatten = Seq(RunFirrtlTransformAnnotation(Dependency(FlattenPass)),
      RunFirrtlTransformAnnotation(Dependency[InlineInstances]))
    val elaborated = Elaboration.elaborate(gen, annos ++ doFlatten, emitter = "experimental-btor2")
    val (sys, comments) = ExpressionConverter.toMaltese(elaborated.annos).get

    op match {
      case BoundedCheck(depth) =>
        val checkerFile = directory + File.separator + elaborated.name + checker.fileExtension
        checker.check(sys, kMax = depth, fileName=Some(checkerFile)) match {
          case ModelCheckFail(witness) =>
            val sim = new TransitionSystemSimulator(sys)
            val vcdFile = directory + File.separator + elaborated.name + ".vcd"
            sim.run(witness, Some(vcdFile))
            val msg = s"Failed to verify ${elaborated.name} with ${checker.name}"
            val error = Error("", List(), Some(vcdFile))
            VerificationFail(msg, List(error))
          case ModelCheckSuccess() =>
            VerificationSuccess
        }

      case BoundedCover(depth) =>
        throw new NotImplementedError("TODO: implement cover")
      case Prove() =>
        throw new NotImplementedError("TODO: k-induction proof")
    }
  }
}
