package uzsttp.servers

import uzhttp.Request.Method
import uzhttp.{HTTPError, Request, Response}
import zio.IO
import cats.implicits._
import Utils._
import uzsttp.encoding.Encoders
import Encoders._

object XmlRoutes {
  val routes: PartialFunction[Request, IO[HTTPError, Response]] = {
    case req if (req.uri.getPath.startsWith("/president")) && (req.method === Method.GET) =>
      IO.succeed(
        writeXmlBody(Person.donald)
      )
    case req if (req.uri.getPath.startsWith("/whatIsMyName")) && (req.method === Method.POST) =>
      extractXmlBody[Person](req).map{ p =>
        Response.plain(p.name)
      }
    case req if (req.uri.getPath.startsWith("/contender")) && (req.method === Method.GET) =>
      IO.succeed(
        writeXmlBody(Person.joe)
      )

  }
}
