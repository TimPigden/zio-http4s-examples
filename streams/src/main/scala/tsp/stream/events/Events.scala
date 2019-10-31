package tsp.stream.events

import java.time.Instant

object Events {

  type Temperature = Double

  trait Event {
    def at: Instant
  }

  case class SimpleEvent(at: Instant)

  case class ReceivedEvent[Evt](event: Evt, receivedAt: Instant)


  case class LongLat(longitude: Double, latitude: Double)
  case class GPS(at: Instant, longLat: LongLat, bearing: Double) extends Event
  case class ChillEvent(vehicleId: String, temperature: Temperature, gps: GPS)

}
