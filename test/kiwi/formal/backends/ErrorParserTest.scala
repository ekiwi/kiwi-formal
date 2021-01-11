// Copyright 2020 The Regents of the University of California
// released under BSD 3-Clause License
// author: Kevin Laeufer <laeufer@cs.berkeley.edu>

package kiwi.formal.backends

import org.scalatest.flatspec.AnyFlatSpec
import firrtl.ir

import scala.util.matching.Regex

class ErrorParserTest extends AnyFlatSpec {
  behavior of "ErrorParser"

  it should "correctly parse smtbmc generated error locations" in {
    val loc = ErrorParser.parseSmtbmcFilename("SimpleCombinatorial.sv:15.20-16.22").get
    assert(loc.filename == "SimpleCombinatorial.sv")
    assert(loc.line == 15)
    assert(loc.col == 20)
    assert(loc.endLine == 16)
    assert(loc.endCol == 22)
  }

  it should "correctly parse chisel/firrtl generated locations (FileInfo)" in {
    val inp = ir.FileInfo("test.scala 11:15")
    val loc = ErrorParser.parseChiselFileInfo(inp.serialize)
    assert(loc == List(ErrorLoc("test.scala", 11, 15)))
  }

  it should "correctly parse chisel/firrtl generated locations (MultiInfo)" in {
    val inp = ir.MultiInfo(List(ir.FileInfo("test.scala 11:15"), ir.FileInfo("test.scala 12:0")))
    val loc = ErrorParser.parseChiselFileInfo(inp.serialize)
    assert(loc == List(ErrorLoc("test.scala", 11, 15), ErrorLoc("test.scala", 12, 0)))
  }
}
