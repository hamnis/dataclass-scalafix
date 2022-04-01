/*
rule = Data
*/
package fix

import data.Data

@Data class PersonWithOverride(name: String, age: Int, address: String) {
  override def toString = "Overridden"
}
