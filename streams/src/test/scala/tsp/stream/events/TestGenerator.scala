package tsp.stream.events

import tsp.stream.events.Events.{ChillEvent, GPS, LongLat}
import tsp.stream.events.Generators.SimpleEventState
import zio.test.Assertion._
import zio.test._
import zio._
import zio.stream.Sink
import zio.test.environment.TestEnvironment

object TestGenerator extends DefaultRunnableSpec (

  suite("test emititing stream")(
/*    testM("fold works") {
      val nItems = 200
      for {
        initialState <- Support.initialiseState
        folded <- Support.foldIt(initialState, nItems)
        _ = println(folded.map(_.toString).mkString("\n"))
      } yield {
        assert(folded.size, equalTo(nItems))
      }
      },*/
    testM("stream works"){
      for {
        initialState <- Support.initialiseState
        stream = ChillEventStream.generatedStream(initialState, Support.randomWalker).take(20)
        sink = Sink.collectAll[ChillEvent]
        runner <- stream.run(sink)
      } yield {
        assert(runner.size, equalTo(20))
      }
    }

    )

)

object Support {
  val randomWalker = Generators.centeringRandomWalkGenerator(1, -10, 0.1)
  case class Folder(s: SimpleEventState, events: Vector[ChillEvent])

  def foldIt(initialState: SimpleEventState,nItems: Int): ZIO[ZEnv, Nothing, Vector[ChillEvent]] =  {
    val seed: ZIO[ZEnv, Nothing, Folder] = IO.succeed(Folder(initialState, Vector.empty))
    val res = 0.until(nItems).foldLeft(seed) { (folderZ, _) =>
      for {
        current <- folderZ
        generated <- randomWalker.generate(current.s)
      } yield Folder(generated._2, current.events :+ generated._1)
    }
    res.map (_.events)
  }

  def initialiseState =
    Generators.now.map { nw =>
      SimpleEventState(ChillEvent("vehicle1", -18.0, GPS(nw, LongLat(0.0, 51.0), 90))) }

}
