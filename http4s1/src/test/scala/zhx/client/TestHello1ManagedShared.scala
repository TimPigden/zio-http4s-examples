package zhx.client

import org.http4s._
import org.http4s.client.Client
import zio.test.Assertion.equalTo
import zio.test.{DefaultRunnableSpec, assertM, suite, testM}
import zio.{Task, ZIO}


object TestHello1ManagedShared extends DefaultRunnableSpec(

  suite("routes suite")(
    testM("test get") {
      for {
        client <- ZIO.environment[Client[Task]]
        req = Request[Task](Method.GET, uri"http://localhost:8080/")
        asserted <- assertM(client.status(req), equalTo(Status.Ok))
      } yield asserted
    }
  ).provideManagedShared(ClientTest.clientManaged)

)