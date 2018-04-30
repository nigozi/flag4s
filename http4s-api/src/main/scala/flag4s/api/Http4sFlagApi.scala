package flag4s.api

import cats.effect._
import cats.instances.either._
import cats.syntax.applicative._
import cats.syntax.either._
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.io._

import flag4s.core._
import flag4s.core.store.{JsonFlag, Store}
import flag4s.core.store.Store._
import io.circe.{Encoder, _}
import io.circe.Encoder._
import io.circe.generic.auto._
import io.circe.syntax._

object Http4sFlagApi {
  def service(implicit store: Store): HttpService[IO] =
    HttpService[IO] {
      case POST -> Root / "flags" / key / "enable" => switchFlag(key, true).map {
        case Right(v) => Ok(v.asJson).unsafeRunSync()
        case Left(e) => BadRequest(errJson(e)).unsafeRunSync()
      }
      case POST -> Root / "flags" / key / "disable" => switchFlag(key, false).map {
        case Right(v) => Ok(v.asJson).unsafeRunSync()
        case Left(e) => BadRequest(errJson(e)).unsafeRunSync()
      }
      case req@PUT -> Root / "flags" =>
        for {
          fl <- req.as[JsonFlag]
          res <- switchFlag(fl.key, fl.value)
        } yield res match {
          case Right(v) => Ok(v).unsafeRunSync()
          case Left(e) => NotFound(errJson(e)).unsafeRunSync()
        }
      case GET -> Root / "flags" / key =>
        flag(key).map {
          case Right(v) => Ok(v.toJsonFlag.asJson).unsafeRunSync()
          case Left(e) => NotFound(errJson(e)).unsafeRunSync()
        }
      case GET -> Root / "flags" =>
        (for {
          keys <- store.keys().unsafeRunSync()
          flags <- keys.map(k => fatalFlag(k).toJsonFlag.asJson).asRight
        } yield flags.asJson) match {
          case Right(r) => Ok(r)
          case Left(e) => BadRequest(errJson(e))
        }
      case DELETE -> Root / "flags" / key =>
        store.remove(key).unsafeRunSync() match {
          case Right(_) => Ok()
          case Left(e) => BadRequest(errJson(e))
        }
    }

  def switchFlag[A: Encoder](key: String, value: A)(implicit store: Store): IO[Either[Throwable, A]] = {
    val valid = for {
      flag <- flag(key)
      valid <- flag.map(f => validateType(f, value).unsafeRunSync()).pure[IO]
    } yield valid.toOption.getOrElse(true)

    valid.flatMap {
      case true => store.put(key, value)
      case false => IO.pure(new RuntimeException("type mismatch!").asLeft)
    }
  }

  private def errJson(e: Throwable): Json = e.getMessage.asJson
}
