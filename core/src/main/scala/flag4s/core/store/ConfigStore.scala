package flag4s.core.store

import java.io.File

import scala.concurrent.ExecutionContext

import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._
import pureconfig.loadConfig
import pureconfig.error.ConfigReaderFailures

import flag4s.core.store.Store._
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

case class Config(features: Map[String, String])

class ConfigStore(path: String)(implicit ec: ExecutionContext) extends Store {
  val config: Either[ConfigReaderFailures, Config] = loadConfig[Config](new File(path).toPath)

  override def put[T: Encoder](key: String, value: T): IO[Either[Throwable, T]] =
    Left(error("altering the value is not supported in config store")).pure[IO]

  override def get[T: Decoder](key: String): IO[Either[Throwable, T]] =
    config
      .flatMap(c => Either.fromOption(c.features.get(key), s"key $key not found"))
      .leftMap(_ => error(s"key $key not found"))
      .map(_.asInstanceOf[T])
      .pure[IO]

  override def keys(): IO[Either[Throwable, List[String]]] =
    config
      .map(_.features.keySet.toList)
      .leftMap(e => error(e.head.description))
      .pure[IO]

  override def remove(key: String): IO[Either[Throwable, Unit]] =
    Left(error("removing a key is not supported in config store")).pure[IO]

  override def rawValue(key: String): IO[Either[Throwable, Json]] =
    config
      .flatMap(c => Either.fromOption(c.features.get(key).map(_.asJson), ""))
      .leftMap(e => new RuntimeException(e.toString)).pure[IO]
}

object ConfigStore {
  def apply(configFile: String)(implicit ec: ExecutionContext): ConfigStore = new ConfigStore(configFile)
}
