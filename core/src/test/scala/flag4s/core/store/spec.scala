package flag4s.core.store

import java.util.UUID

import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers}

import flag4s.core.StringValue
import flag4s.core.store.Store.error
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._

trait FeatureSpec
  extends Matchers
    with MockFactory
    with BeforeAndAfterEach
    with BeforeAndAfterAll {
  def randomKey: String = UUID.randomUUID().toString.take(10)
}

class InMemoryStore extends Store {
  val map: collection.mutable.HashMap[String, String] = collection.mutable.HashMap[String, String]()

  override def put[T: Encoder](key: String, value: T): IO[Either[Throwable, T]] = {
    map.put(key, StoredValue(value).asJson.toString())
    Right(value).pure[IO]
  }

  def get[T: Decoder](key: String): IO[Either[Throwable, T]] =
    Either.fromOption(map.get(key), error(s"flag $key not found"))
      .flatMap(a => decode[StoredValue[T]](a).leftMap(_.getCause))
      .map(_.value)
      .pure[IO]

  def keys(): IO[Either[Throwable, List[String]]] =
    Right(map.keySet.toList).pure[IO]

  def remove(key: String): IO[Either[Throwable, Unit]] =
    map.remove(key).map(_ => Right())
      .getOrElse(Left(error(s"failed to delete flag $key")))
      .pure[IO]

  override def rawValue(key: String): IO[Either[Throwable, Json]] =
    Either.fromOption(map.get(key).map(v => StringValue(v).asJson), error(s"flag $key not found")).pure[IO]
}

