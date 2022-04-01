package fix


final class PersonWithOverride(val name: String, val age: Int, val address: String) extends Product with Serializable {
  override def equals(obj: Any): Boolean = obj match {
    case c: PersonWithOverride =>
      this.name == c.name && this.age == c.age && this.address == c.address
    case _ =>
      false
  }
  override lazy val hashCode: Int = {
    val state = Seq(name, age, address)
    state.foldLeft(0)((a, b) => 31 * a.hashCode() + b.hashCode())
  }
  private def copy(name: String = this.name, age: Int = this.age, address: String = this.address): PersonWithOverride = new PersonWithOverride(name, age, address)
  override def canEqual(obj: Any): Boolean = obj match {
    case c: PersonWithOverride => true
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
    Seq("name", "age", "address").iterator
  }
  override def productIterator = {
    Seq(name, age, address).iterator
  }
  override def productPrefix = "PersonWithOverride"
  def withName(name: String): PersonWithOverride = copy(name = name)
  def withAge(age: Int): PersonWithOverride = copy(age = age)
  def withAddress(address: String): PersonWithOverride = copy(address = address)
  override def toString = "Overridden"
}
