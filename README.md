## Dupin
[![Maven Central](https://img.shields.io/maven-central/v/com.github.yakivy/dupin-core_2.12.svg)](https://mvnrepository.com/search?q=dupin)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/oss.sonatype.org/com.github.yakivy/dupin-core_2.13.svg)](https://oss.sonatype.org/content/repositories/snapshots/com/github/yakivy/dupin-core_2.13/)
[![Build Status](https://travis-ci.com/yakivy/dupin.svg?branch=master)](https://travis-ci.com/yakivy/dupin)
[![codecov.io](https://codecov.io/gh/yakivy/dupin/branch/master/graphs/badge.svg?branch=master)](https://codecov.io/github/yakivy/dupin/branch/master)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

Dupin is a minimal, idiomatic, customizable validation for Scala.

### Quick start
Add dupin dependency to the build file, let's assume you are using sbt:
```scala
libraryDependencies += Seq(
  "com.github.yakivy" %% "dupin-core" % "0.1.0"
)
```
Describe the domain:
```scala
case class Name(value: String)
case class Member(name: Name, age: Int)
case class Team(name: Name, members: Seq[Member])
```
Define some validators:
```scala
import dupin.all._

implicit val nameValidator = BaseValidator[Name](
    _.value.nonEmpty, _.path + " should be non empty"
)

implicit val memberValidator = BaseValidator[Member]
    .combineP(_.name)(implicitly)
    .combinePR(_.age)(_ > 0, _.path + " should be positive")

implicit val teamValidator = BaseValidator[Team]
    .combineP(_.name)(implicitly)
    .combineP(_.members)(implicitly)
    .combineR(_.members.size <= 8, _ => "team should be fed with two pizzas!")
```
Validate them all:
```scala
import dupin.all._

val validTeam = Team(
    Name("bears"),
    List(
        Member(Name("Yakiv"), 26),
        Member(Name("Myroslav"), 31),
        Member(Name("Andrii"), 25)
    )
)

val invalidTeam = Team(
    Name(""),
    Member(Name(""), -1) :: (1 to 10).map(_ => Member(Name("valid name"), 20)).toList
)

val a = validTeam.validate.either
val b = validTeam.isValid
val c = invalidTeam.validate.list

assert(a == Right(validTeam))
assert(b)
assert(c == List(
    ".name should be non empty",
    ".members.[0].name should be non empty",
    ".members.[0].age should be between 18 and 40",
    "team should be fed with two pizzas!"
))
```

### Message customization

In progress

### Kind customization

In progress

### Predefined validators

In progress

### Final custom validator

In progress

### Notes

Library is in active development and API might change.