package tsp.avro

import org.json4s.JValue
import org.json4s.jackson.JsonMethods._
import org.apache.avro.Schema

object AvroCompiler {
  def compile(jv: JValue):Schema = {
    new Schema.Parser().parse(compact(jv))
  }
}
