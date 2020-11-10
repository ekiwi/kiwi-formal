package dank.formal.sby

import chisel3._
import chisel3.stage._
import dank.formal._
import scala.reflect._
import firrtl.EmittedVerilogCircuitAnnotation
import firrtl.transforms.BlackBoxInlineAnno
import scala.collection.mutable.ArrayBuffer
import java.io._
import scala.io.Source

class SbyException(val message: String) extends Exception(message)

/** SymbiYosys operation. */
class SbyRun[T<:FormalModule](gen: => T, mode: String, depth: Int = 20, base: String = "")(implicit c: ClassTag[T]) {
    /* Generate job name. */
    val jobname = base + classTag[T].toString + "_" + mode + depth.toString

    /* Generate SystemVerilog for the module. */
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

    /* Generate configuration file. */
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

    /* Run SymbiYosys and grab outputs. */
    val process = new ProcessBuilder("sby", "-f", "-d", jobname+"/sby", jobname+".sby").start()
    val rc = process.waitFor()
    val stdout = Source.fromInputStream(process.getInputStream())
    
    /* Start gathering errors by checking the return code. */
    val errors = new ArrayBuffer[String]
    if (rc != 0) {
        errors.append("Sby failed, return code: " + rc.toString)
    }

    /** Helper function for recording errors that occurred on a specific source line. */
    private def record_error(error: String, location: String, step: Int = -1) {
        val sv_file_name = location.split(":")(0)
        val sv_file_path = jobname + "/" + sv_file_name
        val sv_line_num = location.split("-").last.split("\\.")(0).toInt
        val source = Source.fromFile(sv_file_path)
        val sv_line = source.getLines().toList(sv_line_num-1)
        source.close()
        val scala_location = if (sv_line.contains("// @[")) {
            "@" + sv_line.split("// @").last
        } else {
            "@[unknown]"
        }

        val error_string =
            scala_location +
            "(" + sv_file_name + ":" + sv_line_num.toString + ") " +
            error +
            (if (step == -1) { "" } else {" (step " + step.toString + ")"})

        errors.append(error_string)
    }

    /* Find all errors. */
    for (line <- stdout.getLines()) {
        if (line.contains("Unreached cover statement at")) {
            record_error("unreached cover statement", line.split(" ").last)
        } else if (line.contains("Assert failed in")) {
            if (line.contains(" (step ")) {
                val location = line.split(" ").dropRight(2).last
                val step = line.split(" ").last.dropRight(1).toInt
                record_error("assert failed", location, step)
            } else {
                record_error("assert failed", line.split(" ").last)
            }
        } else if (line.toLowerCase.contains("error")) {
            errors.append(line)
        }
    }
    
    /** Throws an exception iff there were errors in the run. */
    def throwErrors() {
        if (errors.length != 0) {
            throw new SbyException(errors.mkString("\n"))
        }
    }
}