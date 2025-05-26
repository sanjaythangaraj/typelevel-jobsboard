package com.example.jobsboard.modules

import cats.effect.*
import cats.implicits.*
import doobie.Transactor
import org.typelevel.log4cats.Logger

import com.example.jobsboard.core.*

final class Core[F[_]] private (val jobs: Jobs[F], val users: Users[F], val auth: Auth[F])

object Core {

  def apply[F[_]: Sync: Logger](xa: Transactor[F]): Resource[F, Core[F]] = {
    val coreF: F[Core[F]] = for {
      jobs  <- LiveJobs[F](xa)
      users <- LiveUsers[F](xa)
      auth  <- LiveAuth[F](users)
    } yield new Core(jobs, users, auth)

    Resource.eval(coreF)
  }
}
