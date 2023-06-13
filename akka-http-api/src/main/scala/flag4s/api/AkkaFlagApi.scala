package flag4s.api

import akka.http.scaladsl.server.Directives

import cats.effect.unsafe.implicits.global
import cats.effect.IO
import cats.instances.either._
import cats.instances.list._
import cats.syntax.either._
import cats.syntax.traverse._

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import flag4s.core.{flag, _}
import flag4s.core.store.Store
import io.circe.Json
import io.circe.Encoder._
import io.circe.generic.auto._
import io.circe.syntax._

object AkkaFlagApi extends Directives with FailFastCirceSupport {
  def route(basePath: String = "flags")(implicit store: Store) = pathPrefix(basePath) {
    path(Segment) { key =>
      get {
        flag(key).unsafeRunSync() match {
          case Right(v) => complete(v.asJson)
          case Left(e) => complete(404, errJson(e))
        }
      } ~
        delete {
          store.remove(key).unsafeRunSync() match {
            case Right(_) => complete(())
            case Left(e) => complete(404, errJson(e))
          }
        }
    } ~
      path(Segment / "enable") { key =>
        post {
          switchFlag(key, true).unsafeRunSync() match {
            case Right(v) => complete(v.value.asJson)
            case Left(e) => complete(400, errJson(e))
          }
        }
      } ~
      path(Segment / "disable") { key =>
        post {
          switchFlag(key, false).unsafeRunSync() match {
            case Right(v) => complete(v.value.asJson)
            case Left(e) => complete(400, errJson(e))
          }
        }
      } ~
      pathEnd {
        get {
          (for {
            keys <- store.keys()
            flags <- mapKeys(keys.valueOr(_ => List.empty)).map(f => complete(200, f.asJson))
          } yield flags).unsafeRunSync()
        } ~
          put {
            entity(as[Flag]) { flag =>
              switchFlag(flag.key, flag.value).unsafeRunSync() match {
                case Right(v) => complete(200, v.value)
                case Left(e) => complete(400, errJson(e))
              }
            }
          }
      }
  }

  private def errJson(e: Throwable): Json = e.getMessage.asJson

  private def mapKeys(keys: List[String])(implicit store: Store): IO[List[Flag]] = keys.traverse(fatalFlag)
}
