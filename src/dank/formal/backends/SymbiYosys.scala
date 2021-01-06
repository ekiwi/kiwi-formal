package dank.formal.backends

import chisel3._
import firrtl.AnnotationSeq
import java.io._
import scala.io.Source

/** SymbiYosys operation. */
class SymbiYosys(val engines: Seq[String] = List("smtbmc")) extends Backend {
    override def name = "SymbiYosys"
    override def prefix = "sby"

    override def run(directory: String, op: VerificationOp, opts: VerificationOptions, gen: () => RawModule, annos: AnnotationSeq): VerificationResult = {
        val elaborated = Elaboration.elaborate(gen, annos)


        val jobfile = directory + File.separator + elaborated.name + ".sby"
        val options = op match {
            case BoundedCheck(depth) => List("mode bmc", s"depth $depth")
            case BoundedCover(depth) =>List("mode cover", s"depth $depth")
            case Prove() => List("mode prove")
        }

        // Generate configuration file.
        val writer = new PrintWriter(new File(jobfile))
        writer.println("[options]")
        options.foreach(writer.println)
        writer.println()
        writer.println("[engines]")
        engines.foreach(writer.println)
        writer.println()
        writer.println("[script]")
        elaborated.files.foreach({ file => writer.println("read -formal " + file) })
        writer.println("prep -top " + elaborated.name)
        writer.println()
        writer.println("[files]")
        elaborated.files.foreach({ file => writer.println(directory + File.separator + file) })
        writer.close()

        // Run SymbiYosys and grab outputs
        val process = new ProcessBuilder("sby", "-f", "-d", directory + File.separator + "sby", jobfile).start()
        val rc = process.waitFor()
        val stdout = Source.fromInputStream(process.getInputStream)



        // Find all errors.
        val errors = stdout.getLines().flatMap { line =>
            if (line.contains("Unreached cover statement at")) {
                Some(formatError(directory, "unreached cover statement", line.split(" ").last))
            } else if (line.contains("Assert failed in")) {
                if (line.contains(" (step ")) {
                    val location = line.split(" ").dropRight(2).last
                    val step = line.split(" ").last.dropRight(1).toInt
                    Some(formatError(directory,"assert failed", location, step))
                } else {
                    Some(formatError(directory,"assert failed", line.split(" ").last))
                }
            } else if (line.toLowerCase.contains("error")) {
                Some(line)
            } else {
                None
            }
        }.toList
        stdout.close()

        if (rc != 0) {
            VerificationFail(List("Sby failed, return code: " + rc.toString) ++ errors)
        } else {
            VerificationSuccess
        }
    }

    /** Helper function for recording errors that occurred on a specific source line. */
    private def formatError(directory: String, error: String, location: String, step: Int = -1): String = {
        val sv_file_name = location.split(":")(0)
        val sv_file_path = directory + File.separator + sv_file_name
        val sv_line_num = location.split("-").last.split("\\.")(0).toInt
        val source = Source.fromFile(sv_file_path)
        val sv_line = source.getLines().toList(sv_line_num - 1)
        source.close()
        val scala_location = if (sv_line.contains("// @[")) {
            "@" + sv_line.split("// @").last
        } else {
            "@[unknown]"
        }

        scala_location +
          "(" + sv_file_name + ":" + sv_line_num.toString + ") " +
          error +
          (if (step == -1) {
              ""
          } else {
              " (step " + step.toString + ")"
          })
    }
}