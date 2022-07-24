import mill._
import mill.scalalib._
import mill.scalajslib._
import mill.scalanativelib._
import mill.scalalib.publish._

object versions {
    val publish = "0.4.1"

    val scala212 = "2.12.16"
    val scala213 = "2.13.8"
    val scala3 = "3.1.3"
    val scalaJs = "1.10.1"
    val scalaNative = "0.4.5"
    val scalatest = "3.2.12"
    val cats = "2.8.0"

    val cross = Seq(scala212, scala213, scala3)
}

object core extends Module {
    trait CommonCoreModule extends PublishModule with CrossScalaModule with CrossScalaVersionRanges {
        override def artifactName = "dupin-core"
        override def publishVersion = versions.publish
        override def pomSettings = PomSettings(
            description = artifactName(),
            organization = "com.github.yakivy",
            url = "https://github.com/yakivy/dupin",
            licenses = Seq(License.MIT),
            versionControl = VersionControl.github("yakivy", "dupin"),
            developers = Seq(Developer("yakivy", "Yakiv Yereskovskyi", "https://github.com/yakivy"))
        )
        override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
            ivy"org.typelevel::cats-core:${versions.cats}",
        ) ++ (
            if (crossScalaVersion == versions.scala3) Agg.empty[Dep]
            else Agg(ivy"org.scala-lang:scala-reflect:${scalaVersion()}")
        )
        override def millSourcePath = super.millSourcePath / os.up
    }
    trait CommonTestModule extends ScalaModule with TestModule {
        override def ivyDeps = super.ivyDeps() ++ Agg(
            ivy"org.scalatest::scalatest::${versions.scalatest}",
            ivy"org.typelevel::cats-core::${versions.cats}",
        )
        override def testFramework = "org.scalatest.tools.Framework"
    }
    object jvm extends Cross[JvmModule](versions.cross: _*)
    class JvmModule(val crossScalaVersion: String) extends CommonCoreModule {
        object test extends CommonTestModule with Tests
    }

    object js extends Cross[JsModule](versions.cross: _*)
    class JsModule(val crossScalaVersion: String) extends CommonCoreModule with ScalaJSModule {
        def scalaJSVersion = versions.scalaJs
        object test extends CommonTestModule with Tests
    }

    object native extends Cross[NativeModule](versions.cross: _*)
    class NativeModule(val crossScalaVersion: String) extends CommonCoreModule with ScalaNativeModule {
        def scalaNativeVersion = versions.scalaNative
        object test extends CommonTestModule with Tests
    }
}