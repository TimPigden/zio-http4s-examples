package uzsttp.auth

import zio._
import Authorizer._
import uzhttp._
import uzhttp.HTTPError.Unauthorized

object Authorizer {

  type Authorizer = Has[Service]

  type Auth = Has[AuthInfo]

  case class AuthInfo(status: String)

  object AuthInfo {
    val empty = AuthInfo("Dont care")
  }

  type AccessToken = String

  val Authorization = "Authorization"

  def getAuthorization(req: Request): RIO[Authorizer, AuthInfo] =
    req.headers.get(Authorization) match {
      case None => IO.fail(Unauthorized(""))
      case Some(s) => authorizer.authorize(s)
    }

  trait Service {
    def authorize(token: AccessToken): Task[AuthInfo]
  }

  val friendlyAuthorizer: Service = { token =>
    token match {
      case "friend" => IO.succeed(AuthInfo("Vetted"))
      case "acquaintance" => IO.succeed(AuthInfo("Dodgy"))
      case _ => IO.fail(Unauthorized("sorry, but no entry"))
    }
  }

  val friendlyAuthorizerLive = ZLayer.succeed(friendlyAuthorizer)

  def authorized(needsAuthority: PartialFunction[(Request, AuthInfo), IO[HTTPError, Response]]):
  ZIO[Authorizer, HTTPError, PartialFunction[Request, IO[HTTPError, Response]]] =
    ZIO.access[Authorizer](_.get).map { aut =>
      new PartialFunction[Request, IO[HTTPError, Response]] {
        override def isDefinedAt(x: Request): Boolean = needsAuthority.isDefinedAt((x, AuthInfo.empty))
        override def apply(x: Request): IO[HTTPError, Response] =
          (for {
            authInfo <- getAuthorization(x).provideLayer(ZLayer.succeed(aut))
            applied <- needsAuthority.apply((x, authInfo))
          } yield applied)
          .mapError { th =>
            th match {
              case herr: HTTPError => herr
              case th =>   Unauthorized(th.getMessage)
            }
          }
      }
    }
}

package object authorizer {
  def authorize(accessToken: AccessToken): RIO[Authorizer, AuthInfo]
  = ZIO.accessM[Authorizer](_.get.authorize(accessToken))
}

package object auth {
  def status = ZIO.access[Auth](_.get.status)
}