package flag4s

import cats.effect.IO
import cats.syntax.either._

import io.circe.Encoder
import io.circe.syntax._

package object core extends FlagOps {
  def checkType[A: Encoder](flag: Flag, value: A): IO[Either[Throwable, Unit]] = {
    val org = flag.value.name
    val des = value.asJson.name
    IO.pure(if (org == des) Right((): Unit) else error(s"type mismatch! can't replace $org with $des").asLeft)
  }

  def error(message: String): Throwable = new RuntimeException(message)
}
