package zhx.servers

import org.http4s._
import zhx.encoding.Encoders._
import zio._
import zio.interop.catz._
import zio.test._
import zio.test.Assertion._

object TestHello3Service extends DefaultRunnableSpec(
  suite("routes suite")(
    testM("president returns donald") {
      for{
        response <- Hello3Service.service.run(Request[Task](Method.GET, uri"/president"))
        body <- response.body.compile.toVector.map(x => x.map(_.toChar).mkString(""))
        parsed <- parseIO(body)
      }yield assert(parsed, equalTo(Person.donald))
    },
    testM("joe is 76") {
      val rq = Request[Task](Method.POST, uri"/ageOf")
        .withEntity(Person.joe)
      for {
        response <- Hello3Service.service.run(rq)
        body <- response.body.compile.toVector.map(x => x.map(_.toChar).mkString(""))
      } yield assert(body, equalTo("76"))
    }

  ))
