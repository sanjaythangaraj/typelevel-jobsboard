package com.example.jobsboard.http.routes

import cats.effect.*
import cats.data.*
import cats.implicits.*
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.ci.CIStringSyntax

import scala.concurrent.duration.*
import com.example.jobsboard.fixtures.*
import com.example.jobsboard.core.*
import com.example.jobsboard.domain.security.*
import com.example.jobsboard.domain.auth.*
import com.example.jobsboard.domain.user.*

class AuthRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with SecuredRouteFixture {

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // prep
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
  val mockedAuth: Auth[IO] = new Auth[IO] {
    override def login(email: String, password: String): IO[Option[User]] =
      if (email == danielEmail && password == danielPassword)
        IO(Some(Daniel))
      else IO.pure(None)

    override def signUp(newUserInfo: NewUserInfo): IO[Option[User]] =
      if (newUserInfo.email == riccardoEmail)
        IO.pure(Some(Riccardo))
      else
        IO.pure(None)

    override def changePassword(
        email: String,
        newPasswordInfo: NewPasswordInfo
    ): IO[Either[String, Option[User]]] =
      if (email == danielEmail)
        if (newPasswordInfo.oldPassword == danielPassword)
          IO.pure(Right(Some(Daniel)))
        else IO.pure(Left("Invalid password"))
      else IO.pure(Right(None))

    override def delete(email: String): IO[Boolean] = IO.pure(true)
  }

  given logger: Logger[IO]       = Slf4jLogger.getLogger[IO]
  val authRoutes: HttpRoutes[IO] = AuthRoutes[IO](mockedAuth, mockedAuthenticator).routes

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // tests
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

  "AuthRoutes" - {
    "should return a 401 - unauthorized if login fails" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginInfo(danielEmail, "wrongPassword"))
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 200 - OK + a JWT if login is successful" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginInfo(danielEmail, danielPassword))
        )
      } yield {
        response.status shouldBe Status.Ok
        response.headers.get(ci"Authorization") shouldBe defined
      }
    }

    "should return a 400 Bad Request if the user to create already exists" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users")
            .withEntity(NewUserDaniel)
        )
      } yield {
        response.status shouldBe Status.BadRequest
      }
    }

    "should return a 201 Created if the user creation succeeds" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users")
            .withEntity(NewUserRiccardo)
        )
      } yield {
        response.status shouldBe Status.Created
      }
    }

    "should return a 200 - Ok if logging out with a valid JWT" in {
      for {
        jwtToken <- mockedAuthenticator.create(danielEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }

    "should return a 401 - Unauthorized if logging out without a JWT token" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 404 - NotFound for changing password when user doesn't exist" in {
      for {
        jwtToken <- mockedAuthenticator.create(riccardoEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withEntity(NewPasswordInfo(riccardoPassword, "newPassword"))
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.NotFound
      }
    }

    "should return a 403 - Forbidden for changing password with invalid old password" in {
      for {
        jwtToken <- mockedAuthenticator.create(danielEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withEntity(NewPasswordInfo("wrongPassword", "newPassword"))
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Forbidden
      }
    }

    "should return a 401 - Unauthorized for changing password wihtout JWT" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withEntity(NewPasswordInfo(danielPassword, "newPassword"))
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return 200 - Ok for changing password" in {
      for {
        jwtToken <- mockedAuthenticator.create(danielEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withEntity(NewPasswordInfo(danielPassword, "newPassword"))
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }

    "should return a 401 - unauthorized if a non-admin tries to delete a user" in {
      for {
        jwtToken <- mockedAuthenticator.create(riccardoEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/daniel@rockthejvm.com")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 200 - Ok if an admin tries to delete a user" in {
      for {
        jwtToken <- mockedAuthenticator.create(danielEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/daniel@rockthejvm.com")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }

  }
}
