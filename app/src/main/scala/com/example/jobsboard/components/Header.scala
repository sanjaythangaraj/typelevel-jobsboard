package com.example.jobsboard.components

import tyrian.*
import tyrian.Html.*

import scala.scalajs.js
import scala.scalajs.js.annotation.*

import com.example.jobsboard.core.*

object Header {

  def view() =
    div(`class` := "header-container")(
      renderLogo(),
      div(`class` := "header-nav")(
        ul(`class` := "header-links")(
          renderNavLink("Jobs", "/jobs"),
          renderNavLink("Login", "/login"),
          renderNavLink("Sign Up", "/signup")
        )
      )
    )

  @js.native
  @JSImport("/public/static/img/logo.png", JSImport.Default)
  private val logoImage: String = js.native

  private def renderLogo() =
    a(
      href := "/",
      onEvent(
        "click",
        e => {
          e.preventDefault()
          Router.ChangeLocation("/")
        }
      )
    )(
      img(
        `class` := "home-logo",
        src     := logoImage,
        alt     := "Logo Image"
      )
    )

  private def renderNavLink(text: String, location: String) = {
    li(`class` := "nav-item")(
      a(
        href    := location,
        `class` := "nav-link",
        onEvent(
          "click",
          e => {
            e.preventDefault()
            Router.ChangeLocation(location)
          }
        )
      )(text)
    )
  }
}
