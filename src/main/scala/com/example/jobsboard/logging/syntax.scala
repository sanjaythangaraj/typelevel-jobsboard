package com.example.jobsboard.logging

import cats.*
import cats.implicits.*
import org.typelevel.log4cats.Logger

object syntax {
  extension [F[_] : Logger, E, A](fa: F[A])(using MonadError[F, E]) {
    def log(success: A => String, error: E => String): F[A] = fa.attemptTap {
      case Left(e) => Logger[F].error(error(e))
      case Right(a) => Logger[F].info(success(a))
    }

    def logError(error: E => String): F[A] = fa.attemptTap {
      case Left(e) => Logger[F].error(error(e))
      case Right(_) => ().pure[F]
    }
  }
}
