package zhx.client

import org.http4s._
import org.http4s.client.Client
import zio.test.Assertion.equalTo
import zio.test._
import zio.{Task, ZIO}

object TestHello1ManagedShared extends DefaultRunnableSpec {

  override def spec = suite("routes suite")(
    testM("test get") {
      for {
        client <- hClient.client
        req = Request[Task](Method.GET, uri"http://localhost:8080/")
        asserted <- assertM(client.status(req))(equalTo(Status.Ok))
      } yield asserted
    }
  ).provideCustomLayerShared(ClientTest.clientLive).mapError(TestFailure.fail)

}