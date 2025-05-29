package com.example.jobsboard.playground

import cats.effect.{IO, IOApp}

import java.util.Properties
import javax.mail.internet.MimeMessage
import javax.mail.{Authenticator, Message, PasswordAuthentication, Session, Transport}

import com.example.jobsboard.core.*
import com.example.jobsboard.config.*

object EmailsPlayground {
  def main(args: Array[String]): Unit = {
    // configs
    val host        = "smtp.ethereal.email"
    val port        = 587
    val user        = "king.senger@ethereal.email"
    val pass        = "96QQRbG1ukVG8BGfpR"
    val frontendURL = "https://jobsboard.example.com"
    val token       = "ABCD1234"

    // properties file
    val prop = new Properties
    prop.put("mail.smtp.auth", true)
    prop.put("mail.smtp.starttls.enable", true)
    prop.put("mail.smtp.host", host)
    prop.put("mail.smtp.port", port)
    prop.put("mail.smtp.ssl.trust", host)

    // authentication
    val auth = new Authenticator {
      override def getPasswordAuthentication: PasswordAuthentication =
        new PasswordAuthentication(user, pass)
    }

    // session
    val session = Session.getInstance(prop, auth)

    // email itself
    val subject = "Email from Rock the JVM"
    val content = s"""
     <div style="
        border: 1px solid black;
        padding: 20px;
        font-family: sans-serif;
        line-height: 2;
        font-size: 20px;
     ">
     <h1>Rock the JVM: Password Recovery</h1>
     <p>Your password recovery token is: $token</p>
     <p>
        Click <a href=$frontendURL/login">here</a> to get back to the application
     </p>
     <p>Hello from Rock the JVM</p>
     </div>
    """

    // message = MIME message
    val message = new MimeMessage(session)
    message.setFrom("daniel@rockthejvm.com")
    message.setRecipients(Message.RecipientType.TO, "the.user@gmail.com")
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=utf-8")

    // send
    Transport.send(message)
  }
}

object EmailsEffectPlayground extends IOApp.Simple {

  override def run: IO[Unit] = for {
    emails <- LiveEmails[IO](
      EmailServiceConfig(
        host = "smtp.ethereal.email",
        port = 587,
        user = "king.senger@ethereal.email",
        pass = "96QQRbG1ukVG8BGfpR",
        frontendUrl = "https://jobsboard.example.com"
      )
    )
    _ <- emails.sendPasswordRecoveryEmail("someone@rockthejvm.com", "ROCK78JVM")
  } yield ()
}
