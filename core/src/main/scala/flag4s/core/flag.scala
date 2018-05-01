package flag4s.core

import cats.effect.IO
import cats.syntax.either._

import flag4s.core.store.Store
import flag4s.syntax._
import io.circe.{Decoder, Encoder, Json}
import io.circe.parser._
import io.circe.syntax._

case class Flag(key: String, value: Json)

trait FlagOps {

  def flag(key: String)(implicit store: Store): IO[Either[Throwable, Flag]] =
    for {
      rv <- store.rawValue(key)
    } yield rv.map(v => Flag(key, v))

  def fatalFlag(key: String)(implicit store: Store): Flag = flag(key).unsafeRunSync().valueOr(e => sys.error(e.getMessage))

  def withFlag[A: Encoder, B](key: String, value: A = false)(f: => B)(implicit store: Store): IO[Either[Throwable, B]] =
    for {
      flag <- flag(key)
    } yield flag match {
      case Right(fl) if fl.enabled.unsafeRunSync() => f.asRight
      case _ => error(s"flag $key is off").asLeft
    }

  def newFlag[A: Encoder](key: String, value: A)(implicit store: Store): IO[Either[Throwable, Flag]] =
    for {
      exf <- flag(key)
      res <- if (exf.isRight) IO.pure(error(s"flag $key already exists").asLeft) else store.put(key, value)
    } yield res.map(f => Flag(key, f.asJson))

  def enabled(flag: Flag)(implicit store: Store): IO[Boolean] = is(flag, true)

  def ifEnabled[A](flag: Flag)(f: => A)(implicit store: Store): IO[Either[Throwable, A]] = ifIs(flag, true)(f)

  def is[A: Encoder](flag: Flag, value: A)(implicit store: Store): IO[Boolean] = IO.pure(flag.value == value.asJson)

  def ifIs[A: Encoder, B](flag: Flag, value: A)(f: => B)(implicit store: Store): IO[Either[Throwable, B]] =
    is(flag, value).flatMap {
      case true => IO.pure(f.asRight)
      case false => IO.pure(error(s"${flag.value} is not equal to ${value.asJson}").asLeft)
    }

  def get[A: Decoder](flag: Flag)(implicit store: Store): IO[Either[Throwable, A]] = IO.pure(decode[A](flag.value.toString()))

  def set[A: Encoder](flag: Flag, value: A)(implicit store: Store): IO[Either[Throwable, A]] =
    for {
      _ <- checkType(flag, value)
      res <- store.put(flag.key, value)
    } yield res
}

object FlagOps extends FlagOps

