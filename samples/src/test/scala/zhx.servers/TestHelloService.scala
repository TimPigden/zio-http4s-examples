package zhx.servers

import org.http4s._
import zio._
import zio.interop.catz._
import zio.test.{DefaultRunnableSpec, Predicate, assert, fail, suite, testM}

object ExampleSpec extends DefaultRunnableSpec(
  suite("routes suite")(
    testM("root request returns Ok") {
      val io: Task[Response[Task]] = HelloService.service.run(Request[Task](Method.GET, Uri.uri("/")))
      io.fold(
        e => fail(Cause.fail(e)),
        s => assert(s.status, Predicate.equals(Status.Ok)))
    },
    testM("unmapped request returns not found") {
      val io: Task[Response[Task]] = HelloService.service.run(Request[Task](Method.GET, Uri.uri("/a")))
      io.fold(
        e => fail(Cause.fail(e)),
        s => assert(s.status, Predicate.equals(Status.NotFound))
      )
    },
    testM("root request body returns hello!") {
      (for{
        request <- HelloService.service.run(Request[Task](Method.GET, Uri.uri("/")))
        body <- request.body.compile.toVector.map(x => x.map(_.toChar).mkString(""))
      }yield body)
        .fold(
          e => fail(Cause.fail(e)),
          s => assert(s, Predicate.equals("hello!")))
    }
  ))
