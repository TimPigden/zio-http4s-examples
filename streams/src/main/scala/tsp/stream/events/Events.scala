package tsp.stream.events

import java.time.Instant

object Events {

  type Temperature = Double

  case class LongLat(longitude: Double, latitude: Double)
  case class GPS(time: Instant, longLat: LongLat, bearing: Double)
  case class ChillEvent(vehicleId: String, temperature: Temperature, gps: GPS)

  case class ReceivedEvent(chillEvent: ChillEvent, receivedAt: Instant)
}
