// Copyright 2020 The Regents of the University of California
// released under BSD 3-Clause License
// author: Kevin Laeufer <laeufer@cs.berkeley.edu>
package kiwi.formal.backends

import scala.collection.mutable
import scala.io.{BufferedSource, Source}
import scala.util.matching.Regex

case class ErrorLoc(filename: String, line: Int, col: Int = -1, endLine: Int = -1, endCol: Int = -1)
case class Error(msg: String, locs: List[ErrorLoc], vcd: Option[String] = None, step: Int = -1)

trait ErrorFormatter {
  def format(e: Error): String
}

object DefaultErrorFormatter extends ErrorFormatter {
  override def format(e: Error): String = {
    val scalaLoc = e.locs.find(_.filename.endsWith(".scala")).map(format).map("(" + _ + ") ")
    val svLoc = e.locs.find(_.filename.endsWith(".sv")).map(format).map("(" + _ + ")")
    val loc = if(scalaLoc.isEmpty && svLoc.isEmpty) "" else (scalaLoc ++ svLoc).mkString("") + ": "
    val vcd = e.vcd.map("\n" + _).getOrElse("")
    loc + e.msg + vcd
  }
  private def format(l: ErrorLoc): String = s"${l.filename}:${l.line}"
}

private class ErrorParser(directory: String) {
  import ErrorParser._

  private val sources = mutable.HashMap[String, IndexedSeq[String]]()
  protected def getSourceLines(filename: String): IndexedSeq[String] = {
    sources.getOrElseUpdate(filename, {
      // TODO: search for file
      val src = Source.fromFile(filename)
      val lines = src.getLines().toVector
      src.close()
      lines
    })
  }
  protected def getLine(filename: String, line: Int): String = {
    require(line > 0, "lines are one based")
    getSourceLines(filename)(line - 1)
  }
}

private object ErrorParser {
  // e.g.: SimpleCombinatorial.sv:15.20-16.22
  private val smtbmc: Regex = raw"([^\.]+\.\w+):(\d+)\.(\d+)\-(\d+)\.(\d+)".r
  def parseSmtbmcFilename(line: String): Option[ErrorLoc] = {
    line match {
      case smtbmc(filename, startLine, startCol, endLine, endCol) =>
        Some(ErrorLoc(filename, startLine.toInt, startCol.toInt, endLine.toInt, endCol.toInt))
      case _ => None
    }
  }

  // e.g.: @[test.scala 11:15] (FileInfo)
  private val firrtlInfo: Regex = raw";?\s*@\[([^\]]+)\]".r
  private val chiselFileInfo: Regex = raw"\s*([^\.]+\.\w+) (\d+):(\d+)".r
  def parseChiselFileInfo(line: String): List[ErrorLoc] = {
    line match {
      case firrtlInfo(comment) =>
        chiselFileInfo.findAllMatchIn(comment).map { m =>
          ErrorLoc(m.group(1), m.group(2).toInt, m.group(3).toInt)
        }.toList
      case _ => List()
    }
  }
}