package uzsttp.servers

// Simple data type used for encoding and decoding
case class Person(name: String, age: Int)

object Person {
  val donald = Person("Donald Trump", 73)
  val joe = Person("Joe Biden", 76)
  val DEATH = Person("DEATH", Int.MaxValue)

  val crowd = List("Matthew", "Mark", "John", "Simon", "Andrew").zipWithIndex.map { tup =>
    Person(tup._1, tup._2 + 30)
  }

  def older(person: Person) = person.copy(age = person.age + 1)
}