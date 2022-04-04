/*
rule = Data
*/
package fix

import data.data

@data class PersonWithOverride(name: String, age: Int, address: String) {
  override def toString = "Overridden"
}
