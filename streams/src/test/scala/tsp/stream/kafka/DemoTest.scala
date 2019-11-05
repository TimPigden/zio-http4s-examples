package tsp.stream.kafka

import zio.IO
import zio.test._
import zio.test.Assertion._
import zio._, zio.duration._
import zio.kafka.client._
import zio.kafka.client.serde._

object DemoTest extends DefaultRunnableSpec (

  suite("test basic kafka stuff")(
    testM("test connection"){
      val settings: ConsumerSettings =
        ConsumerSettings(
          bootstrapServers          = List("localhost:9092"),
          groupId                   = "group",
          clientId                  = "client",
          closeTimeout              = 30.seconds,
          extraDriverSettings       = Map(),
          pollInterval              = 250.millis,
          pollTimeout               = 50.millis,
          perPartitionChunkPrefetch = 2
        )

      IO.succeed(assert(true, equalTo(true)))
    }
   )
)
