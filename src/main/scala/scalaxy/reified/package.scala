package scalaxy

import scala.language.experimental.macros
import scala.language.implicitConversions

import scala.reflect._
import scala.reflect.macros.Context
import scala.reflect.runtime
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.TypeTag

import scalaxy.reified.impl
import scalaxy.reified.base.ReifiedValue

package object reified {

  type ReifiedValue[A] = base.ReifiedValue[A]
  type HasReifiedValue[A] = base.HasReifiedValue[A]

  def reify[A](v: A): ReifiedValue[A] = macro impl.reifyValue[A]

  /**
   * Wrapper that provides Function1-like methods to a reified Function1 value.
   */
  implicit class ReifiedFunction1[T1: TypeTag, R: TypeTag](
    val value: ReifiedValue[T1 => R])
      extends HasReifiedValue[T1 => R] {

    assert(value != null)

    override def reifiedValue = value

    def apply(a: T1): R = value.value(a)

    def compose[A: TypeTag](g: ReifiedFunction1[A, T1]): ReifiedFunction1[A, R] = {
      val f = this
      base.reify((c: A) => f(g(c)))
    }

    def andThen[A: TypeTag](g: ReifiedFunction1[R, A]): ReifiedFunction1[T1, A] = {
      val f = this
      base.reify((a: T1) => g(f(a)))
    }
  }

  /**
   * Wrapper that provides Function2-like methods to a reified Function2 value.
   */
  implicit class ReifiedFunction2[T1: TypeTag, T2: TypeTag, R: TypeTag](
    val value: ReifiedValue[Function2[T1, T2, R]])
      extends HasReifiedValue[Function2[T1, T2, R]] {

    assert(value != null)

    override def reifiedValue = value

    def apply(v1: T1, v2: T2): R = value.value(v1, v2)

    /*
    // TODO fix this:
    def curried: ReifiedFunction1[T1, ReifiedFunction1[T2, R]] = {
      val f = this
      def finish(v1: T1) = {
        base.reify((v2: T2) => {
          f(v1, v2)
        })
      }
      base.reify((v1: T1) => finish(v1))
    }
    */

    def tupled: ReifiedFunction1[(T1, T2), R] = {
      val f = this
      base.reify((p: (T1, T2)) => {
        val (v1, v2) = p
        f(v1, v2)
      })
    }
  }

  /**
   * Implicitly extract reified value from its wrappers (such as ReifiedFunction1, ReifiedFunction2).
   */
  implicit def hasReifiedValueToReifiedValue[A](r: HasReifiedValue[A]): ReifiedValue[A] = {
    r.reifiedValue
  }
  /**
   * Implicitly convert reified value to their original non-reified value.
   */
  implicit def hasReifiedValueToValue[A](r: HasReifiedValue[A]): A = r.reifiedValue.value
}

