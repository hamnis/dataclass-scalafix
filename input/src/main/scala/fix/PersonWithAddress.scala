/*
rule = GenerateDataClass
*/
package fix

import dataclass.{data, since}

@data class Address(@since("1.0") street: String, @since("1.0") city: String = "", @since("1.1") zip: String = "")

@data class PersonWithAddress(name: String, age: Int, @since("1.0") address: Option[Address] = None)
