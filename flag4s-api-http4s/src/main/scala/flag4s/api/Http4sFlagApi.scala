package flag4s.api

import scala.concurrent.ExecutionContext

import cats.effect._
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.io._

import flag4s.core.store.Store
import io.circe.Json
import io.circe.syntax._

case class KeyVal(key: String, value: Json)

object Http4sFlagApi {

  def flagService(implicit store: Store, ec: ExecutionContext): HttpService[IO] =
    HttpService[IO] {
      case POST -> Root / "flags" / key / "enable" =>
        store.put(key, true).unsafeRunSync() match {
          case Right(v) => Ok(v.asJson)
          case Left(e) => NotFound(toJson(e))
        }
      case GET -> Root / "flags" =>
        store.keys().unsafeRunSync().map(_.map(mapKey).flatten) match {
          case Right(kv) => Ok(kv.toMap.asJson)
          case Left(e) => BadRequest(toJson(e))
        }
    }

  def toJson(e: Throwable): Json = e.getMessage.asJson

  def mapKey(k: String)(implicit store: Store): Option[(String, String)] =
    store.get[String](k).unsafeRunSync().toOption.map(a => (k, a))
}
