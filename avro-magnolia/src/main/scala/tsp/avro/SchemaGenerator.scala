package tsp.avro

import magnolia._
import org.json4s.JsonAST._

import scala.language.experimental.macros

trait SchemaGenerator[A]  {
  def generate: JValue
}

object AvroSchemaDerivation {
  type Typeclass[T] = SchemaGenerator[T]

  def combine[T](ctx: CaseClass[SchemaGenerator, T]): SchemaGenerator[T] = new SchemaGenerator[T] {
    override def generate: JValue = {
      val fields = ctx.parameters.map { param =>
        val res = param.typeclass.generate
        JObject("name" -> JString(param.label),
            "type" -> res)
      }.toList
      JObject(
        "type" -> JString("record"),
        "name" -> JString(ctx.typeName.short),
        "namespace" -> JString(ctx.typeName.owner),
        "fields" -> JArray(fields)
      )
    }
  }

  def dispatch[T](ctx: SealedTrait[SchemaGenerator, T]): SchemaGenerator[T] = new SchemaGenerator[T] {
    override def generate: JValue = {
      val children = ctx.subtypes.map { st =>
        st.typeclass.generate
      }
      JArray(children.toList)
    }
  }

  implicit def avroSchema[T]: SchemaGenerator[T] = macro Magnolia.gen[T]

}
