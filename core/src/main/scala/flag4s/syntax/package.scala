package flag4s

import cats.effect.IO

import flag4s.core._
import flag4s.core.store.Store
import io.circe.{Decoder, Encoder}

package object syntax {

  implicit class Ops(flag: Flag) {
    def enabled(implicit store: Store): IO[Either[Throwable, Boolean]] = FlagOps.enabled(flag)

    def ifEnabled[A](f: => A)(implicit store: Store): IO[Either[Throwable, A]] = FlagOps.ifEnabled(flag)(f)

    def ifIs[A: Encoder, B](value: A)(f: => B)(implicit store: Store, d: Decoder[A]): IO[Either[Throwable, B]] = FlagOps.ifIs(flag, value)(f)

    def is[A: Encoder](value: A)(implicit store: Store): IO[Either[Throwable, Boolean]] = FlagOps.is(flag, value)

    def get[A: Decoder](implicit store: Store): IO[Either[Throwable, A]] = FlagOps.get(flag)

    def set[A: Encoder](value: A)(implicit store: Store): IO[Either[Throwable, A]] = FlagOps.set(flag, value)
  }

}
