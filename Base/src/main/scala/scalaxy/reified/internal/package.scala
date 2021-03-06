package scalaxy.reified

import scala.language.experimental.macros

import scala.reflect._
import scala.reflect.macros.blackbox.Context
import scala.reflect.runtime.universe

import scalaxy.reified.internal.Utils._
import scalaxy.generic.Generic

package object internal {

  private[reified] lazy val verbose =
    System.getProperty("scalaxy.reified.verbose") == "true" ||
      System.getenv("SCALAXY_REIFIED_VERBOSE") == "1"

  private[reified] val syntheticVariableNamePrefix = "scalaxy$reified$"

  private[this]
  def runtimeExpr
      [A: c.WeakTypeTag]
      (c: Context)
      (tree: c.universe.Tree)
      : c.Expr[universe.Expr[A]] = {
    import c.universe._

    c.Expr[universe.Expr[A]](
      c.reifyTree(
        c.universe.internal.gen.mkRuntimeUniverseRef,
        c.universe.EmptyTree,
        tree
      )
    )
  }

  private[reified]
  def reifiedMacro[A: universe.TypeTag]
                  (v: A)
                  : Reified[A] =
    macro reifiedImpl[A]

  private[reified]
  def reifiedWithDifferentRuntimeValue[A: universe.TypeTag]
                                      (v: A, runtimeValue: A)
                                      : Reified[A] =
    macro reifyWithDifferentRuntimeValueImpl[A]

  def reifiedImpl
      [A: c.WeakTypeTag]
      (c: Context)
      (v: c.Expr[A])
      (tt: c.Expr[universe.TypeTag[A]])
      : c.Expr[Reified[A]] = {
    import c.universe._

    val expr = runtimeExpr[A](c)(c.typecheck(v.tree, pt = weakTypeTag[A].tpe))
    // println("COMPILING EXPR[" + weakTypeTag[A].tpe + "] = " + v.tree)

    c.Expr[Reified[A]](q"""
      implicit val ${TermName(c.freshName("tt"))} = $tt
      new scalaxy.reified.Reified[${weakTypeOf[A]}]($v, $expr)
    """)
  }

  def hasReifiedValueToReifiedValueImpl
      [A: c.WeakTypeTag]
      (c: Context)
      (r: c.Expr[HasReified[A]])
      (tt: c.Expr[universe.TypeTag[A]]) = {
    import c.universe._

    c.Expr[Reified[A]](q"$r.reifiedValue")
  }

  def hasReifiedValueToValueImpl
      [A: c.WeakTypeTag]
      (c: Context)
      (r: c.Expr[HasReified[A]])
      (tt: c.Expr[universe.TypeTag[A]])
      : c.Expr[A] = {
    import c.universe._

    c.Expr[A](q"$r.reifiedValue.value")
  }

  def reifyWithDifferentRuntimeValueImpl
      [A: c.WeakTypeTag]
      (c: Context)
      (v: c.Expr[A], runtimeValue: c.Expr[A])
      (tt: c.Expr[universe.TypeTag[A]]) = {

    import c.universe._

    val expr = runtimeExpr[A](c)(c.typecheck(v.tree))
    c.Expr[Reified[A]](q"""
      implicit val ${TermName(c.freshName("tt"))} = $tt
      new scalaxy.reified.Reified[${weakTypeOf[A]}](
        $runtimeValue,
        scalaxy.reified.internal.Utils.typeCheck($expr, valueTag.tpe))
    """)
  }
}
