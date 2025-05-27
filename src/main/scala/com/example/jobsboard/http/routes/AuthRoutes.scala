package com.example.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.*
import org.http4s.server.Router
import cats.effect.*
import cats.implicits.*
import org.typelevel.log4cats.Logger
import tsec.authentication.{SecuredRequestHandler, TSecAuthService, asAuthed}
import com.example.jobsboard.http.validation.syntax.*
import com.example.jobsboard.http.responses.*
import com.example.jobsboard.core.*
import com.example.jobsboard.domain.auth.*
import com.example.jobsboard.domain.user.*
import com.example.jobsboard.domain.security.*

class AuthRoutes[F[_]: Concurrent: Logger: SecuredHandler] private (
    auth: Auth[F],
    authenticator: Authenticator[F]
) extends HttpValidationDsl[F] {

  // POST /auth/login { loginInfo } => 200 OK with Authorization: Bearer {jwt}
  private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
    req.validate[LoginInfo] { loginInfo =>
      val maybeJwtToken = for {
        maybeUser <- auth.login(loginInfo.email, loginInfo.password)
        _         <- Logger[F].info(s"User logging in: ${loginInfo.email}")
        // create token - return JwtToken
        maybeToken <- maybeUser.traverse { user =>
          authenticator.create(user.email) // F[JwtToken]
        }                                  // F[Option[JwtToken]]
      } yield maybeToken

      maybeJwtToken.map {
        case Some(token) => authenticator.embed(Response(Status.Ok), token)
        case None        => Response(Status.Unauthorized)
      }
    }
  }

  // POST /auth/users { NewUserInfo } => 201 Created
  private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "users" =>
      req.validate[NewUserInfo] { newUserInfo =>
        for {
          maybeNewUser <- auth.signUp(newUserInfo)
          res <- maybeNewUser match {
            case Some(user) => Created(user.email)
            case None       => BadRequest(s"User with email ${newUserInfo.email} already exists")
          }
        } yield res
      }
  }

  // PUT /auth/users/password { NewPasswordInfo } {Authorization: Bearer {jwt} } => 200 OK
  private val changePasswordRoute: AuthRoute[F] = {
    case req @ PUT -> Root / "users" / "password" asAuthed user =>
      req.request.validate[NewPasswordInfo] { newPasswordInfo =>
        for {
          maybeUserOrError <- auth.changePassword(user.email, newPasswordInfo)
          resp <- maybeUserOrError match {
            case Right(Some(_)) => Ok()
            case Right(None)    => NotFound(FailureResponse(s"User ${user.email} not found"))
            case Left(_)        => Forbidden()
          }
        } yield resp
      }
  }

  // POST /auth/reset { ForgotPasswordInfo }
  private val forgotPasswordRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "reset" =>
      for {
        forgotPasswordInfo <- req.as[ForgotPasswordInfo]
        _                  <- auth.sendPasswordRecoveryToken(forgotPasswordInfo.email)
        resp               <- Ok()
      } yield resp
  }

  // POST /auth/recover {RecoverPasswordInfo }
  private val recoverPasswordRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "recover" =>
      for {
        recoverPasswordInfo <- req.as[RecoverPasswordInfo]
        recoverySuccessful <- auth.recoverPasswordFromToken(
          recoverPasswordInfo.email,
          recoverPasswordInfo.token,
          recoverPasswordInfo.newPassword
        )
        resp <-
          if (recoverySuccessful) Ok()
          else Forbidden(FailureResponse("Email/token combination is incorrect"))
      } yield resp
  }

  // POST /auth/logout { Authorization: Bearer {jwt} } => 200 OK
  private val logoutRoute: AuthRoute[F] = { case req @ POST -> Root / "logout" asAuthed _ =>
    val token = req.authenticator
    for {
      _   <- authenticator.discard(token)
      res <- Ok()
    } yield res

  }

  // DELETE /auth/users/daniel@rockthejvm.com
  private val deleteUserRoute: AuthRoute[F] = {
    case req @ DELETE -> Root / "users" / email asAuthed user =>
      auth.delete(email).flatMap {
        case true  => Ok()
        case false => NotFound()
      }
  }

  val unauthedRoutes = loginRoute <+> createUserRoute <+> forgotPasswordRoute <+> recoverPasswordRoute
  val authedRoutes = SecuredHandler[F].liftService(
    changePasswordRoute.restrictedTo(allRoles) |+|
      logoutRoute.restrictedTo(allRoles) |+|
      deleteUserRoute.restrictedTo(adminOnly)
  )

  val routes = Router(
    "/auth" -> (unauthedRoutes <+> authedRoutes)
  )
}

object AuthRoutes {
  def apply[F[_]: Concurrent: Logger: SecuredHandler](
      auth: Auth[F],
      authenticator: Authenticator[F]
  ) =
    new AuthRoutes[F](auth, authenticator)
}
