package dupin.core

import cats.Applicative
import scala.quoted.*

object ValidatorMacro {
    def runWithFieldPath[A : Type](using q: Quotes)(run: Expr[Path => A], f: Expr[_ => _]): Expr[A] =
        '{${run}(${getFieldPath(f)})}

    def getFieldPath(using q: Quotes)(f: Expr[_ => _]): Expr[Path] = {
        import q.reflect.*
        def abort = report.throwError(s"Unable to retrieve field name from function ${f.show}")
        def rec(argName: String, selects: Tree, acc: Expr[Path]): Expr[Path] = selects match {
            case Ident(identName) if identName == argName => acc
            case Select(qualifier, name) => '{
                ${rec(argName, qualifier, acc).asExprOf[Path]}
                    .append(FieldPart(${Literal(StringConstant(name)).asExprOf[String]}))
            }
            case _ => abort
        }
        f.asTerm match {
            case Inlined(_, _, Lambda(List(ValDef(argName, _, _)), selects)) =>
                rec(argName, selects, '{ Path.empty })
            case _ => abort
        }
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
