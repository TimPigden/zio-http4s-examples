package tsp.stream.kafka

import zio.IO
import zio.test._
import zio.test.Assertion._
import zio._, zio.duration._
import zio.kafka.client._

object DemoTest extends DefaultRunnableSpec (

  suite("test emitting stream")(
    testM("random walk"){


      IO.succeed(assert(true)(equalTo(true)))
    }
   )
)
