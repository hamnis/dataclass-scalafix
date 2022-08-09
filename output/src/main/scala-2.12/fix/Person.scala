package fix

import dataclass.data

@data final class Person private (val name: String, val age: Int) extends Product with Serializable {

override def equals(obj: Any): Boolean = obj match {
  case c: Person =>
    this.name == c.name && this.age == c.age
  case _ =>
    false
}
override lazy val hashCode: Int = {
  val state = Seq(name, age)
  state.foldLeft(0)((a, b) => 31 * a.hashCode() + b.hashCode())
}
private def copy(name: String = this.name, age: Int = this.age): Person = new Person(name, age)
override def canEqual(obj: Any): Boolean = obj match {
  case c: Person => true
  case _ => false
}
override def productArity = 2
override def productElement(n: Int) = n match {
  case 0 =>
    name
  case 1 =>
    age
  case _ =>
    throw new IndexOutOfBoundsException()
}
def productElementName(n: Int) = n match {
  case 0 =>
    "name"
  case 1 =>
    "age"
  case _ =>
    throw new IndexOutOfBoundsException()
}
def productElementNames = {
  Iterator("name", "age")
}
override def productIterator = {
  Iterator(name, age)
}
override def productPrefix = "Person"
def withName(name: String): Person = copy(name = name)
def withAge(age: Int): Person = copy(age = age)
override def toString = {
  val sb = new StringBuilder("Person")
  sb.append(productElementNames.zip(productIterator).map({
    case (name, value) =>
      s"$name=$value"
  }).mkString("(", ",", ")"))
  sb.toString
}


}
object Person {
def apply(name: String, age: Int): Person = new Person(name, age)
}
