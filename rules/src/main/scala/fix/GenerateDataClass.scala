package fix

import scalafix.v1._
import scala.meta._

class GenerateDataClass extends SemanticRule("GenerateDataClass") {
  object DataAnnotation {
    def unapply(cls: Defn.Class): Option[(Defn.Class)] = cls.mods.collectFirst {
      case m@Mod.Annot(init) =>
        init.tpe match {
          case Type.Name("data") => Some(cls)
          case _ => None
        }
    }.flatten
  }

  def generateClass(cls: Defn.Class) = {
    val fields = cls.ctor.paramss.head.map(p => Term.Name(p.name.value))
    val fieldsWithType = cls.ctor.paramss.head.flatMap(p => p.decltpe.map(Term.Name(p.name.value) -> _))
    val equal = fields.map(n => q"this.${n} == c.${n}").reduce((a, b) => q"$a && $b")
    val ctor = cls.ctor.copy(paramss = cls.ctor.paramss.map(paramList => paramList.map(param => param.copy(mods = List(Mod.ValParam()), default = None))), mods = List(Mod.Private(Name.Anonymous())))
    val equals = {
      q"""override def equals(obj: Any): Boolean = obj match {
            case c: ${cls.name} => ${equal}
            case _ => false
          }"""
    }
    val hashCode = {
      q"""override lazy val hashCode: Int = {
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
      val terms = fields.zipWithIndex.map { case (term, idx) => Case.apply(p"$idx", None, term) }
      q"""override def productElement(n: Int) = n match {
            ..case $terms
            case _ => throw new IndexOutOfBoundsException()
       }"""
    }

    val productElementName = {
      val terms = fields.zipWithIndex.map { case (term, idx) => Case.apply(p"$idx", None, Lit.String(term.value)) }
      q"""override def productElementName(n: Int) = n match {
           ..case $terms
           case _ => throw new IndexOutOfBoundsException()
       }"""
    }

    val productElementNames = {
      val terms = fields.map(n => Lit.String(n.value))

      q"""override def productElementNames = {
              Iterator(..${terms})
       }"""
    }

    val productIterator = {
      q"""override def productIterator = {
              Iterator(..$fields)
       }"""
    }

    val productPrefix = {
      q"""override def productPrefix = ${cls.name.value}"""
    }

    val productDefs = canEqual :: productArity :: productElement :: productElementName :: productElementNames :: productIterator :: productPrefix :: Nil

    def toString = {
      val className = Lit.String(s"${cls.name.value}")
      q"""override def toString = {
         val sb = new StringBuilder($className)
            sb.append(productElementNames.zip(productIterator).map{
              case (name, value) => s"$$name=$$value"
            }.mkString("(", ",", ")"))
            sb.toString
         }"""
    }

    def copy = {
      val cbn = fieldsWithType.map { case (n, t) => Term.Param(Nil, n, Some(t), Some(q"this.$n")) }
      q"private def copy(..$cbn): ${cls.name} = new ${cls.name}(..$fields)"
    }

    val withFields = fieldsWithType.map { case (n, t) =>
      val withName = Term.Name("with" + n.value.head.toUpper + n.value.tail)
      q"def $withName($n: $t): ${cls.name} = copy($n = $n)"
    }

    def matchesSignature(def1: Defn.Def, def2: Defn.Def) = {
      def1.mods.map(_.toString()).toSet == def2.mods.map(_.toString()).toSet &&
        def1.tparams.size == def2.tparams.size &&
        def1.paramss.flatMap(_.map(_.toString())) == def2.paramss.flatMap(_.map(_.toString()))
    }

    val inits = List(Init(Type.Name("Product"), Name.Anonymous(), Nil), Init(Type.Name("Serializable"), Name.Anonymous(), Nil))
    val methods = {
      val existingDefs = cls.templ.stats.collect {
        case d: Defn.Def => d.name.value -> d
      }.toMap

      val generatedMethods = equals :: hashCode :: copy :: productDefs ::: withFields ::: List(toString)
      generatedMethods.collect {
        case m@Defn.Def(_, n, _, _, _, _) if existingDefs.get(n.value).forall(!matchesSignature(m, _)) => m
        case v@Defn.Val(_, List(Pat.Var(n)), _, _) if !existingDefs.contains(n.value) => v
      }
    }

    val template = cls.templ.copy(stats = Nil, inits = cls.templ.inits.filterNot(t => inits.exists(_.name == t)) ::: inits)
    val modified = cls.copy(mods = cls.mods ::: List(Mod.Final()), ctor = ctor, templ = template)

    val code =
      s"""|$modified {
          |${methods.mkString("\n", "\n", "\n")}
          |${cls.templ.stats.mkString("\n")}
          |}
          |""".stripMargin
    Patch.replaceTree(cls, code)
  }

  def generateCompanion(cls: Defn.Class) = {
    val typeparams = cls.tparams
    val fieldsWithDefault = cls.ctor.paramss.head.map(p => Term.Name(p.name.value) -> p.default)
    val fields = cls.ctor.paramss.head.map(p => Term.Name(p.name.value))
    val apply2Fields = fieldsWithDefault.map { case (name, default) => default.getOrElse(name) }

    val first = Defn.Def(Nil, Term.Name("apply"), typeparams, cls.ctor.paramss.map(_.map(_.copy(default = None))), Some(cls.name), q"new ${cls.name}(..$fields)")

    val rest = if (cls.ctor.paramss.exists(_.exists(_.default.isDefined))) {
      List(
        Defn.Def(Nil, Term.Name("apply"), typeparams, cls.ctor.paramss.map(_.collect { case p if p.default.isEmpty => p }), Some(cls.name), q"new ${cls.name}(..$apply2Fields)")
      )
    }
    else {
      Nil
    }

    val stats = first :: rest

    val block = stats.mkString("\n")
    val code =
      s"""|object ${cls.name.value} {
          |$block
          |}
          |""".stripMargin
    Patch.addRight(cls, code)
  }

  override def fix(implicit doc: SemanticDocument): Patch = {
    val allAnnotationedClasses = doc.tree.collect {
      case DataAnnotation(cls) =>
        generateClass(cls) + generateCompanion(cls)
    }
    Patch.fromIterable(allAnnotationedClasses)
  }

}
