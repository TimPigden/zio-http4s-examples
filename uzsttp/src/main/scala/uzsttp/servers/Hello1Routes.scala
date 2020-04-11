package uzsttp.servers
import uzhttp._
import zio._
import Request._
import HTTPError._
import cats.implicits._
import Utils._
object Hello1Routes {
  val routes: PartialFunction[Request, IO[HTTPError, Response]] = {
    case req if (req.uri.getPath === "/") && (req.method === Method.GET) =>
      IO.succeed(Response.plain("OK"))
  }

}
