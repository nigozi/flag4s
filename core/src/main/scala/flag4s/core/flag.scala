package flag4s.core

import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._

import flag4s.core.store.{JsonFlag, Store}
import flag4s.core.store.Store._
import io.circe.{Encoder, Json}
import io.circe.syntax._

case class Flag(key: String, value: FlagValue) {
  def toJsonFlag: JsonFlag = JsonFlag(key, value.value.asJson)
}

case class FlagValue(value: Json)

trait FlagOps {

  def flag(key: String)(implicit store: Store): IO[Either[Throwable, Flag]] =
    for {
      rv <- store.rawValue(key)
    } yield rv.map(v => Flag(key, v))

  def fatalFlag(key: String)(implicit store: Store): Flag = flag(key).unsafeRunSync().valueOr(e => sys.error(e.getMessage))

  def newFlag[A: Encoder](key: String, value: A)(implicit store: Store): IO[Either[Throwable, Flag]] =
    for {
      exf <- flag(key)
      res <- if (exf.isRight) IO.pure(error(s"flag $key already exists").asLeft) else store.put(key, value)
    } yield res.map(f => Flag(key, FlagValue(f.asJson)))

  def enabled(flag: Flag)(implicit store: Store): IO[Either[Throwable, Boolean]] = is(flag, true)

  def ifEnabled[A](flag: Flag)(f: => A)(implicit store: Store): IO[Either[Throwable, A]] = ifIs(flag, true)(f)

  def is[A: Encoder](flag: Flag, value: A)(implicit store: Store): IO[Either[Throwable, Boolean]] = IO.pure((flag.value.value == value.asJson).asRight)

  def ifIs[A: Encoder, B](flag: Flag, value: A)(f: => B)(implicit store: Store): IO[Either[Throwable, B]] =
    is(flag, value).flatMap {
      case Right(true) => Right(f).pure[IO]
      case _ => IO.pure(new RuntimeException(s"${flag.value} is not equal to ${value.asJson}").asLeft)
    }

  def set[A: Encoder](flag: Flag, value: A)(implicit store: Store): IO[Either[Throwable, A]] =
    validateType(flag, value).map(v =>
      if (v) store.put(flag.key, value).unsafeRunSync() else new RuntimeException("type mismatch!").asLeft
    )

  def validateType[A: Encoder](flag: Flag, value: A): IO[Boolean] = IO.pure(flag.value.value.name == value.asJson.name)
}

object FlagOps extends FlagOps

