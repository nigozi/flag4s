package flag4s.api

import cats.effect._
import cats.instances.either._
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.traverse._
import org.http4s.{EntityDecoder, HttpService, Response}
import org.http4s.circe._
import org.http4s.dsl.io._

import flag4s.core._
import flag4s.core.store.Store
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
        getFlag(key) match {
          case Right(v) => Ok(v.asJson)
          case Left(e) => BadRequest(toJson(e))
        }
      case GET -> Root / "flags" =>
        store.keys().unsafeRunSync().flatMap(a => a.traverse(getFlag)) match {
          case Right(flags) => Ok(flags.asJson)
          case Left(e) => BadRequest(toJson(e))
        }
      case DELETE -> Root / "flags" / key =>
        store.remove(key).unsafeRunSync() match {
          case Right(_) => Ok()
          case Left(e) => BadRequest(toJson(e))
        }
    }

  def getFlag(key: String)(implicit store: Store): Either[Throwable, JsonFlag] = flag(key).unsafeRunSync().map(toJsonFag)

  def switchFlag[A: Encoder](key: String, value: A)(implicit store: Store): IO[Response[IO]] = {
    def validateType(exf: Json): Either[Throwable, Boolean] = Either.fromOption(
      exf.hcursor.downField("value").focus.map(_.name == value.asJson.name), new RuntimeException("")
    )

    val response = for {
      exf <- store.rawValue(key)
      valid <- exf.flatMap(validateType).toOption.getOrElse(true).pure[IO]
      res <- if (valid) store.put(key, value) else Left(new RuntimeException("type mismatch")).pure[IO]
    } yield res match {
      case Right(r) => Ok(r.asJson)
      case Left(e) => BadRequest(toJson(e))
    }

    response.unsafeRunSync()
  }


  def toJson(e: Throwable): Json = e.getMessage.asJson
}
