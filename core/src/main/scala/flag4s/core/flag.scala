package flag4s.core

import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._

import flag4s.core.store.Store
import flag4s.core.store.Store._
import io.circe.{Encoder, Json}
import io.circe.syntax._

case class JsonFlag(key: String, value: Json)

case class Flag(k: String, v: FlagValue)

sealed trait FlagValue

case class StringValue(s: String) extends FlagValue

case class DoubleValue(d: Double) extends FlagValue

case class BooleanValue(b: Boolean) extends FlagValue

trait FlagOps {
  import FlagOps._

  def flag[A](key: String)(implicit store: Store): IO[Either[Throwable, Flag]] =
    for {
      rv <- store.rawValue(key)
    } yield rv.flatMap(v => decodeFlag(key, v))

  def fatalFlag[A](key: String)(implicit store: Store): Flag =
    flag(key).unsafeRunSync().valueOr(sys.error(s"flag $key not found"))

  def newFlag[A: Encoder](key: String, value: A)(implicit store: Store): IO[Either[Throwable, Flag]] =
    for {
      exf <- store.rawValue(key)
      res <- if (exf.isRight) Left(error(s"flag $key already exists")).pure[IO] else store.put(key, value)
    } yield res.flatMap(_ => decodeVal(value.asJson).map(Flag(key, _)))

  def enabled[A](flag: Flag)(implicit store: Store): IO[Either[Throwable, Boolean]] = is(flag, true)

  def ifEnabled[A, B](flag: Flag)(f: => B)(implicit store: Store): IO[Either[Throwable, B]] = ifIs(flag, true)(f)

  def is[A](flag: Flag, value: A)(implicit store: Store): IO[Either[Throwable, Boolean]] = {
    def eq[T](v1: T, v2: T): IO[Either[Throwable, Boolean]] = Right(v1 == v2).pure[IO]

    flag.v match {
      case StringValue(v) => eq(v, value)
      case BooleanValue(v) => eq(v, value)
      case DoubleValue(v) => eq(v, value)
      case _ => Left(error("unknown flag type")).pure[IO]
    }
  }

  def ifIs[A, B](flag: Flag, value: A)(f: => B)(implicit store: Store): IO[Either[Throwable, B]] =
    is(flag, value).flatMap {
      case Right(true) => Right(f).pure[IO]
      case _ => Left(new RuntimeException(s"${flag.v} is not equal to $value")).pure[IO]
    }

  def set[A: Encoder](flag: Flag, value: A)(implicit store: Store): IO[Either[Throwable, A]] = store.put(flag.k, value)

  def toJsonFag(f: Flag): JsonFlag = f.v match {
    case StringValue(s) => JsonFlag(f.k, s.asJson)
    case DoubleValue(d) => JsonFlag(f.k, d.asJson)
    case BooleanValue(b) => JsonFlag(f.k, b.asJson)
    case _ => JsonFlag(f.k, Json.Null)
  }
}

object FlagOps extends FlagOps {
  private def decodeFlag(key: String, value: Json): Either[Throwable, Flag] =
    for {
      v <- readVal(value)
      dv <- decodeVal(v).map(v => Flag(key, v))
    } yield dv

  private def readVal(js: Json): Either[Throwable, Json] = Either.fromOption(js.hcursor.downField("value").focus, new RuntimeException("failed to parse the value"))

  private def decodeVal(v: Json): Either[Throwable, FlagValue] = {
    def convert[A](js: Json)(f: Json => Option[A]) = Either.fromOption(f.apply(js), new RuntimeException("couldn't parse the flag value"))

    v.name match {
      case "Boolean" => convert(v)(js => js.asBoolean.map(BooleanValue))
      case "String" => convert(v)(js => js.asString.map(StringValue))
      case "Number" => convert(v)(js => js.asNumber.map(_.toDouble).map(DoubleValue))
      case _ => Left(new RuntimeException("unknown type"))
    }
  }
}

