// Copyright 2020 The Regents of the University of California
// released under BSD 3-Clause License
// author: Kevin Laeufer <laeufer@cs.berkeley.edu>
// based on code from dank-formal:
// Copyright 2020 Daniel Kasza
// released under Apache License 2.0

package kiwi.formal.transforms

import firrtl._
import firrtl.annotations._
import firrtl.ir._
import firrtl.options.Dependency
import firrtl.passes.ExpandWhens
import firrtl.transforms._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class CoverBranchesTransform extends Transform with DependencyAPIMigration {
    override def prerequisites = Seq(Dependency[InferResets])
    override def optionalPrerequisiteOf = Seq(Dependency(ExpandWhens), Dependency[PropagatePresetAnnotations])

    private def needsCover(s: Statement): Boolean = s match {
        case EmptyStmt | _: Verification | _: Print | _: Stop => false
        case Conditionally(_, _, a, b) => needsCover(a) || needsCover(b)
        case Block(stmts) => stmts.map(needsCover).reduce((a,b) => a || b)
        case _ => true
    }

    private case class BranchToCover(s: Statement, info: Info)

    private def onStatement(toCover: mutable.ListBuffer[BranchToCover], s: Statement): Unit = s match {
        case c: Conditionally => {
            if (needsCover(c.conseq)) {
                toCover.append(BranchToCover(c.conseq, c.info))
                c.conseq.foreachStmt(s => onStatement(toCover, s))
            }
            if (needsCover(c.alt)) {
                toCover.append(BranchToCover(c.alt, c.info))
                c.alt.foreachStmt(s => onStatement(toCover, s))
            }
        }
        case _ => s.foreachStmt(s => onStatement(toCover, s))
    }

    private val Bool = ir.UIntType(ir.IntWidth(1))
    private val False = ir.UIntLiteral(0, Bool.width)
    private val True = ir.UIntLiteral(1, Bool.width)
    private def generateResetDone(c: CircuitTarget, m: ir.Module, clock: ir.Reference): (ir.Reference, List[ir.Statement], Annotation) = {
        val namespace = Namespace(m)
        val reset = m.ports.find(_.name == "reset").getOrElse(throw new RuntimeException("Failed to find reset!"))

        // create a preset to initialize register with defined value
        val preset = ir.DefWire(ir.NoInfo, namespace.newName("preset"), ir.AsyncResetType)
        val presetInit = ir.Connect(ir.NoInfo, ir.Reference(preset), ir.DoPrim(PrimOps.AsAsyncReset, List(False), List(), Bool))
        val anno = PresetAnnotation(c.module(m.name).ref(preset.name))

        // create reset tracker
        val reg = ir.DefRegister(ir.NoInfo, namespace.newName("resetDone"), Bool, clock, ir.Reference(preset), False)
        // TODO: rethink logic
        val update = ir.Connect(ir.NoInfo, ir.Reference(reg), ir.Mux(ir.Reference(reset), True, ir.Reference(reg)))

        (ir.Reference(reg), List(preset, presetInit, reg, update), anno)
    }

    private def onModule(c: CircuitTarget, presetAnnos: ListBuffer[Annotation], m: ir.Module): ir.DefModule = {
        val clocks = m.ports.filter(_.tpe == ir.ClockType)
        assert(clocks.size == 1, "this transform only works for modules with a single clock domain")
        val clock = ir.Reference(clocks.head)

        val (enable, resetStmts, resetAnno) = generateResetDone(c, m, clock)
        presetAnnos.append(resetAnno)

        val toCover = new mutable.ListBuffer[BranchToCover]()
        m.foreachStmt(s => onStatement(toCover, s))

        def mapStatements(s: Statement): Statement = {
            toCover.find(_.s eq s) match {
                case Some(BranchToCover(_, info)) =>
                    val cover = Verification(Formal.Cover, info, clock, enable, True, StringLit(""))
                    Block(Seq(s.mapStmt(mapStatements), cover))
                case None =>
                    s.mapStmt(mapStatements)
            }
        }

        val body = ir.Block(resetStmts :+ mapStatements(m.body))
        m.copy(body=body)
    }

    override def execute(state: CircuitState): CircuitState = {
        // select which modules to add coverage to
        val coverAll = state.annotations.contains(CoverAllBranchesAnnotation)
        val coverAnno = state.annotations.collect { case CoverBranchesAnnotation(target)      => target.name }.toSet
        val notCover = state.annotations.collect { case DoNotCoverBranchesAnnotation(target) => target.name }.toSet
        val cover = if(coverAll) state.circuit.modules.map(_.name).toSet else coverAnno

        val c = CircuitTarget(state.circuit.main)
        val presetAnnos = mutable.ListBuffer[Annotation]()
        val circuit = state.circuit.mapModule({
            case m: ir.Module if (cover.contains(m.name) && (!notCover.contains(m.name))) => onModule(c, presetAnnos, m)
            case other => other
        })

        state.copy(circuit = circuit, annotations = state.annotations ++ presetAnnos)
    }
}

object CoverBranchesTransform

case object CoverAllBranchesAnnotation extends NoTargetAnnotation

case class CoverBranchesAnnotation(target: ModuleName) extends SingleTargetAnnotation[ModuleName] {
    def duplicate(n: ModuleName) = CoverBranchesAnnotation(n)
}

case class DoNotCoverBranchesAnnotation(target: ModuleName) extends SingleTargetAnnotation[ModuleName] {
    def duplicate(n: ModuleName) = DoNotCoverBranchesAnnotation(n)
}

