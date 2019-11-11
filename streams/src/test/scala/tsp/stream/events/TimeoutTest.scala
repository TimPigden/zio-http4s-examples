package tsp.stream.events

import zio.stream.ZStream
import zio.test._
import zio.duration._
import TimeoutTestSupport._
import zio.ZIO
import zio.test.environment.Live

object TimeoutTest extends DefaultRunnableSpec(
  suite("timings")(
    testM("straight with timeout") {
      for {
        ts <- Live.live(timeoutStream)
      } yield assertCompletes
    },

  )
)

object TimeoutTestSupport {
  def timeoutStream =
    ZStream.fromEffect(ZIO.never)
    .take(1)
    .timeout(2.seconds)
    .runCollect
}

