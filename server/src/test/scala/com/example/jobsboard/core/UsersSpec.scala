package com.example.jobsboard.core

import cats.effect.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.Inside
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.example.jobsboard.fixtures.*
import com.example.jobsboard.domain.user.*
import org.postgresql.util.PSQLException

class UsersSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Inside
    with DoobieSpec
    with UserFixture {

  override val initScript: String = "sql/users.sql"

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "Users 'algebra'" - {
    "should retrieve a user by email" in {
      transactor.use { xa =>
        val program = for {
          users     <- LiveUsers[IO](xa)
          retrieved <- users.find("riccardo@rockthejvm.com")
        } yield retrieved

        program.asserting { _ shouldBe Some(Riccardo) }
      }
    }

    "should return None if the email doesn't exist" in {
      transactor.use { xa =>
        val program = for {
          users     <- LiveUsers[IO](xa)
          retrieved <- users.find("notfound@rockthejvm.co")
        } yield retrieved

        program.asserting { _ shouldBe None }
      }
    }

    "should create a new user" in {
      transactor.use { xa =>
        val program = for {
          users     <- LiveUsers[IO](xa)
          email     <- users.create(NewUser)
          maybeUser <- users.find(NewUser.email)
        } yield (email, maybeUser)

        program.asserting { case (email, maybeUser) =>
          email shouldBe NewUser.email
          maybeUser shouldBe Some(NewUser)
        }

      }
    }

    "should fail creating a new user if the email already exists" in {
      transactor.use { xa =>
        val program = for {
          users  <- LiveUsers[IO](xa)
          either <- users.create(Daniel).attempt
        } yield either

        program.asserting { outcome =>
          inside(outcome) {
            case Left(e) => e shouldBe a[PSQLException]
            case _       => fail()
          }
        }
      }
    }

    "should return None when updating a user that does not exist" in {
      transactor.use { xa =>
        val program = for {
          users     <- LiveUsers[IO](xa)
          maybeUser <- users.update(NewUser)
        } yield maybeUser

        program.asserting { _ shouldBe None }
      }
    }

    "should update an existing user" in {
      transactor.use { xa =>
        val program = for {
          users     <- LiveUsers[IO](xa)
          maybeUser <- users.update(UpdatedRiccardo)
        } yield maybeUser

        program.asserting(_ shouldBe Some(UpdatedRiccardo))
      }
    }

    "should delete a user" in {
      transactor.use { xa =>
        val program = for {
          users     <- LiveUsers[IO](xa)
          result    <- users.delete(Daniel.email)
          maybeUser <- users.find("daniel@rockthejvm.com")
        } yield (result, maybeUser)

        program.asserting { case (result, maybeUser) =>
          result shouldBe true
          maybeUser shouldBe None
        }

      }
    }

    "should NOT delete a user that does not exist" in {
      transactor.use { xa =>
        val program = for {
          users  <- LiveUsers[IO](xa)
          result <- users.delete("nobody@rockthejvm.com")
        } yield result

        program.asserting {
          _ shouldBe false
        }
      }
    }

  }
}
