package flag4s.core.store

import scala.concurrent.ExecutionContext

import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._
import com.redis.RedisClient

import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import flag4s.core.store.Store._

class RedisStore(
  host: String,
  port: Int)(implicit ec: ExecutionContext)
  extends Store {

  import RedisStore._

  val client = new RedisClient(host, port)

  override def put[T: Encoder](key: String, value: T): IO[Either[Throwable, T]] =
    if (client.set(key, StoredValue(value).asJson))
      Right(value).pure[IO]
    else
      Left(error(s"failed to store flag $key")).pure[IO]

  override def get[T: Decoder](key: String): IO[Either[Throwable, T]] =
    client.get[String](key)
      .map(a => decode[StoredValue[T]](trim(a)).map(_.value))
      .getOrElse(Left(error(s"flag $key not found")))
      .pure[IO]

  override def remove(key: String): IO[Either[Throwable, Unit]] =
    Either.fromOption(client.del(key), error(s"failed to delete flag $key"))
      .map(_ => (): Unit)
      .pure[IO]

  override def keys(): IO[Either[Throwable, List[String]]] =
    Either.fromOption(client.keys().map(_.flatten), error("operation failed!")).pure[IO]
}

object RedisStore {
  def apply(host: String, port: Int)(implicit ec: ExecutionContext): RedisStore =
    new RedisStore(host, port)

  def trim(s: String): String = s.replace("\n", "").trim
}
