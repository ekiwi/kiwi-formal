// Copyright 2020 The Regents of the University of California
// released under BSD 3-Clause License
// author: Kevin Laeufer <laeufer@cs.berkeley.edu>

package kiwi.formal.backends

import firrtl.analyses.InstanceKeyGraph
import firrtl.analyses.InstanceKeyGraph.InstanceKey
import firrtl.annotations._
import firrtl.options.Dependency
import firrtl.passes.InlineAnnotation
import firrtl.stage.Forms
import firrtl.{AnnotationSeq, DependencyAPIMigration, Transform}

case class DoNotInlineAnnotation(target: ModuleTarget) extends SingleTargetAnnotation[ModuleTarget] {
  override def duplicate(n: ModuleTarget) = copy(target = n)
}

/** Annotates the complete hierarchy to be flattened. */
object FlattenPass extends Transform with DependencyAPIMigration {
  override def prerequisites = Forms.WorkingIR
  // this pass relies on modules not being dedupped yet
  override def optionalPrerequisiteOf = Seq(Dependency[firrtl.transforms.DedupModules])
  override def invalidates(a: Transform): Boolean = false

  override protected def execute(state: firrtl.CircuitState): firrtl.CircuitState = {
    val doNotInline = state.annotations
      .collect{ case DoNotInlineAnnotation(target) if target.circuit == state.circuit.main => target.module }
    val iGraph = InstanceKeyGraph(state.circuit)
    val children = iGraph.getChildInstances.toMap

    // we tag every module to be inlined unless it is explicitly marked as doNotInline
    val cRef = CircuitTarget(state.circuit.main)
    val main = cRef.module(state.circuit.main)
    val inlineAnnos = inlines(main)(children, doNotInline.toSet)

    val annos = state.annotations.filterNot(_.isInstanceOf[DoNotInlineAnnotation]) ++ inlineAnnos
    state.copy(annotations = annos)
  }

  private def inlines(m: ModuleTarget)(implicit children: Map[String, Seq[InstanceKey]], doNotInline: Set[String]): AnnotationSeq = {
    if(doNotInline.contains(m.module)) { Seq() } else {
      val childAnnos = children(m.module).flatMap(c => inlines(m.targetParent.module(c.module)))
      if(m.circuit == m.module) { // never inline the main module
        childAnnos
      } else {
        InlineAnnotation(toName(m)) +: childAnnos
      }
    }
  }

  /** the InlineInstances pass uses Name instead of Target  */
  private def toName(m: ModuleTarget): ModuleName = ModuleName(m.module, CircuitName(m.circuit))
}
