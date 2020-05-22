package uzsttp.encoding

import java.nio.charset.{Charset, StandardCharsets}

import uzsttp.servers.Person
import zio._

import scala.xml.{Node, NodeSeq, PrettyPrinter, XML}
import uzhttp.{HTTPError, Request, Response, Status}
import uzhttp.HTTPError.BadRequest
import zio.stream.{ZSink, ZTransducer}
import Response._
import sttp.client.HttpError
import uzsttp.servers.EndPoint._
import uzsttp.servers.hrequest

object Encoders {

  case class ParseError(msg: String) extends Throwable(msg)

  trait Encoder[Src, A] {
    def write(a: A): Src
  }

  trait Decoder[Src, A] {
    def parse(src: Src): Task[A]
  }

  type XmlWriter[A] = Encoder[Node, A]

  type XmlParser[A] = Decoder[Node, A]

  type StringWriter[A] = Encoder[String, A]

  type StringParser[A] = Decoder[String, A]

  implicit val personXmlWriter: XmlWriter[Person] = { p =>
    <Person>
      <name>{p.name}</name>
      <age>{p.age}</age>
    </Person>
  }

  implicit val personXmlParser: XmlParser[Person] = { node =>
    try {
      val name = (node \ "name").head.text
      val age = (node \ "age").head.text.toInt
      IO.succeed(Person(name, age))
    } catch {
      case e : Exception => IO.fail(ParseError(e.getMessage()))
    }
  }

  def parseXmlString[T](s: String)(implicit xmlParser: XmlParser[T]): IO[Throwable, T] =
    for {
      validXml <- Task(XML.loadString(s))
      parsed <- xmlParser.parse(validXml)
    } yield parsed

  def extractXmlBody[T](req: Request)(implicit xmlParser: XmlParser[T]): IO[HTTPError, T] =
    for {
      s <- requestStringBody(req)
      _ = println(s"extracted string body $s")
      t <- parseXmlString(s)(xmlParser).mapError(e => BadRequest(e.getMessage))
    } yield t

  def parsedXmlBody[T](implicit xmlParser: XmlParser[T]) =
    (for {
      s <- hrequest.stringBody
      _ = println(s"extracted string body $s")
      t <- parseXmlString(s)(xmlParser).mapError(e => BadRequest(e.getMessage))
    } yield t).mapError(e => Some(e)) // to make it play neatly with our Option[E] chain



  def writeXmlString[T](t: T)(implicit xmlWriter: XmlWriter[T]) = {
    // extravagently spaced pretty version for ease of debugging
    val pretty = new PrettyPrinter(80, 2)
    pretty.format(xmlWriter.write(t))
  }

  def xmlResponse(body: String, status: Status = Status.Ok, headers: List[(String, String)] = Nil, charset: Charset = StandardCharsets.UTF_8): Response =
    Response.const(body.getBytes(charset), status, contentType = s"application/xml; charset=${charset.name()}", headers = headers)

  def writeXmlBody[T](t: T)(implicit xmlWriter: XmlWriter[T]) = {
    xmlResponse(writeXmlString(t))
  }

  implicit val personParser: StringParser[Person] = { s => parseXmlString(s) }

  implicit val personWriter: StringWriter[Person] = { s => writeXmlString[Person](s)}

}
