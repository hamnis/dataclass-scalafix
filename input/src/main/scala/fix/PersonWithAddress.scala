/*
rule = GenerateDataClass
*/
package fix

import dataclass.{data, since}

@data class Address(street: String, city: String, zip: String)

@data class PersonWithAddress(name: String, age: Int, @since("1.0") address: Option[Address] = None)