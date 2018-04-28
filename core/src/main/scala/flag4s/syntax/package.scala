package flag4s

import cats.effect.IO

import flag4s.core.Flag
import flag4s.core.store.Store
import io.circe.{Decoder, Encoder}

package object syntax {
  implicit class Ops(flag: Flag) {
    def enabled(implicit store: Store): IO[Either[Throwable, Boolean]] = Flag.enabled(flag)

    def ifEnabled[A](f: => A)(implicit store: Store): IO[Either[Throwable, A]] = Flag.ifEnabled(flag)(f)

    def ifIs[A, B](value: A)(f: => B)(implicit store: Store, d: Decoder[A]): IO[Either[Throwable, B]] = Flag.ifIs(flag, value)(f)

    def is[A](value: A)(implicit store: Store): IO[Either[Throwable, Boolean]] = Flag.is(flag, value)

    def set[A: Encoder](value: A)(implicit store: Store): IO[Either[Throwable, Unit]] = Flag.set(flag, value)
  }
}
