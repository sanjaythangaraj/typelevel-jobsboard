package com.example.jobsboard.modules

import cats.effect.*
import com.example.jobsboard.config.PostgresConfig
import doobie.hikari.HikariTransactor
import doobie.util.*
import doobie.*

object Database {
  def apply[F[_]: Async](config: PostgresConfig): Resource[F, HikariTransactor[F]] = for {
    ec <- ExecutionContexts.fixedThreadPool(config.nThreads)
    xa <- HikariTransactor.newHikariTransactor[F](
      "org.postgresql.Driver",
      config.url,
      config.user,
      config.pass,
      ec
    )
  } yield xa
}
