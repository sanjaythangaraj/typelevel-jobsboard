package com.example.jobsboard.fixtures

import cats.effect.IO
import com.example.jobsboard.core.*
import com.example.jobsboard.domain.user.*


/*
  rockthejvm
  $2a$10$wg0ZmK8p63u61MO18ZrGeerLwBSBYR2kLSzOM7yUjf/ROYzuuv43m
  
  riccardorulez
  $2a$10$fQkUtvu5nuOHbrkNltyDs.6TBXthWiRpssPu2bbmkWcFq5MgftZ5.

  simplepassword
  $2a$10$AEeCiF4V6XPdLGTJnT/65O2W1BmwGo.MytauN1PAEMI6pFYaFOf5O

  riccardorocks
  $2a$10$B4jw.6tGAalLgbjp/R.EOed9lKobwcM/3qS9t2EvdNjH28lv.iF6i
*/

trait UserFixture {

  val mockedUsers: Users[IO] = new Users[IO] {
    override def find(email: String): IO[Option[User]] =
      if (email == danielEmail) IO.pure(Some(Daniel))
      else IO.pure(None)

    override def create(user: User): IO[String] = IO.pure(user.email)

    override def update(user: User): IO[Option[User]] = IO.pure(Some(user))

    override def delete(email: String): IO[Boolean] = IO.pure(true)
  }
  
  val Daniel = User(
    "daniel@rockthejvm.com",
    "$2a$10$wg0ZmK8p63u61MO18ZrGeerLwBSBYR2kLSzOM7yUjf/ROYzuuv43m",
    Some("Daniel"),
    Some("CioCirlan"),
    Some("Rock the JVM"),
    Role.ADMIN
  )
  
  val danielEmail = Daniel.email
  val danielPassword = "rockthejvm"

  val Riccardo = User(
    "riccardo@rockthejvm.com",
    "$2a$10$fQkUtvu5nuOHbrkNltyDs.6TBXthWiRpssPu2bbmkWcFq5MgftZ5.",
    Some("Riccardo"),
    Some("Cardin"),
    Some("Rock the JVM"),
    Role.RECRUITER
  )
  
  val riccardoEmail = Riccardo.email
  val riccardoPassword = "riccardorulez"

  val NewUser = User(
    "newuser@gmail.com",
    "$2a$10$AEeCiF4V6XPdLGTJnT/65O2W1BmwGo.MytauN1PAEMI6pFYaFOf5O",
    Some("john"),
    Some("Doe"),
    Some("Some company"),
    Role.RECRUITER
  )

  val UpdatedRiccardo = User(
    "riccardo@rockthejvm.com",
    "$2a$10$B4jw.6tGAalLgbjp/R.EOed9lKobwcM/3qS9t2EvdNjH28lv.iF6i",
    Some("RICCARDO"),
    Some("CARDIN"),
    Some("Adobe"),
    Role.RECRUITER
  )
  
  val NewUserDaniel = NewUserInfo (
    danielEmail,
    danielPassword,
    Some("Daniel"),
    Some("CioCirlan"),
    Some("Rock the JVM")
  )

  val NewUserRiccardo = NewUserInfo(
    riccardoEmail,
    riccardoPassword,
    Some("Riccardo"),
    Some("Cardin"),
    Some("Rock the JVM")
  )
  
}
