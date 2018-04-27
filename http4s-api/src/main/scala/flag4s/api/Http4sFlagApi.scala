package flag4s.api

import cats.effect._
import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._
import org.http4s.{HttpService, Response}
import org.http4s.circe._
import org.http4s.dsl.io._

import flag4s.core.store.Store
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

case class Flag(key: String, value: Json)

sealed trait FlagValue {
  def jsValue: Json
}

object Http4sFlagApi {
  def service(implicit store: Store): HttpService[IO] =
    HttpService[IO] {
      case POST -> Root / "flags" / key / "enable" => switchFlag(key, true)
      case POST -> Root / "flags" / key / "disable" => switchFlag(key, false)
      case GET -> Root / "flags" / key => getFlag(key) match {
        case Right(v) => Ok(v.asJson)
        case Left(e) => BadRequest(toJson(e))
      }
      case GET -> Root / "flags" =>
        store.keys().unsafeRunSync().flatMap(a => a.traverse(getFlag)) match {
          case Right(flags) => Ok(flags.asJson)
          case Left(e) => BadRequest(toJson(e))
        }
    }

  def getFlag(key: String)(implicit store: Store): Either[Throwable, Flag] =
    store.rawValue(key).unsafeRunSync().map(b => Flag(key, decodeFlag(b)))

  def decodeFlag(body: Json): Json = body.hcursor.downField("value").focus.getOrElse(Json.Null)

  def switchFlag[T: Encoder](key: String, value: T)(implicit store: Store): IO[Response[IO]] =
    store.put(key, value).unsafeRunSync() match {
      case Right(v) => Ok(v.asJson)
      case Left(e) => NotFound(toJson(e))
    }

  def toJson(e: Throwable): Json = e.getMessage.asJson
}
