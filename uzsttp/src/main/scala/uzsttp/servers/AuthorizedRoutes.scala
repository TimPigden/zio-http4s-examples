package uzsttp.servers
import uzhttp._
import uzhttp._
import zio._
import Request._
import uzsttp.auth.Authorizer.AuthInfo
import cats.implicits._
import Utils._
import uzhttp.HTTPError.{Forbidden, Unauthorized}

object AuthorizedRoutes {
  val routes: PartialFunction[(Request, AuthInfo), IO[HTTPError, Response]] = {
    case (req, auth) if (req.uri.getPath === "/") && (req.method === Method.GET) =>
      if (auth.status === "Vetted") IO.succeed(Response.plain("OK"))
      else IO.fail(Forbidden("go get permission"))
  }
}
