/*
rule = GenerateDataClass
*/
package fix

import dataclass.data

@data class PersonWithOverride(name: String, age: Int, address: String) {
  override def toString = "Overridden"
}
