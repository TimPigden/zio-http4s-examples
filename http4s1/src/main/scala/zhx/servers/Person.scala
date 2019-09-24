package zhx.servers

// Simple data type used for encoding and decoding
case class Person(name: String, age: Int)

object Person {
  val donald = Person("Donald Trump", 73)
  val joe = Person("Joe Biden", 76)
}