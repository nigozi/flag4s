package flag4s.core.store

import cats.effect.IO
import cats.syntax.either._

import flag4s.core._
import io.circe.{Decoder, Encoder, Json}

case class StoredValue[A](value: A)

trait Store {
  def put[A: Encoder](key: String, value: A): IO[Either[Throwable, A]]

  def get[A: Decoder](key: String): IO[Either[Throwable, A]]

  def rawValue(key: String): IO[Either[Throwable, Json]]

  def keys(): IO[Either[Throwable, List[String]]]

  def remove(key: String): IO[Either[Throwable, Unit]]
}

object Store {

  def error(message: String): Throwable = new RuntimeException(message)

  def jsonFlag(key: String)(implicit store: Store): IO[Either[Throwable, JsonFlag]] =
    for {
      flag <- flag(key)
    } yield flag.map(_.toJsonFlag)

  def fatalJsonFlag(key: String)(implicit store: Store): JsonFlag =
    jsonFlag(key).unsafeRunSync().valueOr(e => sys.error(e.getMessage))
}