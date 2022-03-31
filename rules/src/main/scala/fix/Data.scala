package fix

import scalafix.v1._
import scala.meta._

class Data extends SemanticRule("Data") {
  object AnnotationName {
    def unapply(cls: Defn.Class): Option[(Mod.Annot, Defn.Class)] = cls.mods.collectFirst{
      case m@Mod.Annot(init) => {
        init.tpe match {
          case Type.Name("Data") => Some(m -> cls)
          case _ => None
        }
      }
    }.flatten
  }

  def modify(mod: Mod.Annot, cls: Defn.Class) = {
    val fields = cls.ctor.paramss.head.map(p => Term.Name(p.name.value))
    val fieldsWithType = cls.ctor.paramss.head.flatMap(p => p.decltpe.map(Term.Name(p.name.value) -> _))
    val equal = fields.map(n => q"this.${n} == c.${n}").reduce((a, b) => q"$a && $b")
    val ctor = cls.ctor.copy(paramss = cls.ctor.paramss.map(paramList => paramList.map(param => param.copy(mods = Mod.ValParam() :: param.mods))))
    val equals = {
      q"""override def equals(obj: Any): Boolean = obj match {
            case c: ${cls.name} => ${equal}
            case _ => false
          }"""
    }
    val hashCode = {
      q"""override def hashCode(): Int = {
            val state = Seq(..$fields)
            state.foldLeft(0)((a, b) => 31 * a.hashCode() + b.hashCode())
          }"""
    }

    val canEqual = {
      q"""override def canEqual(obj: Any): Boolean = obj match {
            case c: ${cls.name} => true
            case _ => false
          }"""
    }

    val productArity = {
      q"""override def productArity = ${fields.size}"""
    }

    val productElement = {
      q"""override def productElement(n: Int) = {
            val state = Seq(..$fields)
            state(n)
       }"""
    }

    val productElementName = {
      q"""override def productElementName(n: Int) = {
            val state = Seq(..$fields)
            state(n)
       }"""
    }

    val productElementNames = {
      val terms = fields.map(n => Lit.String(n.value))

      q"""override def productElementNames = {
              Seq(..${terms}).iterator
       }"""
    }

    val productIterator = {
      q"""override def productIterator = {
              Seq(..$fields).iterator
       }"""
    }

    val productPrefix = {
      q"""override def productPrefix = ${cls.name.value}"""
    }

    val productDefs = canEqual :: productArity :: productElement :: productElementName :: productElementNames :: productIterator :: productPrefix :: Nil

    def toString = {
      val l = Lit.String(s"${cls.name.value}(")
      val sb = q"val sb = new StringBuilder($l)"
      val close = Lit.String(")")
      val sbFields = fields.map(f => q"sb.append($f)")
      val block = Term.Block(sb :: sbFields ::: List(q"sb.append($close)", q"sb.toString()"))
      q"override def toString = $block"
    }

    def copy = {
      val cbn = fieldsWithType.map{case (n, t) => Term.Param(Nil, n, Some(t), Some(q"this.$n"))}
      q"private def copy(..$cbn): ${cls.name} = new ${cls.name}(..$fields)"
    }

    val withFields = fieldsWithType.map{case (n, t) =>
      val withName = Term.Name("with" + n.value.head.toUpper + n.value.tail)
      q"def $withName($n: $t): ${cls.name} = copy($n = $n)"
    }

    val inits = List(Init(Type.Name("Product"), Name.Anonymous(), Nil), Init(Type.Name("Serializable"), Name.Anonymous(), Nil))
    val methods = equals :: hashCode :: copy :: productDefs ::: withFields ::: List(toString) ::: cls.templ.stats
    val template = cls.templ.copy(stats = methods, inits = inits)
    cls.copy(mods = Mod.Final() :: cls.mods.filterNot(_ == mod), ctor = ctor, templ = template)
  }

  override def fix(implicit doc: SemanticDocument): Patch = {
    val allAnnotationedClasses = doc.tree.collect{
      case AnnotationName((mod, c)) =>
        Patch.replaceTree(c, modify(mod, c).toString())
    }
    Patch.fromIterable(allAnnotationedClasses) + Patch.removeGlobalImport(Symbol("data/Data#"))
  }

}
