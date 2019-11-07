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
import net.manub.embeddedkafka.EmbeddedKafka
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import zio.stream.{Sink, ZStream}

object DemoTest extends DefaultRunnableSpec (

  suite("test basic kafka stuff")(

    testM("test consume all on timer"){

      def produceMany(t: String, kvs: List[(String, String)]): UIO[Unit] = ZIO.effectTotal {
        import net.manub.embeddedkafka.Codecs._
        EmbeddedKafka.publishToKafka(t, kvs)
      }

      val embeddedKafka   = EmbeddedKafka.start()
      val bootstrapServer = s"localhost:${embeddedKafka.config.kafkaPort}"
      val consumerSettings = ConsumerSettings(
        bootstrapServers = List(bootstrapServer),
        groupId = "group",
        clientId = "client",
        closeTimeout = 5.seconds,
        extraDriverSettings =       Map(
          ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "earliest",
          ConsumerConfig.METADATA_MAX_AGE_CONFIG  -> "100"
        ),
        pollInterval = 250.millis,
        pollTimeout = 50.millis,
        perPartitionChunkPrefetch = 2
      )
      for {
        kvs <- ZIO((1 to 5).toList.map(i => (s"key$i", s"msg$i")))
        _   <- produceMany("test", kvs)
        consumed <- Consumer.make(consumerSettings).use { consumer =>
          consumer
            .subscribeAnd(Subscription.topics("test"))
            .plainStream(Serde.string, Serde.string)
            .flattenChunks
            .take(5)
            .runCollect
        }
      } yield
//        assert(consumed.size, equalTo(5))
      assert(5, equalTo(5))

    },

/*

/*            .map { r =>

              val key = r.record.key()
              val value = r.record.value()
              val offset = r.record.offset()
              (key, value, offset)
            }.runCollect
*/
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
      bootstrapServers = List("localhost:9092"),
      groupId = "group",
      clientId = "client",
      closeTimeout = 30.seconds,
      extraDriverSettings = Map(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "earliest"),
      pollInterval = 250.millis,
      pollTimeout = 50.millis,
      perPartitionChunkPrefetch = 2
    )

  val veryLongTime = Duration.fromNanos(Long.MaxValue)
  val producerSettings: ProducerSettings =
    ProducerSettings(bootstrapServers = List("localhost:9092"),
      closeTimeout = 30.seconds,
      extraDriverSettings = Map.empty
    )

  def produceMany(t: String, kvs: List[(String, String)]): UIO[Unit] = ZIO.effectTotal {
    import net.manub.embeddedkafka.Codecs._
    EmbeddedKafka.publishToKafka(t, kvs)
  }
}
