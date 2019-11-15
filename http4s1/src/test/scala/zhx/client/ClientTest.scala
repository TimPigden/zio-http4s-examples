package zhx.client

import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.Client
import zio.{Task, ZIO, ZManaged}
import zio.test.TestResult
import zio.interop.catz._
import zio.interop.catz

object ClientTest {

  def testClientM[R](fClient: Client[Task] => Task[TestResult])
  : Task[TestResult] =
    ZIO.runtime[Any].flatMap { implicit rts =>
      val exec = rts.Platform.executor.asEC
      BlazeClientBuilder[Task](exec).resource.use { client =>
        fClient(client)
      }
    }

  def clientManaged: ZManaged[Any, Throwable, Client[Task]] = {
    val zioManaged = ZIO.runtime[Any].map { rts =>
      val exec = rts.Platform.executor.asEC
      implicit def rr = rts
      catz.catsIOResourceSyntax(BlazeClientBuilder[Task](exec).resource).toManaged
    }
    // for our test we need a ZManaged, but right now we've got a ZIO of a ZManaged. To deal with
    // that we create a Managed of the ZIO and then flatten it
    val mgr = zioManaged.toManaged_ // toManaged_ provides an empty release of the rescoure
    mgr.flatten
  }

}
