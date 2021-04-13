## Dupin
[![Maven Central](https://img.shields.io/maven-central/v/com.github.yakivy/dupin-core_2.12.svg)](https://mvnrepository.com/search?q=dupin)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/oss.sonatype.org/com.github.yakivy/dupin-core_2.13.svg)](https://oss.sonatype.org/content/repositories/snapshots/com/github/yakivy/dupin-core_2.13/)
[![Build Status](https://travis-ci.com/yakivy/dupin.svg?branch=master)](https://travis-ci.com/yakivy/dupin)
[![codecov.io](https://codecov.io/gh/yakivy/dupin/branch/master/graphs/badge.svg?branch=master)](https://codecov.io/github/yakivy/dupin/branch/master)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Dupin is a minimal, idiomatic, customizable validation Scala library.

### Table of contents
1. [Motivation](#motivation)
1. [Quick start](#quick-start)
1. [Predefined validators](#predefined-validators)
1. [Customization](#message-customization)
    1. [Message customization](#message-customization)
    1. [Kind customization](#kind-customization)
    1. [Custom validating package](#custom-validating-package)

### Motivation

You may find Dupin useful if you...
- need to return something richer than `String` as validation message
- want to use custom data kind for validation (`Future`, `IO`, etc...)
- use [cats](https://typelevel.org/cats/) and like their API style

### Quick start
Add cats and dupin dependencies to the build file, let's assume you are using sbt:
```scala
libraryDependencies += Seq(
    "org.typelevel" %% "cats-core" % "2.0.0",
    "com.github.yakivy" %% "dupin-core" % "0.1.4",
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
import cats.implicits._
import dupin.base.all._

implicit val nameValidator = BaseValidator[Name]
    .root(_.value.nonEmpty, _.path + " should be non empty")

implicit val memberValidator = BaseValidator[Member]
    .combineP(_.name)(nameValidator)
    .combinePR(_.age)(a => a > 18 && a < 40, _.path + " should be between 18 and 40")

implicit val teamValidator = BaseValidator[Team].derive
    .combineR(_.members.size <= 8, _ => "team should be fed with two pizzas!")
```
Validate them all:
```scala
import dupin.base.all._

val validTeam = Team(
    Name("Bears"),
    List(
        Member(Name("Yakiv"), 26),
        Member(Name("Myroslav"), 31),
        Member(Name("Andrii"), 25)
    )
)

val invalidTeam = Team(
    Name(""),
    Member(Name(""), 0) :: (1 to 10).map(_ => Member(Name("valid name"), 20)).toList
)

val valid = validTeam.isValid
val result = invalidTeam.validate

assert(valid)
assert(result == Validated.invalid(NonEmptyChain(
    ".name should be non empty",
    ".members.[0].name should be non empty",
    ".members.[0].age should be between 18 and 40",
    "team should be fed with two pizzas!"
)))
```

### Predefined validators

The more validators you have, the more logic can be reused without writing validators from the scratch. Let's define common validators for minimum and maximum `Int` value:
```scala
import dupin.base.all._

def min(value: Int) = BaseValidator[Int].root(_ > value, _.path + " should be greater than " + value)
def max(value: Int) = BaseValidator[Int].root(_ < value, _.path + " should be less than " + value)
``` 
And since validators can be combined, you can create validators from other validators:
```scala
import dupin.base.all._

implicit val memberValidator = BaseValidator[Member].path(_.age)(min(18) && max(40))

val invalidMember = Member(Name("Ada"), 0)
val result = invalidMember.validate

assert(result == Validated.invalidNec(".age should be greater than 18"))
```
You can find full list of validators that provided out of the box in `dupin.instances.DupinInstances`

### Message customization

But not many real projects use strings as validation messages, for example you want to support internationalization:
```scala
case class I18nMessage(
    description: String,
    key: String,
    params: List[String]
)
```
As `BaseValidator[A]` is just a type alias for `Validator[Id, String, A]`, you can define own validator type with builder:

```scala
import dupin._

type I18nValidator[A] = Validator[I18nMessage, A, cats.Id]
def I18nValidator[A] = Validator[I18nMessage, A, cats.Id]
```
And start creating validators with custom messages:
```scala
implicit val nameValidator = I18nValidator[Name].root(_.value.nonEmpty, c => I18nMessage(
    c.path + " should be non empty",
    "validator.name.empty",
    List(c.path.toString())
))

implicit val memberValidator = I18nValidator[Member]
    .combinePI(_.name)
    .combinePR(_.age)(a => a > 18 && a < 40, c => I18nMessage(
        c.path + " should be between 18 and 40",
        "validator.member.age",
        List(c.path.toString())
    ))
```
Validation messages will look like:
```scala
import dupin.syntax._

val invalidMember = Member(Name(""), 0)
val result = invalidMember.validate

assert(result == Validated.invalid(NonEmptyChain(
    I18nMessage(
        ".name should be non empty",
        "validator.name.empty",
        List(".name")
    ),
    I18nMessage(
        ".age should be between 18 and 40",
        "validator.member.age",
        List(".age")
    )
)))
```

### Kind customization

For example you want to allow only using of limited list of names and they are stored in the database:
```scala
import scala.concurrent.Future

class NameService {
    private val allowedNames = Set("Ada")
    def contains(name: String): Future[Boolean] =
        // Emulation of DB call
        Future.successful(allowedNames(name))
}
```
So to be able to handle checks that returns `Future[Boolean]`, you just need to define your own validator type with builder:

```scala
import cats.Applicative
import dupin._
import scala.concurrent.Future

type FutureValidator[A] = Validator[Future, String, A]
def FutureValidator[A](implicit A: Applicative[Future]) = Validator[Future, String, A]
``` 
Then you can create validators with generic dsl (don't forget to import required type classes, as minimum `Functor[Future]`):
```scala
import cats.implicits._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

val nameService = new NameService

implicit val nameValidator = FutureValidator[Name].root(
    n => nameService.contains(n.value), _.path + " should be non empty"
)

implicit val memberValidator = FutureValidator[Member]
    .combinePI(_.name)
    .combinePR(_.age)(a => Future.successful(a > 18 && a < 40), _.path + " should be between 18 and 40")
```
Validation result will look like:
```scala
import dupin.syntax._

val invalidMember = Member(Name(""), 0)
val result: Future[ValidatedNec[String, Member]] = invalidMember.validate

assert(Await.result(result, Duration.Inf) == Validated.invalid(NonEmptyChain(
    ".name should be non empty",
    ".age should be between 18 and 40"
)))
```

### Custom validating package

To avoid imports boilerplate and isolating all customizations you can define your own dupin package:

```scala
import cats.Applicative
import dupin.readme.MessageCustomizationDomainFixture._
import dupin.syntax.DupinSyntax
import scala.concurrent.Future

package object custom extends DupinCoreDsl with DupinSyntax {
    type CustomValidator[A] = Validator[Future, I18nMessage, A]
    def CustomValidator[A](implicit A: Applicative[Future]) = Validator[Future, I18nMessage, A]
}
```
Then you can start using your own validator type with single import:
```scala
import dupin.custom._
import cats.implicits._
import scala.concurrent.ExecutionContext.Implicits.global

val nameService = new NameService

implicit val nameValidator = CustomValidator[Name].root(
    n => nameService.contains(n.value), c => I18nMessage(
        c.path + " should be non empty",
        "validator.name.empty",
        List(c.path.toString())
    )
)

val validName = Name("Ada")
val valid: Future[Boolean] = validName.isValid

assert(Await.result(valid, Duration.Inf))
```