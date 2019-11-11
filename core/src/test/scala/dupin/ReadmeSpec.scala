package dupin

import dupin.base._

object ReadmeSpec {
    case class Login(value: String) extends AnyVal
    case class RegistrationForm(login: Login, age: Int, password: String, passwordAgain: String)
    case class LoginForm(login: Login, password: String)

    val AgeErrorMessage: BaseMessage[Int] =
        c => s"you can't have ${c.value} years"
    val PasswordsErrorMessage: BaseMessage[Any] =
        _ => "passwords should be equal"
    val LoginFormErrorMessage: BaseMessage[Any] =
        _ => "invalid login or password"

    implicit val loginValidator = BaseValidator[Login].root(
        _.value.matches("[a-zA-Z]{2,16}"),
        c => s"${c.path} should contain only letters and have from 2 to 16 characters"
    )

    private val positive = BaseValidator[Int].root(_ > 0, _ + " should be positive")
    private def max(v: Int) = BaseValidator[Int].root(_ <= v, _ + s" should be less then $v")
    implicit val registrationFormValidator = BaseValidator[RegistrationForm]
        .combineP(_.login)(implicitly)
        .combineP(_.age)(positive && max(100))
        .combinePR(_.login)(_.value == "", _ => "")
        .combineR(a => a.password == a.passwordAgain, PasswordsErrorMessage)


    implicit val loginFormValidator = (
        BaseValidator[LoginForm].path(_.login)(implicitly) &&
            BaseValidator[LoginForm].path(_.login)(Validator.root(_.value.nonEmpty, _ => "")) &&
            BaseValidator[LoginForm].root(_.password == "", _ => "")
        ).recoverAsF(LoginFormErrorMessage)
}
