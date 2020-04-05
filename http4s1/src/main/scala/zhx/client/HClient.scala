package zhx.client
import org.http4s.client.Client
import zhx.client.HClient.HClient
import zio._
object HClient {

  type HClient = Has[Service]

  trait Service {
    def client: Client[Task]
  }

  case class SimpleClient(client: Client[Task]) extends Service
}

package object hClient {
  def client = ZIO.access[HClient](_.get.client)
}
