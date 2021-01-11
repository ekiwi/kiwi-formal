// Copyright 2020 The Regents of the University of California
// released under BSD 3-Clause License
// author: Kevin Laeufer <laeufer@cs.berkeley.edu>
package kiwi.formal.backends

import chisel3.RawModule
import firrtl.AnnotationSeq

/** Formal Backend */
trait Backend {
  /** human readable name of the backend engine */
  def name: String
  /** simple ASCII prefix for naming folders */
  def prefix: String
  /** run a verification operation (non reentrant!) */
  def run(directory: String, op: VerificationOp, opts: VerificationOptions, gen: () => RawModule, annos: AnnotationSeq): VerificationResult
}

sealed trait VerificationOp
case class BoundedCheck(depth: Int) extends VerificationOp
case class BoundedCover(depth: Int) extends VerificationOp
case class Prove() extends VerificationOp

case class VerificationOptions(timeout: Option[Int] = None)

sealed trait VerificationResult {
  def throwErrors(): Unit
}

case class VerificationFail(errors: Iterable[String]) extends VerificationResult with BoundedCheckResult with BoundedCoverResult with ProveResult {
  assert(errors.nonEmpty)
  override def throwErrors(): Unit = {
    throw new VerificationException(errors.mkString("\n"))
  }
}

case object VerificationSuccess extends VerificationResult with BoundedCheckResult with BoundedCoverResult with ProveResult {
  override def throwErrors(): Unit = {}
}

class VerificationException(val message: String) extends Exception(message)


sealed trait BoundedCheckResult extends VerificationResult
sealed trait BoundedCoverResult extends VerificationResult
sealed trait ProveResult extends VerificationResult

object Backend {
  def symbiYosys(): Backend = new SymbiYosys()
  def botrmc(): Backend = new Maltese(new maltese.mc.BtormcModelChecker())
}
