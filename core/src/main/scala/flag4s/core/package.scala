package flag4s

import cats.effect.IO

import flag4s.core.store.Store
import io.circe.{Encoder, Json}
import io.circe.syntax._

package object core {
  def flag[A](key: String)(implicit store: Store): IO[Either[Throwable, Flag]] = Flag.flag(key)

  def fatalFlag[A](key: String)(implicit store: Store): Flag = Flag.fatalFlag(key)

  def enabled[A](flag: Flag)(implicit store: Store): IO[Either[Throwable, Boolean]] = Flag.enabled(flag)

  def ifEnabled[A, B](flag: Flag)(f: => B)(implicit store: Store): IO[Either[Throwable, B]] = Flag.ifEnabled(flag)(f)

  def ifIs[A, B](flag: Flag, value: A)(f: => B)(implicit store: Store): IO[Either[Throwable, B]] = Flag.ifIs(flag, value)(f)

  def is[A](flag: Flag, value: A)(implicit store: Store): IO[Either[Throwable, Boolean]] = Flag.is(flag, value)

  def set[A: Encoder](flag: Flag, value: A)(implicit store: Store): IO[Either[Throwable, Unit]] = Flag.set(flag, value)

  def toJsonFag(f: Flag): JsonFlag = f.v match {
    case StringValue(s) => JsonFlag(f.k, s.asJson)
    case DoubleValue(d) => JsonFlag(f.k, d.asJson)
    case BooleanValue(b) => JsonFlag(f.k, b.asJson)
    case _ => JsonFlag(f.k, Json.Null)
  }
}
