package flag4s.core

import cats.effect.IO
import cats.syntax.either._
import cats.syntax.applicative._

import flag4s.core.store.Store
import flag4s.syntax._
import io.circe.{Decoder, Encoder, Json}
import io.circe.parser._
import io.circe.syntax._

case class Flag(key: String, value: Json)

trait FlagOps {

  /**
    * finds the flag
    *
    * @param key   flag's key
    * @param store key/val store
    * @return Flag if found, Throws Exception otherwise
    */
  def fatalFlag(key: String)(implicit store: Store): IO[Flag] = flag(key).map(_.valueOr(e => sys.error(e.getMessage)))

  /**
    * executes the function f if the flag is set to the given value
    *
    * @param key   flag's key
    * @param value expected value
    * @param f     function to execute if flag is on
    * @param store key/val store
    * @tparam A type of flag
    * @tparam B the return type of f
    * @return Right[B] if flag is on, Left[Throwable] otherwise.
    */
  def withFlag[A: Encoder, B](key: String, value: A)(f: => B)(implicit store: Store): IO[Either[Throwable, B]] =
    for {
      flag <- flag(key)
    } yield flag match {
      case Right(fl) if fl.is(value).unsafeRunSync() => f.asRight
      case _ => error(s"flag $key is off").asLeft
    }

  /**
    * finds the flag
    *
    * @param key   flag's key
    * @param store key/val store
    * @return Right[Flag] if found, Left[Throwable] otherwise
    */
  def flag(key: String)(implicit store: Store): IO[Either[Throwable, Flag]] =
    for {
      rv <- store.rawValue(key)
    } yield rv.map(v => Flag(key, v))

  /**
    * switches the flag's value to the given value
    *
    * @param key   flag's key
    * @param value new value
    * @tparam A type of flag
    * @return Right[Flag] if successful, Left[Throwable] otherwise
    */
  def switchFlag[A: Encoder](key: String, value: A)(implicit store: Store): IO[Either[Throwable, A]] = {
    val valid = flag(key).flatMap {
      case Right(f) => checkType(f, value)
      case _ => Right((): Unit).pure[IO]
    }

    valid.flatMap(_ => store.put(key, value))
  }

  /**
    * sets the flag's value to the given value
    *
    * @param flag  flag
    * @param value value to set
    * @param store key/val store
    * @tparam A type of flag
    * @return Right[A] if successful, Left[Throwable] otherwise
    */
  def set[A: Encoder](flag: Flag, value: A)(implicit store: Store): IO[Either[Throwable, A]] =
    for {
      _ <- checkType(flag, value)
      res <- store.put(flag.key, value)
    } yield res

  /**
    * creates a new flag
    *
    * @param key   flag's key
    * @param value flag's value
    * @param store key/val store
    * @tparam A type of flag
    * @return Right[Flag] if created successfully, Left[Throwable] otherwise
    */
  def newFlag[A: Encoder](key: String, value: A)(implicit store: Store): IO[Either[Throwable, Flag]] =
    for {
      exf <- flag(key)
      res <- if (exf.isRight) IO.pure(error(s"flag $key already exists").asLeft) else store.put(key, value)
    } yield res.map(f => Flag(key, f.asJson))

  /**
    * checks if the flag's value is true
    *
    * @param flag  flag
    * @param store key/val store
    * @return true if flag is on false otherwise
    */
  def enabled(flag: Flag)(implicit store: Store): IO[Boolean] = is(flag, true)

  /**
    * executes the function f if the flag's value is true
    *
    * @param flag  flag
    * @param f     function to execute if flag is on
    * @param store key/val store
    * @tparam A the return type of f
    * @return Right[A] if flag is on, Left[Throwable] otherwise.
    */
  def ifEnabled[A](flag: Flag)(f: => A)(implicit store: Store): IO[Either[Throwable, A]] = ifIs(flag, true)(f)

  /**
    * executes the function f if the flag's value is equal to the given value
    *
    * @param flag  flag
    * @param value expected value
    * @param f     function to execute if flag is on
    * @param store key/val store
    * @tparam A type of flag
    * @tparam B the return type of f
    * @return Right[B] if flag is on, Left[Throwable] otherwise.
    */
  def ifIs[A: Encoder, B](flag: Flag, value: A)(f: => B)(implicit store: Store): IO[Either[Throwable, B]] =
    is(flag, value).flatMap {
      case true => IO.pure(f.asRight)
      case false => IO.pure(error(s"${flag.value} is not equal to ${value.asJson}").asLeft)
    }

  /**
    * checks if the flag's value is equal to the given value
    *
    * @param flag  flag
    * @param value expected value
    * @param store key/val store
    * @tparam A type of flag
    * @return true if flag is enabled false otherwise
    */
  def is[A: Encoder](flag: Flag, value: A)(implicit store: Store): IO[Boolean] = IO.pure(flag.value == value.asJson)

  /**
    * returns the flag's value as the given type
    *
    * @param flag  flag
    * @param store key/val store
    * @tparam A desired type
    * @return Right[A] if successful, Left[Throwable] otherwise
    */
  def get[A: Decoder](flag: Flag)(implicit store: Store): IO[Either[Throwable, A]] = IO.pure(decode[A](flag.value.toString()))
}

object FlagOps extends FlagOps

