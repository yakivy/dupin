## Dupin
[![Maven Central](https://img.shields.io/maven-central/v/com.github.yakivy/dupin-core_2.12.svg)](https://mvnrepository.com/search?q=dupin)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/oss.sonatype.org/com.github.yakivy/dupin-core_2.13.svg)](https://oss.sonatype.org/content/repositories/snapshots/com/github/yakivy/dupin-core_2.13/)
[![Build Status](https://travis-ci.com/yakivy/dupin.svg?branch=master)](https://travis-ci.com/yakivy/dupin)
[![codecov.io](https://codecov.io/gh/yakivy/dupin/branch/master/graphs/badge.svg?branch=master)](https://codecov.io/github/yakivy/dupin/branch/master)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

Dupin is a minimal, idiomatic, customizable validation for Scala.

### Table of contents
1. [Motivation](#motivation)
1. [Quick start](#quick-start)
1. [Predefined validators](#predefined-validators)
1. [Customization](#message-customization)
    1. [Message customization](#message-customization)
    1. [Kind customization](#kind-customization)
    1. [Custom validating package](#custom-validating-package)
1. [Integration example](#integration-example)
1. [Notes](#notes)

### Motivation

You may find Dupin useful if you want to...
- return something richer than `String` as validation message
- use custom data kind for validation (`Future`, `IO`, etc...)
- reuse validation parts across whole project

### Quick start
Add dupin dependency to the build file, let's assume you are using sbt:
```scala
libraryDependencies += Seq(
  "com.github.yakivy" %% "dupin-core" % "0.1.3"
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
import cats.implicits._

implicit val nameValidator = BaseValidator[Name]
    .root(_.value.nonEmpty, _.path + " should be non empty")

implicit val memberValidator = BaseValidator[Member]
    .combineP(_.name)(nameValidator)
    .combinePR(_.age)(a => a > 18 && a < 40, _.path + " should be between 18 and 40")

implicit val teamValidator = BaseValidator[Team]
    .combinePI(_.name)
    .combineP(_.members)(element(memberValidator))
    .combineR(_.members.size <= 8, _ => "team should be fed with two pizzas!")
```
Validate them all:
```scala
import dupin.all._

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

### Predefined validators

The more validators you have, the more logic can be reused without writing validators from the scratch. Let's write common validators for minimum and maximum `Int` value:
```scala
import dupin.all._

def min(value: Int) = BaseValidator[Int].root(_ > value, _.path + " should be grater then " + value)
def max(value: Int) = BaseValidator[Int].root(_ < value, _.path + " should be less then " + value)
``` 
And since validators can be combined, you can create validators from other validators:
```scala
import dupin.all._

implicit val memberValidator = BaseValidator[Member].path(_.age)(min(18) && max(40))

val invalidMember = Member(Name("Ada"), 0)
val messages = invalidMember.validate.list

assert(messages == List(".age should be grater then 18"))
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
As `BaseValidator[R]` is just a type alias for `Validator[String, R, Id]`, you can define own validator type with builder:
```scala
import dupin.all._

type I18nValidator[R] = Validator[I18nMessage, R, cats.Id]
def I18nValidator[R] = Validator[I18nMessage, R, cats.Id]
```
And start creating validators with custom messages:
```scala
import dupin.all._

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
Then validation messages will look like:
```scala
import dupin.all._

val invalidMember = Member(Name(""), 0)
val messages: List[I18nMessage] = invalidMember.validate.list

assert(messages == List(
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
))
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
So to be able to handle checks that returns `Future[Boolean]`, you just need to define own validator type with builder:
```scala
import cats.Applicative
import dupin.all._
import scala.concurrent.Future

type FutureValidator[R] = Validator[String, R, Future]
def FutureValidator[R](implicit A: Applicative[Future]) = Validator[String, R, Future]
``` 
Then you can create validators with generic dsl (don't forget to import required type classes, as minimum `Functor[Future]`):
```scala
import cats.implicits._
import dupin.all._
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
And validation result will look like:
```scala
import cats.data.NonEmptyList
import cats.implicits._
import dupin.all._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

val invalidMember = Member(Name(""), 0)
val messages: Future[Either[NonEmptyList[String], Member]] = invalidMember.validate.map(_.either)

assert(Await.result(messages, Duration.Inf) == Left(NonEmptyList.of(
    ".name should be non empty",
    ".age should be between 18 and 40"
)))
```

### Custom validating package

To avoid imports boilerplate and isolating all customizations you can define own dupin package:
```scala
package dupin

import cats.Applicative
import cats.instances.FutureInstances
import dupin.instances.DupinInstances
import dupin.syntax.DupinSyntax
import scala.concurrent.Future

package object custom
    extends DupinCoreDsl with DupinInstances with DupinSyntax
        with FutureInstances {
    type CustomValidator[R] = Validator[I18nMessage, R, Future]
    def CustomValidator[R](implicit A: Applicative[Future]) = Validator[I18nMessage, R, Future]
}
```
Then you can start using own validator type with single import:
```scala
import dupin.custom._
import scala.concurrent.ExecutionContext.Implicits.global

val nameService = new NameService

implicit val nameValidator = CustomValidator[Name](
    n => nameService.contains(n.value), c => I18nMessage(
        c.path + " should be non empty",
        "validator.name.empty",
        List(c.path.toString())
    )
)

val validName = Name("Ada")

assert(Await.result(validName.isValid, Duration.Inf))
```

### Integration example
For example you are using play framework in your project, then instead of parsing dtos directly from `JsValue`, you can create own wrapper around it and inject validation logic there, like:
```scala
import cats.data.NonEmptyList
import dupin.all._
import dupin.core.Success
import play.api.libs.json.JsValue
import play.api.libs.json.Reads

case class JsonContent(jsValue: JsValue) {
    def as[A: Reads](
        implicit validator: BaseValidator[A] = BaseValidator.success
    ): Either[NonEmptyList[String], A] = {
        jsValue.validate[A].asEither match {
            case Right(value) => validator.validate(value).either
            case Left(errors) => Left(NonEmptyList(errors.toString, Nil))
        }
    }
}
```
`= BaseValidator.success` - will allow you to successfully parse dtos that don't have validator
