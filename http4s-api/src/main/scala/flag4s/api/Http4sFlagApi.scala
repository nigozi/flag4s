package flag4s.api

import cats.effect._
import cats.instances.either._
import cats.syntax.either._
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.io._

import flag4s.core.{flag, _}
import flag4s.core.store.Store
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
          fl <- req.as[Flag]
          res <- switchFlag(fl.key, fl.value)
        } yield res match {
          case Right(v) => Ok(v).unsafeRunSync()
          case Left(e) => NotFound(errJson(e)).unsafeRunSync()
        }
      case GET -> Root / "flags" / key =>
        flag(key).map {
          case Right(v) => Ok(v.asJson).unsafeRunSync()
          case Left(e) => NotFound(errJson(e)).unsafeRunSync()
        }
      case GET -> Root / "flags" =>
        (for {
          keys <- store.keys().unsafeRunSync()
          flags <- keys.map(k => fatalFlag(k).asJson).asRight
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
    val valid = flag(key).map {
      case Right(f) => checkType(f, value).unsafeRunSync()
      case _ => Right((): Unit)
    }

    IO.pure {
      for {
        _ <- valid.unsafeRunSync()
        res <- store.put(key, value).unsafeRunSync()
      } yield res
    }
  }

  private def errJson(e: Throwable): Json = e.getMessage.asJson
}
