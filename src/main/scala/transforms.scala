package dank.formal.transforms

import scala.collection.mutable.ArrayBuffer
import firrtl._
import firrtl.transforms._
import firrtl.ir._
import firrtl.PrimOps._
import firrtl.annotations._
import firrtl.options.Dependency
import firrtl.stage.TransformManager
import firrtl.passes.ExpandWhens

class CoverBranchesTransform extends Transform with DependencyAPIMigration {
    override def prerequisites = Seq(Dependency[InferResets])
    override def optionalPrerequisiteOf = Seq(Dependency(ExpandWhens))

    def needsCover(s: Statement): Boolean = {
        var r = false
        s match {
            case EmptyStmt =>
            case _: Verification =>
            case _: Print =>
            case _: Stop =>
            case _: Block => s.foreachStmt(r |= needsCover(_))
            case _ => r = true
        }
        return r
    }

    case class BranchToCover(predicate: Expression, info: Info)

    def onStatement(toCover: ArrayBuffer[BranchToCover], pred: Expression, s: Statement) {
        s match {
            case c: Conditionally => {
                val conseqPred = DoPrim(And, Seq(pred, c.pred), Nil, pred.tpe)
                val altPred = DoPrim(And, Seq(pred, DoPrim(Not, Seq(c.pred), Nil, pred.tpe)), Nil, pred.tpe)
                
                if (needsCover(c.conseq)) {
                    toCover.append(BranchToCover(conseqPred, c.info))
                    c.conseq.foreachStmt(s => onStatement(toCover, conseqPred, s))
                }

                if (needsCover(c.alt)) {
                    toCover.append(BranchToCover(altPred, c.info))
                    c.alt.foreachStmt(s => onStatement(toCover, altPred, s))
                }
            }
            case _ => s.foreachStmt(s => onStatement(toCover, pred, s))
        }
    }

    def onModule(m: firrtl.ir.Module) = {
        var clk: Port = null
        m.foreachPort({ port=>
            if (port.name.matches("clock")) {
                clk = port; 
            }
        })
        assert(clk != null)

        var reset_done: DefWire = null
        def retrieveResetDone(s: Statement) {
            s match {
                case w: DefWire => if (w.name.equals("_reset_done")) {
                    assert(reset_done == null)
                    reset_done = w
                }
                case b: Block => b.foreachStmt(retrieveResetDone)
                case _ =>
            }
        }
        retrieveResetDone(m.body)
        assert(reset_done != null)

        val toCover = new scala.collection.mutable.ArrayBuffer[BranchToCover]()

        m.foreachStmt(s => onStatement(toCover, UIntLiteral(1), s))
        
        val enable = Reference(reset_done)

        val nb = new ArrayBuffer[Statement]()
        nb.append(m.body)
        toCover.foreach({ c =>
            val cover = Verification(Formal.Cover, c.info, Reference(clk), c.predicate, enable, StringLit(""))
            nb.append(cover)
        })
        
        m.copy(body = Block(nb.toSeq))
    }

    protected def execute(state: firrtl.CircuitState) = {
        val cover    = state.annotations.collect { case CoverBranchesAnnotation(target)      => target.name }.toSet
        val notCover = state.annotations.collect { case DoNotCoverBranchesAnnotation(target) => target.name }.toSet
        state.copy(circuit = state.circuit.mapModule({
            case m: firrtl.ir.Module if (cover.contains(m.name) && (!notCover.contains(m.name))) => onModule(m)
            case other => other
        }))
    }
}

object CoverBranchesTransform

case class CoverBranchesAnnotation(target: ModuleName) extends SingleTargetAnnotation[ModuleName] {
    def duplicate(n: ModuleName) = CoverBranchesAnnotation(n)
}

case class DoNotCoverBranchesAnnotation(target: ModuleName) extends SingleTargetAnnotation[ModuleName] {
    def duplicate(n: ModuleName) = DoNotCoverBranchesAnnotation(n)
}

