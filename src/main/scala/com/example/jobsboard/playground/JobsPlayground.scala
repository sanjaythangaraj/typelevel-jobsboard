package com.example.jobsboard.playground

import cats.effect.*
import doobie.*
import doobie.implicits.*
import doobie.util.*
import doobie.hikari.HikariTransactor
import com.example.jobsboard.domain.job.*
import com.example.jobsboard.core.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.io.StdIn

object JobsPlayground extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val postgresResource: Resource[IO, HikariTransactor[IO]] = for {
    ec <- ExecutionContexts.fixedThreadPool(32)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql:board",
      "docker",
      "docker",
      ec
    )
  } yield xa

  val jobInfo = JobInfo.minimal(
    company = "Rock the JVM",
    title = "Software Engineer",
    description = "Best job ever",
    externalUrl = "rockthejvm.com",
    remote = true,
    location = "Anywhere"
  )

  val jobInfo2 = JobInfo(
    company = "The M Word",
    title = "Cats Developer",
    description = "To tame them cats",
    externalUrl = "themword.com",
    remote = false,
    location = "NYC",
    salaryLo = Some(1000),
    salaryHi = Some(2000),
    currency = Some("USD"),
    country = Some("USA"),
    tags = Some(List("cats", "cats effect")),
    image = None,
    seniority = Some("junior developer"),
    other = None
  )

  override def run: IO[Unit] = postgresResource.use { xa =>
    for {
      jobs <- LiveJobs[IO](xa)
      _    <- IO(println("Ready. Next...")) *> IO(StdIn.readLine)
      _   <- jobs.create("daniel@rockthejvm.com", jobInfo)
      _ <- IO.println("Next...") *> IO.readLine
      id <- jobs.create("admin@themword.com", jobInfo2)
      _ <- IO.println("Next...") *> IO.readLine
      list <- jobs.all()
      _ <- IO.println(s"All jobs: $list. Next...") *> IO(StdIn.readLine)
      _ <- jobs.update(id, jobInfo2.copy(remote = true, title= "Typelevel Developer", other = Some("tea > coffee")))
      newJob <- jobs.find(id)
      _    <- IO(println(s"New job: $newJob. Next...")) *> IO(StdIn.readLine)
      _ <- jobs.delete(id)
      listAfter <- jobs.all()
      _ <- IO.println(s"Deleted job. List now: $listAfter")
    } yield ()
  }
}
