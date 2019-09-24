package zhx.client

import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s._
import org.http4s.client.Client
import zio.{IO, Task, ZIO}
import zio.test._
import zio.test.{DefaultRunnableSpec, TestResult, ZSpec, assertM}
import zio.interop.catz._
import zio.IO._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.test.Assertion.equalTo

object ClientTest {

  def testClientM[R](fClient: Client[Task] => Task[TestResult])
  : Task[TestResult] =
    ZIO.runtime[Any].flatMap { implicit rts =>
      val exec = rts.Platform.executor.asEC
      BlazeClientBuilder[Task](exec).resource.use { client =>
        fClient(client)
      }
    }

  case class ServerEnv(clock: Clock, console: Console, blocking: Blocking, serverCreated: Unit)

}
