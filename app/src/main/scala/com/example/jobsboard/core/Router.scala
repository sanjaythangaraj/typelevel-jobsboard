package com.example.jobsboard.core

import tyrian.*
import cats.effect.*
import fs2.dom.History

case class Router private (location: String, history: History[IO, String]) {
  import Router.*

  def update(msg: Msg): (Router, Cmd[IO, Msg]) = msg match {
    case ChangeLocation(newLocation, browserTriggered) =>
      if (location == newLocation) (this, Cmd.None)
      else {
        val cmd =
          if (browserTriggered) Cmd.None
          else goto(newLocation)
        (this.copy(location = newLocation), cmd)
      }
    case _ => (this, Cmd.None)
  }

  def goto[M](location: String): Cmd[IO, M] =
    Cmd.SideEffect[IO] {
      history.pushState(location, location)
    }
}

object Router {
  trait Msg
  case class ChangeLocation(location: String, browserTriggered: Boolean = false)   extends Msg
  case class ExternalRedirect(location: String) extends Msg

  def startAt[M](initialLocation: String): (Router, Cmd[IO, M]) = {
    val router = Router(initialLocation, History[IO, String])
    (router, router.goto(initialLocation))
  }
}
