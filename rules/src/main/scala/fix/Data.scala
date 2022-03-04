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

    val template = cls.templ.copy(stats = equals :: hashCode :: copy :: withFields ::: List(toString) ::: cls.templ.stats)
    cls.copy(mods = Mod.Final() :: cls.mods.filterNot(_ == mod), ctor = ctor, templ = template)
  }

  override def fix(implicit doc: SemanticDocument): Patch = {
    //println("Tree.syntax: " + doc.tree.syntax)
    //println("Tree.structure: " + doc.tree.structure)
    //println("Tree.structureLabeled: " + doc.tree.structureLabeled)

    val allAnnotationedClasses = doc.tree.collect{
      case AnnotationName((mod, c)) =>
        Patch.replaceTree(c, modify(mod, c).toString())
        //val fields = modified.ctor.paramss.head
    }
    //allAnnotationedClasses.foreach(println)
    Patch.fromIterable(allAnnotationedClasses)

    //Patch.empty
  }

}
