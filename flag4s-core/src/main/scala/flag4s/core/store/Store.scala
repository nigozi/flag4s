package flag4s.core.store

import cats.effect.IO

import io.circe.{Decoder, Encoder}

case class StoredValue[T](value: T)

trait Store {
  def put[T: Encoder](key: String, value: T): IO[Either[Throwable, T]]

  def get[T: Decoder](key: String): IO[Either[Throwable, T]]

  def keys(): IO[Either[Throwable, List[String]]]

  def remove(key: String): IO[Either[Throwable, Unit]]
}

object Store {
  def error(message: String): Throwable = new RuntimeException(message)
}