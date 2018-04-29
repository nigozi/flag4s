package flag4s.api

import cats.effect._
import cats.instances.either._
import cats.syntax.either._
import org.http4s.{EntityDecoder, HttpService, Response}
import org.http4s.circe._
import org.http4s.dsl.io._

import flag4s.core._
import flag4s.core.store.Store
import flag4s.core.store.Store._
import io.circe.{Encoder, _}
import io.circe.Encoder._
import io.circe.generic.auto._
import io.circe.syntax._

object Http4sFlagApi {
  implicit val decoder: EntityDecoder[IO, JsonFlag] = jsonOf[IO, JsonFlag]

  def service(implicit store: Store): HttpService[IO] =
    HttpService[IO] {
      case POST -> Root / "flags" / key / "enable" => switchFlag(key, true)
      case POST -> Root / "flags" / key / "disable" => switchFlag(key, false)
      case req@PUT -> Root / "flags" =>
        for {
          f <- req.decode[JsonFlag](fl => switchFlag(fl.key, fl.value))
        } yield f
      case GET -> Root / "flags" / key =>
        jsonFlag(key).map {
          case Right(v) => Ok(v.asJson).unsafeRunSync()
          case Left(e) => NotFound(errJson(e)).unsafeRunSync()
        }
      case GET -> Root / "flags" =>
        (for {
          keys <- store.keys().unsafeRunSync()
          flags <- keys.map(fatalJsonFlag).asRight
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

  def switchFlag[A: Encoder](key: String, value: A)(implicit store: Store): IO[Response[IO]] = {
    val valid = flag(key).unsafeRunSync().map(f => validateType(f, value)).toOption.getOrElse(true)
    val res = if (valid) store.put(key, value).unsafeRunSync() else new RuntimeException("type mismatch!").asLeft

    res match {
      case Right(r) => Ok(r.asJson)
      case Left(e) => BadRequest(errJson(e))
    }
  }

  private def errJson(e: Throwable): Json = e.getMessage.asJson
}
