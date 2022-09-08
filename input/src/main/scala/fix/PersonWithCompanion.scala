/*
rule = GenerateDataClass
*/
package fix

import dataclass.data

@data class PersonWithCompanion(name: String, age: Int)

object PersonWithCompanion {
def hello = s"Hello!"
}
