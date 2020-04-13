package uzsttp.servers

import Processor._
import cats.data.NonEmptyList
import hrequest._
import uzhttp.Request.Method
import uzhttp.{HTTPError, Response}
import uzsttp.encoding.Encoders._
import zio.ZIO
import sttp.client._

object  XmlRoutes2 {

  val president = uriMethod(startsWith("president"), Method.GET).as {
    writeXmlBody(Person.donald)
  }

  val contender = uriMethod(endsWith("contender"), Method.GET).as {
    writeXmlBody(Person.joe)
  }

  val whatIsMyName = for {
    _ <- uriMethod(endsWith(NonEmptyList.of("whatIsMyName")), Method.POST)
    person <- parsedXmlBody[Person]
  } yield Response.plain(person.name)

  val routes = combineRoutes(president, contender, whatIsMyName)
}
