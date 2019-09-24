package zsx.events

import zio.IO
import zio.test._
import zio.test.Assertion._
import zsx.events.Events.{ChillEvent, GPS, LongLat}
import zsx.events.Generators.SimpleEventState

// WORK IN PROGRESS!
class TestChillStream extends DefaultRunnableSpec (

  suite("test emititing stream")(
    testM("root request returns Ok") {
        for {
          nw <- Generators.now
          initialState = SimpleEventState(ChillEvent("vehicle1", -18.0, GPS(nw, LongLat(0.0, 51.0), 90)))
          //response <- generateSteam

        } yield assert(true, equalTo(true))
      }
    )

)
