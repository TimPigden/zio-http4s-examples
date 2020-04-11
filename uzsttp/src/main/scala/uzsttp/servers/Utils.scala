package uzsttp.servers
import cats.Eq
import uzhttp.Request.Method

object Utils {

  implicit val methodEq: Eq[Method] = { (a, b) => a == b }

}
