package com.example.jobsboard.playground

import cats.effect.IO
import org.scalajs.dom.{console, document}
import tyrian.Html.*
import tyrian.cmds.Logger
import tyrian.{Cmd, Html, Sub, TyrianApp}

import scala.concurrent.duration.*
import scala.scalajs.js.annotation.*

object PlaygroundApp {
  sealed trait Msg
  case class Increment(amount: Int) extends Msg

  case class Model(count: Int)
}

// @JSExportTopLevel("RockTheJvmApp")
class PlaygroundApp extends TyrianApp[PlaygroundApp.Msg, PlaygroundApp.Model] {
  import PlaygroundApp.*

  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (Model(0), Cmd.None)

  override def subscriptions(model: Model): Sub[IO, Msg] =
//    Sub.None
    Sub.every[IO](1.second).map(_ => Increment(1))

  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    case Increment(amount) =>
      // console.log("changing count by " + amount)
      // (model.copy(count = model.count + amount), Logger.consoleLog[IO]("changing count by " + amount))
      (model.copy(count = model.count + amount), Cmd.None)

  override def view(model: Model): Html[Msg] =
    div(
      button(onClick(Increment(1)))("increase"),
      button(onClick(Increment(-1)))("decrease"),
      div(s"Tyrian running: ${model.count}")
    )
}
