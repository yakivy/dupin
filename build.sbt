lazy val versions = new {
    val scala213 = "2.13.1"
    val scala212 = "2.12.10"
    val cats = "2.0.0"
    val scalatest = "3.0.8"
}

lazy val commonSettings = Seq(
    organization := "com.github.yakivy",
    scmInfo := Some(ScmInfo(
        url("https://github.com/yakivy/dupin"),
        "scm:git@github.com:yakivy/dupin.git"
    )),
    developers := List(Developer(
        id = "yakivy",
        name = "Yakiv Yereskovskyi",
        email = "yakiv.yereskovskyi@gmail.com",
        url = url("https://github.com/yakivy")
    )),
    licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.html")),
    homepage := Some(url("https://github.com/yakivy/dupin")),
    scalaVersion := versions.scala213,
    crossScalaVersions := Seq(versions.scala212, versions.scala213),
)

lazy val commonDependencies = Seq(
    libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-core" % versions.cats % "test,provided",
        "org.scalatest" %% "scalatest" % versions.scalatest % "test",
        "org.scala-lang" % "scala-reflect" % scalaVersion.value
    )
)

lazy val publishingSettings = Seq(
    publishTo := {
        val nexus = "https://oss.sonatype.org/"
        if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
        else Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true,
    pomIncludeRepository := { _ => false },
)

lazy val root = project.in(file("."))
    .settings(name := "dupin")
    .settings(publishingSettings: _*)
    .settings(publish / skip := true)
    .aggregate(core)

lazy val core = project.in(file("core"))
    .settings(name := "dupin-core")
    .settings(commonSettings: _*)
    .settings(commonDependencies: _*)
    .settings(publishingSettings: _*)

