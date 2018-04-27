package flag4s.core

import cats.effect.IO
import cats.syntax.applicative._

import flag4s.core.store.Store
import io.circe.{Decoder, Encoder}

case class Flag(key: String) {
  def enabled(implicit store: Store): IO[Boolean] = is(true)

  def ifEnabled[A: Decoder](f: => A)(implicit store: Store): IO[Option[A]] = ifIs(true)(f)

  def ifIs[A: Decoder, B](value: A)(f: => B)(implicit store: Store): IO[Option[B]] =
    is(value).flatMap {
      case true => Some(f).pure[IO]
      case false => None.pure[IO]
    }

  def is[T: Decoder](value: T)(implicit store: Store): IO[Boolean] = store.get(key).map(_ == value)

  def set[T: Encoder](value: T)(implicit store: Store): IO[Unit] = store.put(key, value).map(a => a == value)
}
