package flag4s.core

import java.util.UUID

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.applicative._
import flag4s.core.store.{Store, StoredValue}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Encoder, Json}
import org.http4s.{EntityDecoder, Response, Status}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

trait FeatureSpec
  extends Matchers
    with MockFactory
    with BeforeAndAfterEach
    with BeforeAndAfterAll {
  def randomKey: String = UUID.randomUUID().toString.take(10)

  def statusCheck(actual: IO[Response[IO]], expected: Status): Boolean = actual.unsafeRunSync().status == expected

  def bodyCheck[A](actual: IO[Response[IO]], expected: A)(implicit ev: EntityDecoder[IO, A]): Boolean = {
    val act = actual.unsafeRunSync().as[A].unsafeRunSync()
    act == expected
  }
}

class InMemoryStore extends Store {
  val map: collection.mutable.HashMap[String, String] = collection.mutable.HashMap[String, String]()

  override def put[T: Encoder](key: String, value: T): IO[Either[Throwable, T]] = {
    map.put(key, StoredValue(value.asJson).asJson.toString())
    Right(value).pure[IO]
  }

  def keys(): IO[Either[Throwable, List[String]]] =
    Right(map.keySet.toList).pure[IO]

  def remove(key: String): IO[Either[Throwable, Unit]] =
    map.remove(key).map(_ => Right(()))
      .getOrElse(Left(error(s"failed to delete flag $key")))
      .pure[IO]

  override def rawValue(key: String): IO[Either[Throwable, Json]] = {
    def get: Option[String] = map.get(key)

    get match {
      case Some(v) => decode[StoredValue](v).map(_.value).pure[IO]
      case _ => Left(error(s"failed to parse value of $key")).pure[IO]
    }
  }
}