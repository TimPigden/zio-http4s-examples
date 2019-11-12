package tsp.misc.testing

import tsp.misc.testing.SequentialTestHelper._
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration._
import zio.test._
import zio.test.TestAspect._
import zio.test.Assertion._
import zio.test.environment.{Live, TestEnvironment}

object SequentialTest extends DefaultRunnableSpec (
  suite("sequential test")(
    List(tst(1), tst(2), tst(3)) :_*
  ).provideManagedShared(extraEnvironment) @@ sequential
)

object SequentialTestHelper {
  def tst(i: Int) = {
    val label = s"test$i"
    testM(label)(
      for {
        _ <- Live.live(console.putStrLn(s"start$i"))
        nameRes <- withAdminClient { s =>
          s.doSomething
        }
        _ <- Live.live(console.putStrLn(s"end$i"))
      } yield assert(nameRes, equalTo("done something"))
    )
  }

  case class AdminClient(private val semaphore: Semaphore) {
    def doSomething: RIO[Clock with Blocking, String] =
      semaphore.withPermit(
        ZIO.sleep(1.seconds).map { _ => "done something"}
      )
  }

  def adminClient =
    Managed.fromEffect {
      Semaphore.make(1L).map{ sem => AdminClient(sem)}
    }

  type ExtraClockBlocking = ExtraStuff with Clock with Blocking

  def liveClockBlocking: ZIO[ExtraEnvironment, Nothing, ExtraClockBlocking] =
    for {
      clck    <- Live.live(ZIO.environment[Clock])
      blcking <- ZIO.environment[Blocking]
      extr    <- ZIO.environment[ExtraStuff]
    } yield new ExtraStuff with Clock with Blocking {
      override val extraStuff: ExtraStuff.Service = extr.extraStuff

      override val clock: Clock.Service[Any]       = clck.clock
      override val blocking: Blocking.Service[Any] = blcking.blocking
    }
  def withAdminClient[T](f: AdminClient => RIO[Any with Clock with ExtraStuff with Blocking, T]) =
    for {
      lcb <- liveClockBlocking
      fRes <- adminClient.use { ac =>
        f(ac)
      }.provide(lcb)
    } yield fRes

  trait ExtraStuff {
    def extraStuff: ExtraStuff.Service
  }

  val simpleService: ExtraStuff.Service = new ExtraStuff.Service {
    override def name: String = "hi"
  }

  object ExtraStuff {
    trait Service {
      def name: String
    }

    val make: Managed[Nothing, ExtraStuff] = Managed.fromEffect(IO.succeed{
      new ExtraStuff {
        override val extraStuff: Service = simpleService
      }
    })
  }

  type ExtraEnvironment = TestEnvironment with ExtraStuff

  val extraEnvironment: Managed[Nothing, ExtraEnvironment] =
    for {
      testEnvironment <- TestEnvironment.Value
      extra <- ExtraStuff.make
    } yield new TestEnvironment(
      testEnvironment.blocking,
      testEnvironment.clock,
      testEnvironment.console,
      testEnvironment.live,
      testEnvironment.random,
      testEnvironment.sized,
      testEnvironment.system
    ) with ExtraStuff {
      val extraStuff = extra.extraStuff
    }

}
