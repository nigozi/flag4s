package flag4s.core.store

import java.io.File

import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._
import flag4s.core._
import io.circe.syntax._
import io.circe.{Encoder, Json}
import pureconfig.generic.semiauto._
import pureconfig.{ConfigReader, ConfigSource}


case class Config(features: Map[String, String])

object Config {
  implicit val configReader: ConfigReader[Config] = deriveReader
}

class ConfigStore(path: String) extends Store {
  val config: Either[Throwable, Config] =
    ConfigSource.file(new File(path).toPath).load[Config].leftMap(e => error(e.head.description))

  override def put[A: Encoder](key: String, value: A): IO[Either[Throwable, A]] =
    IO.pure(error("value modification is not supported in config store").asLeft)

  override def keys(): IO[Either[Throwable, List[String]]] =
    config
      .map(_.features.keySet.toList)
      .pure[IO]

  override def remove(key: String): IO[Either[Throwable, Unit]] =
    Left(error("removing a flag is not supported in config store")).pure[IO]

  override def rawValue(key: String): IO[Either[Throwable, Json]] =
    config
      .flatMap(c => Either.fromOption(c.features.get(key).map(v => v.asJson), error(s"key $key not found")))
      .pure[IO]
}

object ConfigStore {
  def apply(configFile: String): ConfigStore = new ConfigStore(configFile)
}
