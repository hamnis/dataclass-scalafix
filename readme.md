# Scalafix rules for dataclass

### Usage

The use of scalafix as a source generator is documented in [olafurpg/scalafix-codegen][1].
Since we need to produce SemanticDB for the data class source, but we want to
avoid infinite loop of generation, we need to split data classes into another subproject.

```scala
ThisBuild / scalaVersion      := "3.1.3"
ThisBuild / version           := "0.1.0-SNAPSHOT"
ThisBuild / semanticdbEnabled := true

def dataclassGen(data: Reference) = Def.taskDyn {
  val root = (ThisBuild / baseDirectory).value.toURI.toString
  val from = (data / Compile / sourceDirectory).value
  val to = (Compile / sourceManaged).value
  val outFrom = from.toURI.toString.stripSuffix("/").stripPrefix(root)
  val outTo = to.toURI.toString.stripSuffix("/").stripPrefix(root)
  val rule = url("https://raw.githubusercontent.com/hamnis/dataclass-scalafix/9d1bc56b0b53c537293f1218d5600a2e987ee82a/rules/src/main/scala/fix/GenerateDataClass.scala")
  (data / Compile / compile).value
  Def.task {
    (data / Compile / scalafix)
      .toTask(s" --rules $rule --out-from=$outFrom --out-to=$outTo")
      .value
    (to ** "*.scala").get
  }
}

lazy val app = project
  .settings(
    Compile / sourceGenerators += dataclassGen(definitions).taskValue,
  )

// put data classes here
lazy val definitions = project
```

Optionally apply [scalafmt](https://scalameta.org/scalafmt/) to make it somewhat readable.

#### Annotations.scala

Add the following file to both the subprojects:

```scala
package dataclass

import scala.annotation.StaticAnnotation

class data extends StaticAnnotation

class since extends StaticAnnotation
```

### To develop rule

```
sbt ~tests/test
# edit rules/src/main/scala/fix/GenerateDataClass.scala
```

  [1]: https://github.com/olafurpg/scalafix-codegen
