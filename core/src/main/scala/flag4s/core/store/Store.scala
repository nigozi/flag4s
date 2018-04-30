package flag4s.core.store

import cats.effect.IO
import cats.syntax.applicative._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import flag4s.core._
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.auto._
import io.circe.parser.decode

case class StoredFlag(key: String, value: StoredValue)
case class StoredValue(value: Json)

trait Store {
  def put[A: Encoder](key: String, value: A): IO[Either[Throwable, A]]

  def get[A: Decoder](key: String): IO[Either[Throwable, A]] = {
    def fromJson(v: Json): Either[Throwable, A] = decode[A](v.toString())

    for {
      rv <- rawValue(key)
      pv <- rv.flatMap(fromJson).pure[IO]
    } yield pv
  }

  def rawValue(key: String): IO[Either[Throwable, Json]]

  def keys(): IO[Either[Throwable, List[String]]]

  def remove(key: String): IO[Either[Throwable, Unit]]
}

object Store {
  implicit val flagEntityDecoder: EntityDecoder[IO, Flag] = jsonOf[IO, Flag]
}