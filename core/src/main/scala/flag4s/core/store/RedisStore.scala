package flag4s.core.store

import scala.concurrent.ExecutionContext

import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._
import com.redis.RedisClient

import io.circe.{Decoder, Encoder, Json}
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.generic.auto._
import flag4s.core.store.Store._

class RedisStore(
  host: String,
  port: Int)(implicit ec: ExecutionContext)
  extends Store {

  import RedisStore._

  val client = new RedisClient(host, port)

  override def put[A: Encoder](key: String, value: A): IO[Either[Throwable, A]] =
    if (client.set(key, StoredValue(value).asJson))
      IO.pure(value.asRight)
    else
      IO.pure(error(s"failed to store flag $key").asLeft)

  override def get[A: Decoder](key: String): IO[Either[Throwable, A]] =
    client.get[String](key)
      .map(a => decode[StoredValue[A]](trim(a)).map(_.value))
      .getOrElse(error(s"flag $key not found").asLeft)
      .pure[IO]

  override def remove(key: String): IO[Either[Throwable, Unit]] =
    Either.fromOption(client.del(key), error(s"failed to delete flag $key"))
      .map(_ => (): Unit)
      .pure[IO]

  override def keys(): IO[Either[Throwable, List[String]]] =
    IO.pure(Either.fromOption(client.keys().map(_.flatten), error("operation failed!")))

  override def rawValue(key: String): IO[Either[Throwable, Json]] =
    IO.pure(Either.fromOption(client.get[String](key).map(_.asJson), error("operation failed!")))
}

object RedisStore {
  def apply(host: String, port: Int)(implicit ec: ExecutionContext): RedisStore =
    new RedisStore(host, port)

  def trim(s: String): String = s.replace("\n", "").trim
}
