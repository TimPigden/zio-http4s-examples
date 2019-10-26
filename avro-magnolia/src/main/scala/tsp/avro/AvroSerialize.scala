package tsp.avro

import java.io.File

import org.apache.avro.Schema
import org.apache.avro.file.{DataFileReader, DataFileWriter}
import org.apache.avro.generic.{GenericData, GenericDatumReader, GenericDatumWriter, GenericRecord}

import scala.collection.mutable.ListBuffer

object AvroSerialize {

  def writeBinary[T](ts: Iterable[T])
                    (dfCreate: (DataFileWriter[GenericRecord], Schema) => Unit)
                    (implicit schemaGenerator: SchemaGenerator[T], avroWriter: AvroWriter[T]): Unit = {
    val asJson = schemaGenerator.generate
    val schema = AvroCompiler.compile(asJson)

    val datumWriter = new GenericDatumWriter[GenericRecord](schema)
    val dataFileWriter = new DataFileWriter(datumWriter)
    dfCreate(dataFileWriter, schema)
    ts.foreach { t =>
      val gr = avroWriter.write(schema, t).asInstanceOf[GenericRecord]
      dataFileWriter.append(gr.asInstanceOf[GenericRecord])
    }
    dataFileWriter.flush()
    dataFileWriter.close()
  }

  def writeBinaryFile[T](ts: Iterable[T], file: File)
                        (implicit schemaGenerator: SchemaGenerator[T], avroWriter: AvroWriter[T]): Unit =
    writeBinary(ts){ (dfw, schema) => dfw.create(schema, file)}

  def readBinaryFile[T](file: File)(implicit schemaGenerator: SchemaGenerator[T], avroReader: AvroReader[T]): List[T] = {
    val schema = AvroCompiler.compile(schemaGenerator.generate)
    val datumReader = new GenericDatumReader[AnyRef](schema)
    val dataFileReader = new DataFileReader(file, datumReader)
    val record = new GenericData.Record(schema)
    val out = ListBuffer.empty[T]
    while (dataFileReader.hasNext) {
      dataFileReader.next(record)
      val t = avroReader.read(schema, record)
      out.append(t)
    }
    dataFileReader.close()
    out.toList
  }

}
