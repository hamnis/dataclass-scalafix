package fix


final class Person(val name: String, val age: Int) extends Product with Serializable {
  override def equals(obj: Any): Boolean = obj match {
    case c: Person =>
      this.name == c.name && this.age == c.age
    case _ =>
      false
  }
  override def hashCode(): Int = {
    val state = Seq(name, age)
    state.foldLeft(0)((a, b) => 31 * a.hashCode() + b.hashCode())
  }
  private def copy(name: String = this.name, age: Int = this.age): Person = new Person(name, age)
  override def canEqual(obj: Any): Boolean = obj match {
    case c: Person => true
    case _ => false
  }
  override def productArity = 2
  override def productElement(n: Int) = {
    val state = Seq(name, age)
    state(n)
  }
  override def productElementName(n: Int) = {
    val state = Seq(name, age)
    state(n)
  }
  override def productElementNames = {
    Seq("name", "age").iterator
  }
  override def productIterator = {
    Seq(name, age).iterator
  }
  override def productPrefix = "Person"
  def withName(name: String): Person = copy(name = name)
  def withAge(age: Int): Person = copy(age = age)
  override def toString = {
    val sb = new StringBuilder("Person(")
    sb.append(name)
    sb.append(age)
    sb.append(")")
    sb.toString()
  }
}
