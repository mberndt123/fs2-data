val scala212 = "2.12.12"
val scala213 = "2.13.3"
val fs2Version = "2.4.4"
val circeVersion = "0.13.0"
val shapelessVersion = "2.3.3"

val commonSettings = List(
  scalaVersion := scala212,
  crossScalaVersions := Seq(scala213, scala212),
  organization := "org.gnieh",
  version := "0.8.0-SNAPSHOT",
  licenses += ("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/satabin/fs2-data")),
  scalacOptions ++= List("-feature",
                         "-deprecation",
                         "-unchecked",
                         "-Ypatmat-exhaust-depth",
                         "off",
                         "-Ywarn-unused:imports"),
  scalacOptions ++= PartialFunction
    .condOpt(CrossVersion.partialVersion(scalaVersion.value)) {
      case Some((2, n)) if n < 13 =>
        List("-Ypartial-unification", "-language:higherKinds")
    }
    .toList
    .flatten,
  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary),
  addCompilerPlugin("com.olegpy" % "better-monadic-for" % "0.3.1" cross CrossVersion.binary),
  libraryDependencies ++= List(
    "co.fs2" %%% "fs2-core" % fs2Version,
    "org.scala-lang.modules" %%% "scala-collection-compat" % "2.2.0",
    "io.circe" %%% "circe-parser" % circeVersion % "test",
    "co.fs2" %% "fs2-io" % fs2Version % "test",
    "com.disneystreaming" %%% "weaver-framework" % "0.5.0-RC2" % "test"
  ),
  testFrameworks += new TestFramework("weaver.framework.TestFramework"),
  scmInfo := Some(ScmInfo(url("https://github.com/satabin/fs2-data"), "scm:git:git@github.com:satabin/fs2-data.git"))
)

val publishSettings = List(
  publishMavenStyle := true,
  publishArtifact in Test := false,
  // The Nexus repo we're publishing to.
  publishTo := Some(
    if (isSnapshot.value)
      Opts.resolver.sonatypeSnapshots
    else
      Opts.resolver.sonatypeStaging
  ),
  pomIncludeRepository := { x =>
    false
  },
  developers := List(
    Developer(id = "satabin",
              name = "Lucas Satabin",
              email = "lucas.satabin@gnieh.org",
              url = url("https://github.com/satabin"))
  ),
  pomExtra := (
    <ciManagement>
      <system>travis</system>
      <url>https://travis-ci.org/#!/satabin/fs2-data</url>
    </ciManagement>
    <issueManagement>
      <system>github</system>
      <url>https://github.com/satabin/fs2-data/issues</url>
    </issueManagement>
  )
)

val root = (project in file("."))
  .settings(commonSettings)
  .enablePlugins(ScalaUnidocPlugin, SiteScaladocPlugin, NanocPlugin, GhpagesPlugin)
  .settings(
    name := "fs2-data",
    publishArtifact := false,
    skip in publish := true,
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(benchmarks),
    siteSubdirName in ScalaUnidoc := "api",
    addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), siteSubdirName in ScalaUnidoc),
    Nanoc / sourceDirectory := file("site"),
    git.remoteRepo := scmInfo.value.get.connection.replace("scm:git:", ""),
    ghpagesNoJekyll := true
  )
  .aggregate(
    text.jvm,
    text.js,
    csv.jvm,
    csv.js,
    csvGeneric.jvm,
    csvGeneric.js,
    json.jvm,
    json.js,
    jsonCirce.jvm,
    jsonCirce.js,
    jsonDiffson.jvm,
    jsonDiffson.js,
    jsonInterpolators,
    xml.jvm,
    xml.js
  )

lazy val text = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .in(file("text"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "fs2-data-text",
    description := "Utilities for textual data format"
  )

lazy val csv = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .in(file("csv"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(name := "fs2-data-csv", description := "Streaming CSV manipulation library")
  .jsSettings(libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.0.0" % Test)
  .dependsOn(text)

lazy val csvGeneric = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("csv/generic"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "fs2-data-csv-generic",
    description := "Generic CSV row decoder generation",
    libraryDependencies ++= List(
      "com.chuusai" %%% "shapeless" % shapelessVersion,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    ),
    libraryDependencies ++=
      (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v <= 12 =>
          Seq(
            compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
          )
        case _ =>
          // if scala 2.13.0 or later, macro annotations merged into scala-reflect
          Nil
      }),
    scalacOptions ++= PartialFunction
      .condOpt(CrossVersion.partialVersion(scalaVersion.value)) {
        case Some((2, n)) if n >= 13 =>
          Seq(
            "-Ymacro-annotations"
          )
      }
      .toList
      .flatten
  )
  .jsSettings(libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.0.0" % Test)
  .dependsOn(csv)

lazy val json = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .in(file("json"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(name := "fs2-data-json", description := "Streaming JSON manipulation library")
  .dependsOn(text)

lazy val jsonCirce = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .in(file("json/circe"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "fs2-data-json-circe",
    description := "Streaming JSON library with support for circe ASTs",
    libraryDependencies ++= List(
      "io.circe" %%% "circe-core" % circeVersion,
      "org.gnieh" %%% "diffson-circe" % "4.0.3" % "test"
    )
  )
  .dependsOn(json % "compile->compile;test->test", jsonDiffson % "test->test")

lazy val jsonDiffson = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("json/diffson"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "fs2-data-json-diffson",
    description := "Streaming JSON library with support for patches",
    libraryDependencies ++= List(
      "org.gnieh" %%% "diffson-core" % "4.0.3"
    )
  )
  .dependsOn(json % "compile->compile;test->test")

lazy val jsonInterpolators = project
  .in(file("json/interpolators"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "fs2-data-json-interpolators",
    description := "Json interpolators support",
    libraryDependencies ++= List(
      "com.propensive" %% "contextual" % "1.2.1",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    )
  )
  .dependsOn(json.jvm % "compile->compile;test->test")

lazy val xml = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .in(file("xml"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "fs2-data-xml",
    description := "Streaming XML manipulation library"
  )
  .dependsOn(text)

lazy val documentation = project
  .in(file("documentation"))
  .enablePlugins(MdocPlugin)
  .settings(commonSettings)
  .settings(
    mdocIn := file("documentation/docs"),
    mdocOut := file("site/content/documentation"),
    libraryDependencies ++= List(
      "com.beachape" %% "enumeratum" % "1.5.15",
      "org.gnieh" %% "diffson-circe" % "4.0.3",
      "io.circe" %% "circe-generic-extras" % circeVersion,
      "co.fs2" %% "fs2-io" % fs2Version
    )
  )
  .dependsOn(csv.jvm, csvGeneric.jvm, json.jvm, jsonDiffson.jvm, jsonCirce.jvm, jsonInterpolators, xml.jvm)

lazy val benchmarks = project
  .in(file("benchmarks"))
  .enablePlugins(JmhPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= List(
      "com.github.pathikrit" %% "better-files" % "3.9.1"
    )
  )
  .dependsOn(csv.jvm)