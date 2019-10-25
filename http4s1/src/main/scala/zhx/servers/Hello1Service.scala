package zhx.servers

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import zio.Task
import zio.interop.catz._
import zio.interop.catz.implicits._

/**
 * simplest possible service with a single get as string
 */
object Hello1Service {

  private val dsl = Http4sDsl[Task]
  import dsl._

  val service = HttpRoutes.of[Task] {
    case GET -> Root => Ok("hello1!")
  }.orNotFound

}
