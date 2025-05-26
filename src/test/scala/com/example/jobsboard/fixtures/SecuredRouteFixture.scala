package com.example.jobsboard.fixtures

import cats.effect.*
import cats.data.OptionT
import tsec.authentication.{IdentityStore, JWTAuthenticator, SecuredRequestHandler}
import tsec.mac.jca.HMACSHA256

import scala.concurrent.duration.*
import com.example.jobsboard.domain.security.*
import com.example.jobsboard.domain.user.*
import org.http4s.{AuthScheme, Credentials, Request}
import org.http4s.headers.Authorization
import tsec.jws.mac.JWTMac

trait SecuredRouteFixture extends UserFixture {
  val mockedAuthenticator: Authenticator[IO] = {
    val key = HMACSHA256.unsafeGenerateKey
    val idStore: IdentityStore[IO, String, User] = (email: String) =>
      if (email == danielEmail) OptionT.pure(Daniel)
      else if (email == riccardoEmail) OptionT.pure(Riccardo)
      else OptionT.none[IO, User]

    JWTAuthenticator.unbacked.inBearerToken(
      expiryDuration = 1.day,
      maxIdle = None,
      identityStore = idStore,
      signingKey = key
    )
  }

  extension (r: Request[IO])
    def withBearerToken(a: JwtToken): Request[IO] =
      r.putHeaders {
        val jwtString = JWTMac.toEncodedString[IO, HMACSHA256](a.jwt)
        // Authorization: Bearer {jwt}
        Authorization(Credentials.Token(AuthScheme.Bearer, jwtString))
      }


  given securedHandler: SecuredHandler[IO] = SecuredRequestHandler(mockedAuthenticator)
}
