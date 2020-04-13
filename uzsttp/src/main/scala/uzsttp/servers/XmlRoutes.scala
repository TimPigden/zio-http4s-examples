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
    case req if (req.uri.getPath === "/president") && (req.method === Method.GET) =>
      IO.succeed(
        writeXmlBody(Person.donald)
      )
    case req if (req.uri.getPath === "/whatIsMyName") && (req.method === Method.POST) =>
      extractXmlBody[Person](req).map{ p =>
        Response.plain(p.name)
      }
    case req if (req.uri.getPath === "/contender") && (req.method === Method.GET) =>
      IO.succeed(
        writeXmlBody(Person.joe)
      )
  }
}
