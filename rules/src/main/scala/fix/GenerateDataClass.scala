package fix

import scalafix.v1._

import scala.meta._

class GenerateDataClass(config: Configuration) extends SemanticRule("GenerateDataClass") {
  val scalaBinaryVersion = ScalaBinaryVersion.fromString(config.scalaVersion)
  def this() = this(Configuration.apply())

  override def withConfiguration(config: Configuration): metaconfig.Configured[Rule] =
    metaconfig.Configured.ok(new GenerateDataClass(config))

  object DataAnnotation {
    def unapply(cls: Defn.Class): Option[(Defn.Class)] = cls.mods.collectFirst {
      case Mod.Annot(init) =>
        init.tpe match {
          case Type.Name("data") => Some(cls)
          case _ => None
        }
    }.flatten
  }

  private final val sinceStr = "@since"

  def generateClass(cls: Defn.Class) = {
    val scala212 = scalaBinaryVersion == ScalaBinaryVersion.Scala212

    val fields = cls.ctor.paramss.head.map(p => Term.Name(p.name.value))
    val fieldsWithType =
      cls.ctor.paramss.head.flatMap(p => p.decltpe.map(Term.Name(p.name.value) -> _))
    val equal = fields.map(n => q"this.${n} == c.${n}").reduce((a, b) => q"$a && $b")
    val ctor = cls.ctor.copy(
      paramss = cls.ctor.paramss.map(paramList =>
        paramList.map(param =>
          param.copy(
            mods = param.mods.filter(_.toString().startsWith(sinceStr)) ::: List(Mod.ValParam()),
            default = None,
          )
        )
      ),
      mods = List(Mod.Private(Name.Anonymous())),
    )
    val equals =
      q"""override def equals(obj: Any): Boolean = obj match {
            case c: ${cls.name} => ${equal}
            case _ => false
          }"""
    val hashCode =
      q"""override lazy val hashCode: Int = {
            val state = Seq(..$fields)
            state.foldLeft(0)((a, b) => 31 * a.hashCode() + b.hashCode())
          }"""

    val canEqual =
      q"""override def canEqual(obj: Any): Boolean = obj match {
            case c: ${cls.name} => true
            case _ => false
          }"""

    val productArity =
      q"""override def productArity = ${fields.size}"""

    val productElement = {
      val terms = fields.zipWithIndex.map { case (term, idx) => Case.apply(p"$idx", None, term) }
      q"""override def productElement(n: Int) = n match {
            ..case $terms
            case _ => throw new IndexOutOfBoundsException()
       }"""
    }

    val productElementName = {
      val terms = fields.zipWithIndex.map { case (term, idx) =>
        Case.apply(p"$idx", None, Lit.String(term.value))
      }

      val mods = if (scala212) Nil else List(Mod.Override())

      q"""..$mods def productElementName(n: Int) = n match {
           ..case $terms
           case _ => throw new IndexOutOfBoundsException()
       }"""
    }

    val productElementNames = {
      val terms = fields.map(n => Lit.String(n.value))
      val mods = if (scala212) Nil else List(Mod.Override())

      q"""..$mods def productElementNames = {
              Iterator(..${terms})
       }"""
    }

    val productIterator =
      q"""override def productIterator = {
              Iterator(..$fields)
       }"""

    val productPrefix =
      q"""override def productPrefix = ${cls.name.value}"""

    val productDefs =
      canEqual :: productArity :: productElement :: productElementName :: productElementNames :: productIterator :: productPrefix :: Nil

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

    def matchesSignature(def1: Defn.Def, def2: Defn.Def) =
      def1.mods.map(_.toString()).toSet == def2.mods.map(_.toString()).toSet &&
        def1.tparams.size == def2.tparams.size &&
        def1.paramss.flatMap(_.map(_.toString())) == def2.paramss.flatMap(_.map(_.toString()))

    def mkInit(name: String) = Init(Type.Name(name), Name.Anonymous(), Nil)

    val inits = List(mkInit("Product"), mkInit("Serializable"))
    val methods = {
      val existingDefs = cls.templ.stats.collect { case d: Defn.Def =>
        d.name.value -> d
      }.toMap

      val generatedMethods =
        equals :: hashCode :: copy :: productDefs ::: withFields ::: List(toString)
      generatedMethods.collect {
        case m @ Defn.Def(_, n, _, _, _, _)
            if existingDefs.get(n.value).forall(!matchesSignature(m, _)) =>
          m
        case v @ Defn.Val(_, List(Pat.Var(n)), _, _) if !existingDefs.contains(n.value) => v
      }
    }

    val template = cls.templ.copy(
      stats = Nil,
      inits = cls.templ.inits.filterNot(t => inits.exists(_.name == t)) ::: inits,
    )
    val modified = cls.copy(mods = cls.mods ::: List(Mod.Final()), ctor = ctor, templ = template)

    val code =
      s"""|$modified {
          |${methods.mkString("\n", "\n", "\n")}
          |${cls.templ.stats.mkString("\n")}
          |}
          |""".stripMargin
    Patch.replaceTree(cls, code)
  }

  def generateCompanion(cls: Defn.Class, objOpt: Option[Defn.Object]) = {
    val params = cls.ctor.paramss.head
    val typeparams = cls.tparams
    val fieldsWithDefault = params.map(p => Term.Name(p.name.value) -> p.default)
    val fields = params.map(p => Term.Name(p.name.value))
    val allSinceValues = (for {
      params <- cls.ctor.paramss
      param <- params
    } yield param.mods.find(m => m.toString.startsWith(sinceStr)) match {
      case Some(mod) => mod.toString
      case _ => ""
    }).distinct.sorted

    def isParamIn(param: Term.Param, sinces: Set[String]): Boolean =
      param.mods.find(m => m.toString.startsWith(sinceStr)) match {
        case Some(mod) => sinces(mod.toString)
        case None => true
      }

    def cleanParam(param: Term.Param): Term.Param =
      param.copy(
        default = None,
        mods = param.mods.filterNot(m => m.toString.startsWith(sinceStr)),
      )

    val first = Defn.Def(
      Nil,
      Term.Name("apply"),
      typeparams,
      cls.ctor.paramss.map(_.map(cleanParam)),
      Some(cls.name),
      q"new ${cls.name}(..$fields)",
    )

    val sinceApplies =
      if (allSinceValues.size > 1) {
        (for {
          i <- 0 to (allSinceValues.size - 1)
        } yield {
          val sub = allSinceValues.slice(0, i + 1).toSet
          cls.ctor.paramss.map(_.collect { case p if isParamIn(p, sub) => p })
        }).toList
      } else Nil

    val nonDefaults =
      if (cls.ctor.paramss.exists(_.exists(_.default.isDefined))) {
        List(cls.ctor.paramss.map(_.collect { case p if p.default.isEmpty => p }))
      } else Nil

    val restParamsss = (sinceApplies ::: nonDefaults).distinct.filter { paramss =>
      paramss.head.size != cls.ctor.paramss.head.size
    }
    val rest = restParamsss.map { paramss =>
      val fieldNames = paramss.head.map(_.name.value).toSet
      val applyFields = fieldsWithDefault.map { case (name, default) =>
        if (fieldNames(name.value)) name
        else default.getOrElse(sys.error(s"default value missing for $name"))
      }
      Defn.Def(
        Nil,
        Term.Name("apply"),
        typeparams,
        paramss.map(_.map(cleanParam)),
        Some(cls.name),
        q"new ${cls.name}(..$applyFields)",
      )
    }

    val stats = first :: rest.toList

    objOpt match {
      case Some(obj) => {
        val block = (obj.templ.stats ::: stats).mkString("\n")
        val code =
          s"""|object ${cls.name.value} {
              |$block
              |}
              |""".stripMargin
        Patch.replaceTree(obj, code)
      }
      case None => {
        val block = stats.mkString("\n")
        val code =
          s"""|object ${cls.name.value} {
              |$block
              |}
              |""".stripMargin
        Patch.addRight(cls, code)
      }
    }
  }

  override def fix(implicit doc: SemanticDocument): Patch = {
    def getExistingCompanion(cls: Defn.Class) = {
      val name = Term.Name(cls.name.value).structure
      doc.tree.collect {
        case o: Defn.Object => o.name.structure match {
          case `name` => Some(o)
          case _ => None
        }
      }.flatten.headOption
    }

    val allAnnotationedClasses = doc.tree.collect { case DataAnnotation(cls) =>
      generateClass(cls) + generateCompanion(cls, getExistingCompanion(cls))
    }
    Patch.fromIterable(allAnnotationedClasses)
  }

}

sealed trait ScalaBinaryVersion
object ScalaBinaryVersion {
  case object Scala3 extends ScalaBinaryVersion
  case object Scala213 extends ScalaBinaryVersion
  case object Scala212 extends ScalaBinaryVersion

  def fromString(value: String): ScalaBinaryVersion = {
    val toBinaryVersion = if (value.startsWith("3")) "3" else value.split('.').take(2).mkString(".")
    toBinaryVersion match {
      case "2.12" => Scala212
      case "2.13" => Scala213
      case "3" => Scala3
      case _ => Scala213
    }
  }
}
