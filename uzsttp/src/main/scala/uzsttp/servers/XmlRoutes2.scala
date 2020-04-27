package uzsttp.servers

import EndPoint._
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

  // this version better if many routes - combineRoutes folds over the arguments
  val routes = combineRoutes(president, contender, whatIsMyName)

  // this version illustrates new orElseOption syntax nicely
  //val routes = president orElseOptional  contender orElseOptional whatIsMyName

}
