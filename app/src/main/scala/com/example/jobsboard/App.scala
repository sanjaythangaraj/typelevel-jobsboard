package com.example.jobsboard

import org.scalajs.dom.document

import scala.scalajs.js.annotation.*

@JSExportTopLevel("RockTheJvmApp")
class App {
  @JSExport
  def doSomething(containerId: String): Unit =
    document.getElementById(containerId).innerHTML = "Hello from ScalaJs"
}
