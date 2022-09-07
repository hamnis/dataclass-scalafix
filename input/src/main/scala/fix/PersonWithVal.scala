/*
rule = GenerateDataClass
*/
package fix

import dataclass.data

@data class PersonWithVal(val name: String, val age: Int)
