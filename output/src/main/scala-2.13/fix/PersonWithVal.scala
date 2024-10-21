package fix

import dataclass.data

@data final class PersonWithVal private (val name: String, val age: Int) extends Product with Serializable {

override def equals(obj: Any): Boolean = obj match {
  case c: PersonWithVal =>
    this.name == c.name && this.age == c.age
  case _ =>
    false
}
override lazy val hashCode: Int = {
  val state = Seq(name, age)
  state.foldLeft(0)((a, b) => 31 * a.hashCode() + b.hashCode())
}
private def copy(name: String = this.name, age: Int = this.age): PersonWithVal = new PersonWithVal(name, age)
override def canEqual(obj: Any): Boolean = obj match {
  case c: PersonWithVal => true
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
override def productElementName(n: Int) = n match {
  case 0 =>
    "name"
  case 1 =>
    "age"
  case _ =>
    throw new IndexOutOfBoundsException()
}
override def productElementNames = {
  Iterator("name", "age")
}
override def productIterator = {
  Iterator(name, age)
}
override def productPrefix = "PersonWithVal"
def withName(name: String): PersonWithVal = copy(name = name)
def withAge(age: Int): PersonWithVal = copy(age = age)
override def toString = {
  val sb = new StringBuilder("PersonWithVal")
  sb.append(productElementNames.zip(productIterator).map {
    case (name, value) =>
      s"$name=$value"
  }.mkString("(", ",", ")"))
  sb.toString
}


}
object PersonWithVal {
def apply(name: String, age: Int): PersonWithVal = new PersonWithVal(name, age)
}
