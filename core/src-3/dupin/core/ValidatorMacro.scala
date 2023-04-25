package dupin.core

import cats.Applicative
import scala.quoted.*

object ValidatorMacro {
    def runWithFieldPath[A : Type](using q: Quotes)(run: Expr[Path => A], f: Expr[_ => _]): Expr[A] =
        '{${run}(${getFieldPath(f)})}

    def getFieldPath(using q: Quotes)(f: Expr[_ => _]): Expr[Path] = {
        import q.reflect.*
        def abort = report.throwError(s"Unable to retrieve field path from function ${f.show}")
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

    def derive[F[_] : Type, E : Type, A : Type](using q: Quotes)(A: Expr[Applicative[F]]): Expr[Validator[F, E, A]] = {
        import q.reflect.*
        val atpe = TypeRepr.of[A]
        atpe.typeSymbol.fieldMembers.sortBy(_.fullName).map(_.tree).collect {
            case m: ValDef => m
        }.foldLeft('{Validator[F, E].success[A]($A)}) { case (t, m) =>
            atpe.memberType(m.symbol).asType match { case '[t] =>
                val resolvedValidator = Implicits.search(TypeRepr.of[Validator[F, E, t]]) match {
                    case iss: ImplicitSearchSuccess => iss.tree.asExpr.asInstanceOf[Expr[Validator[F, E, t]]]
                    case isf: ImplicitSearchFailure => report.errorAndAbort(isf.explanation)
                }
                '{
                    $t.combinePE(
                        Path(FieldPart(${Literal(StringConstant(m.symbol.name)).asExprOf[String]})),
                        a => ${Select('a.asTerm, m.symbol).asExprOf[t]}
                    )($resolvedValidator)($A)
                }
            }
        }
    }
}
