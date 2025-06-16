lazy val V = _root_.scalafix.sbt.BuildInfo

val rulesCrossVersions = Seq(V.scala212, V.scala213)
val scala3Version = "3.3.4"

inThisBuild(
  List(
    tlBaseVersion := "0.3",
    organization := "net.hamnaberg",
    homepage := Some(url("https://github.com/hamnis/dataclass-scalafix")),
    startYear := Some(2022),
    licenses := Seq(License.Apache2),
    developers := List(
      Developer(
        "hamnis",
        "Erlend Hamnaberg",
        "erlend@hamnaberg.net",
        url("https://github.com/hamnis"),
      )
    ),
    sonatypeCredentialHost := xerial.sbt.Sonatype.sonatypeLegacy,
    crossScalaVersions := rulesCrossVersions,
    scalaVersion := crossScalaVersions.value.head,
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
  )
)

lazy val `data` = (project in file("."))
  .aggregate(
    rules.projectRefs ++
      input.projectRefs ++
      output.projectRefs ++
      annotation.projectRefs ++
      tests.projectRefs: _*
  )
  .enablePlugins(NoPublishPlugin)

lazy val annotation = projectMatrix
  .settings(
    moduleName := "dataclass-annotation"
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(rulesCrossVersions :+ scala3Version)

lazy val rules = projectMatrix
  .settings(
    moduleName := "dataclass-scalafix",
    libraryDependencies += "ch.epfl.scala" %% "scalafix-core" % V.scalafixVersion,
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(rulesCrossVersions)

lazy val input = projectMatrix
  .dependsOn(annotation)
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(scalaVersions = rulesCrossVersions :+ scala3Version)
  .enablePlugins(NoPublishPlugin)

lazy val output = projectMatrix
  .dependsOn(annotation)
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(scalaVersions = rulesCrossVersions :+ scala3Version)
  .enablePlugins(NoPublishPlugin)

lazy val testsAggregate = Project("tests", file("target/testsAggregate"))
  .aggregate(tests.projectRefs: _*)
  .settings(
    publish / skip := true
  )

lazy val tests = projectMatrix
  .settings(
    libraryDependencies += "ch.epfl.scala" % "scalafix-testkit" % V.scalafixVersion % Test cross CrossVersion.full,
    scalafixTestkitOutputSourceDirectories :=
      TargetAxis
        .resolve(output, Compile / unmanagedSourceDirectories)
        .value,
    scalafixTestkitInputSourceDirectories :=
      TargetAxis
        .resolve(input, Compile / unmanagedSourceDirectories)
        .value,
    scalafixTestkitInputClasspath :=
      TargetAxis.resolve(input, Compile / fullClasspath).value,
    scalafixTestkitInputScalacOptions :=
      TargetAxis.resolve(input, Compile / scalacOptions).value,
    scalafixTestkitInputScalaVersion :=
      TargetAxis.resolve(input, Compile / scalaVersion).value,
  )
  .defaultAxes(
    rulesCrossVersions.map(VirtualAxis.scalaABIVersion) :+ VirtualAxis.jvm: _*
  )
  .customRow(
    scalaVersions = Seq(V.scala213),
    axisValues = Seq(TargetAxis(V.scala213), VirtualAxis.jvm),
    settings = Seq(),
  )
  .customRow(
    scalaVersions = Seq(V.scala213),
    axisValues = Seq(TargetAxis(scala3Version), VirtualAxis.jvm),
    settings = Seq(),
  )
  .customRow(
    scalaVersions = Seq(V.scala212),
    axisValues = Seq(TargetAxis(V.scala212), VirtualAxis.jvm),
    settings = Seq(),
  )
  .dependsOn(rules)
  .enablePlugins(ScalafixTestkitPlugin, NoPublishPlugin)
