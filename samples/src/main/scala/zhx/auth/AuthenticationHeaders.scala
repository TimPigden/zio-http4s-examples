package zhx.auth

import org.http4s.{Header, Request}
import org.http4s.headers.Authorization
import zhx.auth.Authenticator.AuthToken
import zio.{IO, RIO, Task}

/**
 * The purpose of this is to extract authentication headers from the request
 * It can also be used as a blueprint for other stuff that needs extraction.
 * NB you might want better security than this :-)
 */
trait AuthenticationHeaders[R <: Authenticator] {
  type AuthHTask[T] = RIO[R, T]

  private def unauthenticated = IO.succeed(Left(new Exception("bad format authentication")))

  def getToken(req: Request[AuthHTask]) : AuthHTask[Either[Throwable, AuthToken]] = {
    val userNamePasswordOpt: Option[Array[String]] =
      for {
        auth <- req.headers.get(Authorization).map(_.value)
        asSplit = auth.split(" ")
        if asSplit.size == 2
      } yield asSplit
    val tok = userNamePasswordOpt.map { asSplit =>
      val res1 = for {
        authentic <- authenticator.authenticatorService
        tok  <- authentic.authenticate(asSplit(0), asSplit(1))
      } yield tok
      res1.either
    }
    tok.getOrElse(unauthenticated)
  }

}

object AuthenticationHeaders {
  def addAuthentication[Tsk[_]](request: Request[Tsk], username: String, password: String): Request[Tsk] =
    request.withHeaders(request.headers.put(Header("Authorization", s"$username $password")))

}
