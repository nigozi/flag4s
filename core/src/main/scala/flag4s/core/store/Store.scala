package flag4s.core.store

import cats.effect.IO
import cats.syntax.applicative._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import flag4s.core._
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.auto._
import io.circe.parser.decode

case class StoredValue[A](value: A)

case class JsonFlag(key: String, value: Json)

trait Store {
  def put[A: Encoder](key: String, value: A): IO[Either[Throwable, A]]

  def get[A: Decoder](key: String): IO[Either[Throwable, A]] = {
    def convertTo(v: FlagValue): Either[Throwable, A] = decode[A](v.value.toString())

    for {
      rv <- rawValue(key)
      pv <- rv.flatMap(convertTo).pure[IO]
    } yield pv
  }

  def rawValue(key: String): IO[Either[Throwable, FlagValue]]

  def keys(): IO[Either[Throwable, List[String]]]

  def remove(key: String): IO[Either[Throwable, Unit]]
}

object Store {
  def error(message: String): Throwable = new RuntimeException(message)

  implicit val flagEntityDecoder: EntityDecoder[IO, JsonFlag] = jsonOf[IO, JsonFlag]

  def parseFlagValue(v: Json): FlagValue = {
    v.hcursor.downField("value").focus.map(FlagValue).getOrElse(FlagValue(Json.Null))
  }
}