package tsp.stream.kafka

import zio.IO
import zio.test._
import zio.test.Assertion._
import zio._
import zio.duration._
import zio.kafka.client._
import zio.kafka.client.serde._
import zio.test.environment.Live
import DemoStuff._
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import zio.stream.{Sink, ZStream}

object DemoTest extends DefaultRunnableSpec (

  suite("test basic kafka stuff")(

    testM("test consume all on timer"){

      val subscription = Subscription.topics("test")
      for {
        consumed <- Consumer.make(consumerSettings).use { consumer =>
          consumer
            .subscribeAnd(subscription)
            .plainStream(Serde.string, Serde.string)
            .flattenChunks
            .timeout(3.seconds)
            .take(5)
            .map { r =>
              val key = r.record.key()
              val value = r.record.value()
              val offset = r.record.offset()
              (key, value, offset)
            }.runCollect
        }
      } yield assert(1, equalTo(20))

    },

/*
    testM("run producer"){
      val producer = Producer.make(producerSettings, Serde.string, Serde.string)

      for {
        mdTasks <- producer.use { p =>
         ZStream.fromIterable(10 until 20)
           .mapM { i =>
             p.produce(new ProducerRecord("test2", i.toString, (10 * i).toString))
           }.runCollect
        }
        mds <- ZIO.collectAll(mdTasks)
        _ <- Live.live(console.putStrLn(s"${mds.mkString("\n")}"))
      } yield {
        assert(true, equalTo(true))
      }
    }
    */

   )
)

object DemoStuff {
  val consumerSettings: ConsumerSettings =
    ConsumerSettings(
      bootstrapServers          = List("localhost:9092"),
      groupId                   = "group",
      clientId                  = "client",
      closeTimeout              = 30.seconds,
      extraDriverSettings       = Map(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "earliest"),
      pollInterval              = 250.millis,
      pollTimeout               = 50.millis,
      perPartitionChunkPrefetch = 2
    )

  val veryLongTime = Duration.fromNanos(Long.MaxValue)
  val producerSettings: ProducerSettings =
    ProducerSettings(bootstrapServers = List("localhost:9092"),
      closeTimeout                    = 30.seconds,
      extraDriverSettings = Map.empty
    )


}
