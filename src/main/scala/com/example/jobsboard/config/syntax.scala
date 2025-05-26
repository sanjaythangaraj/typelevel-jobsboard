package com.example.jobsboard.config

import cats.*
import cats.implicits.*
import pureconfig.error.ConfigReaderException
import pureconfig.{ConfigReader, ConfigSource}

import scala.reflect.ClassTag

object syntax {
  extension (source: ConfigSource)
    def loadF[F[_]: MonadThrow, A: ConfigReader](using tag: ClassTag[A]): F[A] =
      MonadThrow[F].pure(source.load[A]).flatMap {
        case Left(failures) => MonadThrow[F].raiseError[A](ConfigReaderException(failures))
        case Right(value)   => MonadThrow[F].pure(value)
      }
}
