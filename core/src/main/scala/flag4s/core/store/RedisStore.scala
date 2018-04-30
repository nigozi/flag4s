package flag4s.core.store

import scala.concurrent.ExecutionContext

import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._
import com.redis.RedisClient

import flag4s.core.FlagValue
import flag4s.core.store.Store._
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax._

class RedisStore(
  host: String,
  port: Int)(implicit ec: ExecutionContext)
  extends Store {

  val client = new RedisClient(host, port)

  override def put[A: Encoder](key: String, value: A): IO[Either[Throwable, A]] =
    if (client.set(key, StoredValue(value).asJson))
      IO.pure(value.asRight)
    else
      IO.pure(error(s"failed to store flag $key").asLeft)

  override def remove(key: String): IO[Either[Throwable, Unit]] =
    Either.fromOption(client.del(key), error(s"failed to delete flag $key"))
      .map(_ => (): Unit)
      .pure[IO]

  override def keys(): IO[Either[Throwable, List[String]]] =
    IO.pure(Either.fromOption(client.keys().map(_.flatten), error("operation failed!")))

  override def rawValue(key: String): IO[Either[Throwable, FlagValue]] =
    IO.pure(Either.fromOption(client.get[String](key).map(v => parseFlagValue(v.asJson)), error("operation failed!")))
}

object RedisStore {
  def apply(host: String, port: Int)(implicit ec: ExecutionContext): RedisStore =
    new RedisStore(host, port)

  def trim(s: String): String = s.replace("\n", "").trim
}
