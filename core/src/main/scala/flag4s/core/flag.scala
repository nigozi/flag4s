package flag4s.core

import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._

import flag4s.core.store.Store
import flag4s.core.store.Store._
import io.circe.{Encoder, Json}

case class Flag(k: String, v: FlagValue)

sealed trait FlagValue

case class StringValue(s: String) extends FlagValue

case class DoubleValue(d: Double) extends FlagValue

case class BooleanValue(b: Boolean) extends FlagValue

case class JsonValue(js: Json) extends FlagValue

case class JsonFlag(key: String, value: Json)

object Flag {
  def flag[A](key: String)(implicit store: Store): IO[Either[Throwable, Flag]] =
    for {
      rv <- store.rawValue(key)
    } yield rv.flatMap(v => decodeFlag(key, v))

  def fatalFlag[A](key: String)(implicit store: Store): Flag =
    flag(key).unsafeRunSync().valueOr(sys.error(s"flag $key not found"))

  def enabled[A](flag: Flag)(implicit store: Store): IO[Either[Throwable, Boolean]] = is(flag, true)

  def ifEnabled[A, B](flag: Flag)(f: => B)(implicit store: Store): IO[Either[Throwable, B]] = ifIs(flag, true)(f)

  def ifIs[A, B](flag: Flag, value: A)(f: => B)(implicit store: Store): IO[Either[Throwable, B]] =
    is(flag, value).flatMap {
      case Right(true) => Right(f).pure[IO]
      case _ => Left(new RuntimeException(s"${flag.v} is not equal to $value")).pure[IO]
    }

  def is[A](flag: Flag, value: A)(implicit store: Store): IO[Either[Throwable, Boolean]] =
    flag.v match {
      case StringValue(v) => Right(v == value.toString).pure[IO]
      case BooleanValue(v) => Right(v == value).pure[IO]
      case DoubleValue(v) => Right(v == value).pure[IO]
      case _ => Left(error("unknown flag type")).pure[IO]
    }

  def set[A: Encoder](flag: Flag, value: A)(implicit store: Store): IO[Either[Throwable, Unit]] =
    store.put(flag.k, value).map(_ => Right((): Unit))

  private def decodeFlag(key: String, value: Json): Either[Throwable, Flag] = {
    decodeVal(value).map(v => Flag(key, v))
  }

  private def decodeVal(body: Json): Either[Throwable, FlagValue] = {
    def readVal = Either.fromOption(body.hcursor.downField("value").focus, new RuntimeException("failed to parse the value"))

    readVal.flatMap { v =>
      v.name match {
        case "Boolean" => Either.fromOption(v.asBoolean.map(BooleanValue), new RuntimeException("couldn't parse the flag value as boolean"))
        case "String" => Either.fromOption(v.asString.map(StringValue), new RuntimeException("couldn't parse the flag value as string"))
        case "Number" => Either.fromOption(v.asNumber.map(_.toDouble).map(DoubleValue), new RuntimeException("couldn't parse the flag value as double"))
        case _ => Left(new RuntimeException("unknown type"))
      }
    }
  }
}

