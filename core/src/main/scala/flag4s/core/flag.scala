package flag4s.core

import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._

import flag4s.core.store.Store
import flag4s.core.store.Store._
import flag4s.parser._
import io.circe.{Encoder, Json}
import io.circe.syntax._

case class JsonFlag(key: String, value: Json)

case class Flag(k: String, v: FlagValue) {
  def toJsonFlag: JsonFlag = JsonFlag(k, v.toJson)
}

sealed trait FlagValue {
  def toJson: Json
}

case class StringValue(s: String) extends FlagValue {
  override def toJson: Json = s.asJson
}

case class DoubleValue(d: Double) extends FlagValue {
  override def toJson: Json = d.asJson
}

case class BooleanValue(b: Boolean) extends FlagValue {
  override def toJson: Json = b.asJson
}

trait FlagOps {

  def flag[A](key: String)(implicit store: Store): IO[Either[Throwable, Flag]] =
    for {
      rv <- store.rawValue(key)
    } yield rv.flatMap(v => jsonToFlag(key, v))

  def fatalFlag[A](key: String)(implicit store: Store): Flag =
    flag(key).unsafeRunSync().valueOr(e => sys.error(e.getMessage))

  def newFlag[A: Encoder](key: String, value: A)(implicit store: Store): IO[Either[Throwable, Flag]] =
    for {
      exf <- store.rawValue(key)
      res <- if (exf.isRight) IO.pure(error(s"flag $key already exists").asLeft) else store.put(key, value)
    } yield res.flatMap(_ => jsonToFlagValue(value.asJson).map(Flag(key, _)))

  def enabled[A](flag: Flag)(implicit store: Store): IO[Either[Throwable, Boolean]] = is(flag, true)

  def ifEnabled[A, B](flag: Flag)(f: => B)(implicit store: Store): IO[Either[Throwable, B]] = ifIs(flag, true)(f)

  def is[A: Encoder](flag: Flag, value: A)(implicit store: Store): IO[Either[Throwable, Boolean]] = {
    IO.pure((flag.v.toJson == value.asJson).asRight)
  }

  def ifIs[A: Encoder, B](flag: Flag, value: A)(f: => B)(implicit store: Store): IO[Either[Throwable, B]] =
    is(flag, value).flatMap {
      case Right(true) => Right(f).pure[IO]
      case _ => IO.pure(new RuntimeException(s"${flag.v} is not equal to $value").asLeft)
    }

  def set[A: Encoder](flag: Flag, value: A)(implicit store: Store): IO[Either[Throwable, A]] = store.put(flag.k, value)

  def validateType[A: Encoder](flag: Flag, value: A): Boolean = flag.toJsonFlag.value.name == value.asJson.name
}

object FlagOps extends FlagOps

