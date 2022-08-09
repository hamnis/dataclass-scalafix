package fix

import dataclass.{data, since}

@data final class Address private (val street: String, val city: String, val zip: String) extends Product with Serializable {

override def equals(obj: Any): Boolean = obj match {
  case c: Address =>
    this.street == c.street && this.city == c.city && this.zip == c.zip
  case _ =>
    false
}
override lazy val hashCode: Int = {
  val state = Seq(street, city, zip)
  state.foldLeft(0)((a, b) => 31 * a.hashCode() + b.hashCode())
}
private def copy(street: String = this.street, city: String = this.city, zip: String = this.zip): Address = new Address(street, city, zip)
override def canEqual(obj: Any): Boolean = obj match {
  case c: Address => true
  case _ => false
}
override def productArity = 3
override def productElement(n: Int) = n match {
  case 0 =>
    street
  case 1 =>
    city
  case 2 =>
    zip
  case _ =>
    throw new IndexOutOfBoundsException()
}
override def productElementName(n: Int) = n match {
  case 0 =>
    "street"
  case 1 =>
    "city"
  case 2 =>
    "zip"
  case _ =>
    throw new IndexOutOfBoundsException()
}
override def productElementNames = {
  Iterator("street", "city", "zip")
}
override def productIterator = {
  Iterator(street, city, zip)
}
override def productPrefix = "Address"
def withStreet(street: String): Address = copy(street = street)
def withCity(city: String): Address = copy(city = city)
def withZip(zip: String): Address = copy(zip = zip)
override def toString = {
  val sb = new StringBuilder("Address")
  sb.append(productElementNames.zip(productIterator).map({
    case (name, value) =>
      s"$name=$value"
  }).mkString("(", ",", ")"))
  sb.toString
}


}
object Address {
def apply(street: String, city: String, zip: String): Address = new Address(street, city, zip)
}


@data final class PersonWithAddress private (val name: String, val age: Int, @since("1.0") val address: Option[Address]) extends Product with Serializable {

override def equals(obj: Any): Boolean = obj match {
  case c: PersonWithAddress =>
    this.name == c.name && this.age == c.age && this.address == c.address
  case _ =>
    false
}
override lazy val hashCode: Int = {
  val state = Seq(name, age, address)
  state.foldLeft(0)((a, b) => 31 * a.hashCode() + b.hashCode())
}
private def copy(name: String = this.name, age: Int = this.age, address: Option[Address] = this.address): PersonWithAddress = new PersonWithAddress(name, age, address)
override def canEqual(obj: Any): Boolean = obj match {
  case c: PersonWithAddress => true
  case _ => false
}
override def productArity = 3
override def productElement(n: Int) = n match {
  case 0 =>
    name
  case 1 =>
    age
  case 2 =>
    address
  case _ =>
    throw new IndexOutOfBoundsException()
}
override def productElementName(n: Int) = n match {
  case 0 =>
    "name"
  case 1 =>
    "age"
  case 2 =>
    "address"
  case _ =>
    throw new IndexOutOfBoundsException()
}
override def productElementNames = {
  Iterator("name", "age", "address")
}
override def productIterator = {
  Iterator(name, age, address)
}
override def productPrefix = "PersonWithAddress"
def withName(name: String): PersonWithAddress = copy(name = name)
def withAge(age: Int): PersonWithAddress = copy(age = age)
def withAddress(address: Option[Address]): PersonWithAddress = copy(address = address)
override def toString = {
  val sb = new StringBuilder("PersonWithAddress")
  sb.append(productElementNames.zip(productIterator).map({
    case (name, value) =>
      s"$name=$value"
  }).mkString("(", ",", ")"))
  sb.toString
}


}
object PersonWithAddress {
def apply(name: String, age: Int, @since("1.0") address: Option[Address]): PersonWithAddress = new PersonWithAddress(name, age, address)
def apply(name: String, age: Int): PersonWithAddress = new PersonWithAddress(name, age, None)
}
