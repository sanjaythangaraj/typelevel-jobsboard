package com.example.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.HttpRoutes
import org.http4s.server.Router
import cats.effect.*
import cats.implicits.*
import org.typelevel.log4cats.Logger
import tsec.authentication.asAuthed

import com.example.jobsboard.core.*
import com.example.jobsboard.http.responses.*
import com.example.jobsboard.http.validation.syntax.*
import com.example.jobsboard.domain.job.*
import com.example.jobsboard.domain.pagination.*
import com.example.jobsboard.domain.security.*
import com.example.jobsboard.domain.user.*

import java.util.UUID

class JobRoutes[F[_]: Concurrent: Logger: SecuredHandler] private (jobs: Jobs[F])
    extends HttpValidationDsl[F] {

  object OffsetQueryParam extends OptionalQueryParamDecoderMatcher[Int]("offset")
  object LimitQueryParam  extends OptionalQueryParamDecoderMatcher[Int]("limit")

  // POST /jobs?limit=x&offset=y {filters} // TODO add query params and filters
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root :? LimitQueryParam(limit) +& OffsetQueryParam(offset) =>
      for {
        filter   <- req.as[JobFilter]
        jobsList <- jobs.all(filter, Pagination(limit, offset))
        res      <- Ok(jobsList)
      } yield res
  }

  // GET /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    jobs.find(id) flatMap {
      case Some(job) => Ok(job)
      case None      => NotFound(FailureResponse(s"Job $id  not found"))
    }
  }

  // POST /jobs/create {jobInfo}
  private val createJobRoute: AuthRoute[F] = { case req @ POST -> Root / "create" asAuthed _ =>
    req.request.validate[JobInfo] { jobInfo =>
      for {
        jobId <- jobs.create("TODO@rockthejvm.com", jobInfo)
        res   <- Created(jobId)
      } yield res
    }

  }

  // PUT /jobs/uuid {jobInfo}
  private val updateJobRoute: AuthRoute[F] = { case req @ PUT -> Root / UUIDVar(id) asAuthed user =>
    req.request.validate[JobInfo] { jobInfo =>
      jobs.find(id).flatMap {
        case None =>
          NotFound(FailureResponse(s"Cannot update job $id: not found"))
        case Some(job) if user.owns(job) || user.isAdmin =>
          jobs.update(id, jobInfo) *> Ok()
        case _ =>
          Forbidden(FailureResponse("You can only delete your own jobs"))
      }
    }
  }

  // DELETE /jobs/uuid
  private val deleteJobRoute: AuthRoute[F] = { case DELETE -> Root / UUIDVar(id) asAuthed user =>
    jobs.find(id) flatMap {
      case None => NotFound(FailureResponse(s"Cannot delete job $id: not found"))
      case Some(job) if user.owns(job) || user.isAdmin =>
        jobs.delete(id) *> Ok()
      case _ => Forbidden(FailureResponse("You can only delete your own jobs"))
    }
  }
  val unauthedRoutes = allJobsRoute <+> findJobRoute
  val authedRoutes = SecuredHandler[F].liftService(
    createJobRoute.restrictedTo(allRoles) |+|
      updateJobRoute.restrictedTo(allRoles) |+|
      deleteJobRoute.restrictedTo(allRoles)
  )
  val routes = Router(
    "/jobs" -> (unauthedRoutes <+> authedRoutes)
  )

}

object JobRoutes {
  def apply[F[_]: Concurrent: Logger: SecuredHandler](jobs: Jobs[F]) =
    new JobRoutes[F](jobs)
}
