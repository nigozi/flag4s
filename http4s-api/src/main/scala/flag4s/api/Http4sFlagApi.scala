package flag4s.api

import cats.effect._
import cats.instances.either._
import cats.instances.list._
import cats.syntax.either._
import cats.syntax.traverse._
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.io._

import flag4s.core.{flag, _}
import flag4s.core.store.Store
import flag4s.core.store.Store._
import io.circe._
import io.circe.Encoder._
import io.circe.generic.auto._
import io.circe.syntax._

object Http4sFlagApi {
  def service(`basePath`: String = "flags")(implicit store: Store): HttpService[IO] =
    HttpService[IO] {
      case POST -> Root / `basePath` / key / "enable" => switchFlag(key, true).flatMap {
        case Right(v) => Ok(v.asJson)
        case Left(e) => BadRequest(errJson(e))
      }
      case POST -> Root / `basePath` / key / "disable" => switchFlag(key, false).flatMap {
        case Right(v) => Ok(v.asJson)
        case Left(e) => BadRequest(errJson(e))
      }
      case req@PUT -> Root / `basePath` =>
        for {
          f <- req.as[Flag]
          s <- switchFlag(f.key, f.value)
          r <- s.map(r => Ok(r.asJson)).valueOr(e => NotFound(errJson(e)))
        } yield r
      case GET -> Root / `basePath` / key =>
        flag(key).flatMap {
          case Right(v) => Ok(v.asJson)
          case Left(e) => NotFound(errJson(e))
        }
      case GET -> Root / `basePath` =>
        for {
          keys <- store.keys()
          flags <- mapKeys(keys.valueOr(_ => List.empty)).flatMap(f => Ok(f.asJson))
        } yield flags
      case DELETE -> Root / `basePath` / key =>
        store.remove(key).flatMap {
          case Right(_) => Ok()
          case Left(e) => BadRequest(errJson(e))
        }
    }

  private def errJson(e: Throwable): Json = e.getMessage.asJson

  private def mapKeys(keys: List[String])(implicit store: Store): IO[List[Flag]] = keys.traverse(fatalFlag)
}
