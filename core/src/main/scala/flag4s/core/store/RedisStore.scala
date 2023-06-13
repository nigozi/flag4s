package flag4s.core.store

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.applicative._
import cats.syntax.either._
import com.redis.RedisClient

import flag4s.core._
import io.circe.{Encoder, Json}
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._

class RedisStore(
                  host: String,
                  port: Int,
                  database: Int = 0,
                  secret: Option[Any] = None)
  extends Store {

  def redisClient = new RedisClient(host, port, database, secret)

  private lazy val client = redisClient

  override def put[A: Encoder](key: String, value: A): IO[Either[Throwable, A]] =
    if (client.set(key, StoredValue(value.asJson).asJson))
      IO.pure(value.asRight)
    else
      IO.pure(error(s"failed to store flag $key").asLeft)

  override def remove(key: String): IO[Either[Throwable, Unit]] =
    Either.fromOption(client.del(key), error(s"failed to delete flag $key"))
      .map(_ => (): Unit)
      .pure[IO]

  override def keys(): IO[Either[Throwable, List[String]]] =
    IO.pure(Either.fromOption(client.keys().map(_.flatten), error("operation failed!")))

  override def rawValue(key: String): IO[Either[Throwable, Json]] = {
    def get: Option[String] = client.get[String](key)

    get match {
      case Some(v) => decode[StoredValue](v).map(_.value).pure[IO]
      case _ => Left(error(s"failed to parse value of $key")).pure[IO]
    }
  }
}

object RedisStore {
  def apply(host: String, port: Int): RedisStore =
    new RedisStore(host, port)

  def trim(s: String): String = s.replace("\n", "").trim
}
