package zhx.servers

import org.http4s._
import zhx.encoding.Encoders._
import zio._
import zio.interop.catz._
import zio.test.{DefaultRunnableSpec, Predicate, assert, fail, suite, testM}

object TestHello3Service extends DefaultRunnableSpec(
  suite("routes suite")(
    testM("president returns donald") {
      (for{
        response <- Hello3Service.service.run(Request[Task](Method.GET, Uri.uri("/president")))
        body <- response.body.compile.toVector.map(x => x.map(_.toChar).mkString(""))
        parsed <- parseIO(body)
      }yield parsed)
        .fold(
          e => fail(Cause.fail(e)),
          s => assert(s, Predicate.equals(Person.donald)))
    },
    testM("joe is 76") {
      (for{
        rq <- Request[Task](Method.POST, Uri.uri("/ageOf"))
          .withBody(Person.joe)
        response <- Hello3Service.service.run(rq)
        body <- response.body.compile.toVector.map(x => x.map(_.toChar).mkString(""))
      }yield body)
        .fold(
          e => fail(Cause.fail(e)),
          s => assert(s, Predicate.equals("76")))
    }

  ))
