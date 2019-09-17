package zhx.encoding

import cats.Monad
import cats.effect.Sync
import org.http4s.EntityDecoder.collectBinary
import org.http4s.headers.`Content-Type`
import org.http4s.{DecodeResult, DefaultCharset, Entity, EntityDecoder, EntityEncoder, Headers, InvalidMessageBodyFailure, MalformedMessageBodyFailure, MediaType}
import zhx.servers.Person
import zio.{IO, Task}

import scala.xml.{Node, NodeSeq, PrettyPrinter, XML}

object Encoders {

  trait XmlWriter[A] {
    def write(a: A): Node
  }

  trait XmlParser[A] {
    def parse(node: Node): Either[String, A]
  }

  implicit def xmlEntityEncoder[F[_] : Monad, X](implicit writer: XmlWriter[X]): EntityEncoder[F, X] =
    EntityEncoder
      .stringEncoder[F]
      .contramap { x: X =>
        val node = writer.write(x)
        val s = (new PrettyPrinter(80, 2)).format(node) // just because it's easier to debug
        s
      }
      .withContentType(`Content-Type`(MediaType.application.xml))

  implicit def xmlEntityDecoder[F[_] : Sync, X](implicit parser: XmlParser[X]): EntityDecoder[F, X] =
    EntityDecoder
      .decodeBy(MediaType.application.xml)(msg =>
        collectBinary(msg).flatMap { bs =>
          val asString = new String(bs.toArray, msg.charset.getOrElse(DefaultCharset).nioCharset)
          try {
            val asJValue = XML.loadString(asString)
            val xOrErr = parser.parse(asJValue)
            xOrErr match {
              case Right(x) => DecodeResult.success(x)
              case Left(msg) => DecodeResult.failure(InvalidMessageBodyFailure(msg))
            }
          } catch {
            case e: Exception => DecodeResult.failure(MalformedMessageBodyFailure("bad Xml", Some(e)))
          }
        }
      )

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
      Right(Person(name, age))
    } catch {
      case e : Exception => Left("parse error in xml")
    }

  }

  def parseIO[T](s: String)(implicit xmlParser: XmlParser[T]): Task[T] =
    IO.effect {
      val xml = XML.loadString(s)
      xmlParser.parse(xml) match {
        case Left(err) => throw new Exception(err)
        case Right(t) => t
      }
    }

}
