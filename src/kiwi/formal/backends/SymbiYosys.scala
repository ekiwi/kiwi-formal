// Copyright 2020 The Regents of the University of California
// released under BSD 3-Clause License
// author: Kevin Laeufer <laeufer@cs.berkeley.edu>
// based on code from dank-formal:
// Copyright 2020 Daniel Kasza
// released under Apache License 2.0


package kiwi.formal.backends

import chisel3._
import firrtl.AnnotationSeq

import java.io._
import scala.collection.mutable
import scala.io.{BufferedSource, Source}

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

        // parse errors from output
        val stdout = Source.fromInputStream(process.getInputStream)
        val errorParser = new SymbiYosysErrorParser(directory)
        val errors = errorParser.parseLines(stdout.getLines())
        stdout.close()

        if (rc != 0) {
            VerificationFail("Sby failed, return code: " + rc.toString, errors)
        } else {
            VerificationSuccess
        }
    }
}


private class SymbiYosysErrorParser(directory: String) extends ErrorParser(directory) {
    def parseLines(lines: Iterator[String]): List[Error] = {
        val vcd: Option[String] = None
        lines.flatMap { line =>
            if (line.contains("Unreached cover statement at")) {
                Some(Error("unreached cover statement", getLoc(line.split(" ").last), vcd))
            } else if (line.contains("Assert failed in")) {
                if (line.contains(" (step ")) {
                    val location = line.split(" ").dropRight(2).last
                    val step = line.split(" ").last.dropRight(1).toInt
                    Some(Error("assert failed", getLoc(location), vcd, step))
                } else {
                    Some(Error("assert failed", getLoc(line.split(" ").last), vcd))
                }
            } else if (line.toLowerCase.contains("error")) {
                Some(Error(line, List(), None))
            } else {
                None
            }
        }.toList
    }

    private def getLoc(loc: String): List[ErrorLoc] = {
        val svLoc = ErrorParser.parseSmtbmcFilename(loc).getOrElse(throw new RuntimeException(s"Failed to parse location: `$loc`"))
        val svLine = getLine(directory + File.separator + svLoc.filename, svLoc.endLine)
        val scalaLocs = svComment(svLine).map(ErrorParser.parseChiselFileInfo).getOrElse(List())
        List(svLoc) ++ scalaLocs
    }

    private def svComment(line: String): Option[String] = if(line.contains("//")) {
        Some(line.split("//").tail.mkString(""))
    } else { None }
}

