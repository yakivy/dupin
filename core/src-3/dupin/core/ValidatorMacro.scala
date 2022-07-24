package dupin.core

import cats.Applicative
import scala.quoted.*

object ValidatorMacro {
    def runWithFieldName[A : Type](using q: Quotes)(run: Expr[String => A], f: Expr[_ => _]): Expr[A] =
        '{${run}(${getFieldName(f)})}

    def getFieldName(using q: Quotes)(f: Expr[_ => _]): Expr[String] = {
        import q.reflect.*
        (f.asTerm match {
            case Inlined(_, _, Lambda(_, Select(_, name))) => Literal(StringConstant(name))
            case _ => report.throwError(s"Unable to retrieve field name from function ${f.show}")
        }).asExprOf[String]
    }

    def derive[F[_] : Type, E : Type, A : Type](
        using q: Quotes)(A: Expr[Applicative[F]]
    ): Expr[Validator[F, E, A]] = {
        import q.reflect.*
        TypeRepr.of[A].typeSymbol.memberFields.sortBy(_.fullName).map(_.tree).collect {
            case m: ValDef => (m.symbol, m.rhs.map(_.tpe).getOrElse(m.tpt.tpe))
        }.foldLeft('{Validator[F, E].success[A]($A)}) { case (t, m) =>
            m._2.asType match { case '[tpe] =>
                val resolvedValidator = Expr.summon[Validator[F, E, tpe]] match {
                    case Some(arg) => arg
                    case _ => report.throwError(s"Unable to resolve implicit validator for ${m._1.name} field")
                }
                '{$t.combinePE(
                    Path(FieldPart(${Literal(StringConstant(m._1.name)).asExprOf[String]})),
                    a => ${Select('a.asTerm, m._1).asExprOf[tpe]})(
                    $resolvedValidator)(
                    $A
                )}
            }
        }
    }
}
