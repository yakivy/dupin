lazy val versions = new {
    val scala213 = "2.13.1"
    val scala212 = "2.12.8"
    val cats = "2.0.0"
}

lazy val commonSettings = Seq(
    scalaVersion := versions.scala213,
    crossScalaVersions := Seq(versions.scala212, versions.scala213),
)

lazy val commonDependencies = Seq(
    libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-core" % versions.cats % "test,provided",
        "org.scala-lang" % "scala-reflect" % scalaVersion.value
    ),
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.0" cross CrossVersion.full)
)

lazy val root = project.in(file(".")).settings(commonSettings: _*)

lazy val core = project.in(file("core")).settings(commonSettings: _*).settings(commonDependencies: _*)

