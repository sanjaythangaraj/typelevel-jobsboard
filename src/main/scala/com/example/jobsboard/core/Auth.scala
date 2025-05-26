package com.example.jobsboard.core

import cats.*
import cats.data.OptionT
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import org.typelevel.log4cats.Logger
import com.example.jobsboard.domain.auth.*
import com.example.jobsboard.domain.security.*
import com.example.jobsboard.config.*
import com.example.jobsboard.domain.user.*
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

import scala.concurrent.duration.*

trait Auth[F[_]] {
  def login(email: String, password: String): F[Option[User]]
  def signUp(newUserInfo: NewUserInfo): F[Option[User]]
  def changePassword(
      email: String,
      newPasswordInfo: NewPasswordInfo
  ): F[Either[String, Option[User]]]
  def delete(email: String): F[Boolean]
}

class LiveAuth[F[_]: Sync: Logger] private (
    users: Users[F]
) extends Auth[F] {

  override def login(email: String, password: String): F[Option[User]] = for {
    // find user in DB -> return None if no user
    maybeUser <- users.find(email) // F[Option[User]]

    // check password - return None if password doesn't match
    maybeValidatedUser <- maybeUser.filterA { user =>
      BCrypt
        .checkpwBool[F](
          password,
          PasswordHash[BCrypt](user.hashedPassword)
        )
    } // F[Option[User]]


  } yield maybeValidatedUser

  override def signUp(newUserInfo: NewUserInfo): F[Option[User]] =
    // find user in DB, return None if users exists
    users.find(newUserInfo.email).flatMap {
      case Some(_) => None.pure[F]
      case None =>
        for {
          // hash the new password
          hashedPassword <- BCrypt.hashpw[F](newUserInfo.password)
          user <- User(
            email = newUserInfo.email,
            hashedPassword = hashedPassword,
            firstName = newUserInfo.firstName,
            lastName = newUserInfo.lastName,
            company = newUserInfo.company,
            role = Role.RECRUITER
          ).pure[F]
          // create a new user in the db
          _ <- users.create(user)
        } yield Some(user)
    }

  override def changePassword(
      email: String,
      newPasswordInfo: NewPasswordInfo
  ): F[Either[String, Option[User]]] = {

    def updateUser(user: User, newPassword: String): F[Option[User]] = for {
      hashedPassword <- BCrypt.hashpw[F](newPassword)
      updatedUser    <- users.update(user.copy(hashedPassword = hashedPassword))
    } yield updatedUser

    def checkAndUpdate(
        user: User,
        oldPassword: String,
        newPassword: String
    ): F[Either[String, Option[User]]] = for {
      // check old password
      passCheck <- BCrypt
        .checkpwBool[F](
          oldPassword,
          PasswordHash[BCrypt](user.hashedPassword)
        )
      // if password ok, hash new password and update user in DB
      updateResult <-
        if (passCheck) {
          updateUser(user, newPassword).map(Right(_))
        } else Left("Invalid password").pure[F]
    } yield updateResult

    // find user in DB, return Right(None) if user doesn't exist
    users.find(email).flatMap {
      case None => Right(None).pure[F]
      case Some(user) =>
        checkAndUpdate(user, newPasswordInfo.oldPassword, newPasswordInfo.newPassword)
    }
  }

  override def delete(email: String): F[Boolean] =
    users.delete(email)
}

object LiveAuth {
  def apply[F[_]: Sync: Logger](
      users: Users[F]
  ): F[LiveAuth[F]] = {
    new LiveAuth[F](users).pure[F]
  }
}
