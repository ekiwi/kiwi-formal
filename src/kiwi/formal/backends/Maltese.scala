// Copyright 2020 The Regents of the University of California
// released under BSD 3-Clause License
// author: Kevin Laeufer <laeufer@cs.berkeley.edu>

package kiwi.formal.backends

import chisel3.RawModule
import firrtl.AnnotationSeq
import firrtl.backends.experimental.smt.ExpressionConverter
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

    val elaborated = Elaboration.elaborate(gen, annos, emitter = "experimental-btor2")
    val sys = ExpressionConverter.toMaltese(elaborated.annos).get

    op match {
      case BoundedCheck(depth) =>
        val checkerFile = directory + File.separator + elaborated.name + checker.fileExtension
        checker.check(sys, kMax = depth, fileName=Some(checkerFile)) match {
          case ModelCheckFail(witness) =>
            val sim = new TransitionSystemSimulator(sys)
            val vcdFile = directory + File.separator + elaborated.name + ".vcd"
            sim.run(witness, Some(vcdFile))
            val errors = List(s"Failed to verify ${elaborated.name}.\n$vcdFile")
            VerificationFail(errors)
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
