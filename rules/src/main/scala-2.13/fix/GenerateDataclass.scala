package fix

import scalafix.v1._
import scala.meta._

class GenerateDataClass extends SemanticRule("GenerateDataClass") {
  override def fix(implicit doc: SemanticDocument): Patch =
    GenerateDataClassImpl.fix(doc, scala212 = false)
}