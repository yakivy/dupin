import mill._
import mill.scalalib._
import mill.scalajslib._
import mill.scalanativelib._
import mill.scalalib.publish._

object versions {
    val publish = "0.5.0"

    val scala212 = "2.12.17"
    val scala213 = "2.13.10"
    val scala3 = "3.2.1"
    val scalaJs = "1.12.0"
    val scalaNative = "0.4.8"

    val cats = "2.9.0"
    val kindProjector = "0.13.2"
    val scalatest = "3.2.14"
    val disciplineScalatest = "2.2.0"

    val cross = Seq(scala212, scala213, scala3)
}

object core extends Module {
    trait CommonModule extends ScalaModule {
        override def scalacPluginIvyDeps = T {
            super.scalacPluginIvyDeps() ++ {
                if (scalaVersion() == versions.scala3) Agg.empty[Dep]
                else Agg(ivy"org.typelevel:::kind-projector:${versions.kindProjector}")
            }
        }
        override def scalacOptions = T {
            super.scalacOptions() ++ {
                if (scalaVersion() == versions.scala3) Seq("-Ykind-projector")
                else Seq.empty[String]
            }
        }
    }
    trait CommonCoreModule extends CommonModule with PublishModule with CrossScalaModule with CrossScalaVersionRanges {
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
        override def millSourcePath = super.millSourcePath / os.up
        override def compileIvyDeps = T {
            super.compileIvyDeps() ++ Agg(
                ivy"org.typelevel::cats-core:${versions.cats}",
            ) ++ (
                if (scalaVersion() == versions.scala3) Agg.empty[Dep]
                else Agg(ivy"org.scala-lang:scala-reflect:${scalaVersion()}")
            )
        }
    }
    trait CommonTestModule extends CommonModule with TestModule {
        override def ivyDeps = super.ivyDeps() ++ Agg(
            ivy"org.scalatest::scalatest::${versions.scalatest}",
            ivy"org.typelevel::cats-core::${versions.cats}",
            ivy"org.typelevel::cats-laws::${versions.cats}",
            ivy"org.typelevel::discipline-scalatest::${versions.disciplineScalatest}",
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