package dank.formal.sby

import chisel3._
import chisel3.stage._
import dank.formal._
import scala.reflect._
import firrtl.EmittedVerilogCircuitAnnotation
import firrtl.transforms.BlackBoxInlineAnno
import scala.collection.mutable.ArrayBuffer
import java.io._
import scala.sys.process._

class SbyRun[T<:FormalModule](gen: => T, mode: String, depth: Int = 0, base: String = "")(implicit c: ClassTag[T]) {
    // Generate job name.
    val jobname = base + classTag[T].toString + "_" + mode + (if (depth != 0) "_" + depth.toString else "")
    println(jobname)

    // Generate SystemVerilog for the module.
    val stage = new chisel3.stage.ChiselStage
    val annotations = stage.execute(Array("-X", "sverilog", "--target-dir", jobname), Seq(ChiselGeneratorAnnotation(() => gen)))
    val files = new ArrayBuffer[String]
    var module_name: String = null
    annotations.foreach({
        case a: EmittedVerilogCircuitAnnotation => {
            files.append(a.value.name + a.value.outputSuffix)
            module_name = a.value.name
        }
        case a: BlackBoxInlineAnno => files.append(a.name)
        case other => 
    })
    assert(module_name != null)

    // Generate configuration file.
    val writer = new PrintWriter(new File(jobname + ".sby"))
    writer.println("[options]")
    writer.println("mode " + mode)
    writer.println("depth " + depth.toString)
    writer.println()
    writer.println("[engines]")
    writer.println("smtbmc")
    writer.println()
    writer.println("[script]")
    files.foreach({ file => writer.println("read -formal " + file) })
    writer.println("prep -top " + module_name)
    writer.println()
    writer.println("[files]")
    files.foreach({ file => writer.println(jobname + "/" + file) })
    writer.close()

    val output = ("sby " + "-f -d " + jobname + "/sby " + jobname + ".sby").!!<

    println(output)
}