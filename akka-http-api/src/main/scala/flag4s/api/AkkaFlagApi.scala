package flag4s.api

import scala.concurrent.ExecutionContext

import akka.http.scaladsl.server.Directives
import cats.instances.either._
import cats.syntax.either._

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import flag4s.core.{flag, _}
import flag4s.core.store.Store
import io.circe.Encoder._
import io.circe.generic.auto._
import io.circe.syntax._

object AkkaFlagApi extends Directives with FailFastCirceSupport {
  def route(basePath: String = "flags")(implicit store: Store, ec: ExecutionContext) = pathPrefix(basePath) {
    path(Segment) { key =>
      get {
        flag(key).unsafeRunSync() match {
          case Right(v) => complete(v.asJson)
          case Left(e) => complete(404, e.getMessage)
        }
      } ~
        delete {
          store.remove(key).unsafeRunSync() match {
            case Right(_) => complete()
            case Left(e) => complete(404, e.getMessage)
          }
        }
    } ~
      path(Segment / "enable") { key =>
        post {
          switchFlag(key, true).unsafeRunSync() match {
            case Right(v) => complete(v.asJson)
            case Left(e) => complete(400, e.getMessage)
          }
        }
      } ~
      path(Segment / "disable") { key =>
        post {
          switchFlag(key, false).unsafeRunSync() match {
            case Right(v) => complete(v.asJson)
            case Left(e) => complete(400, e.getMessage)
          }
        }
      } ~
      pathEnd {
        get {
          (for {
            keys <- store.keys().unsafeRunSync()
            flags <- keys.map(k => fatalFlag(k).asJson).asRight
          } yield flags.asJson) match {
            case Right(r) => complete(200, r)
            case Left(e) => complete(400, e.getMessage)
          }
        } ~
          put {
            entity(as[Flag]) { flag =>
              switchFlag(flag.key, flag.value).unsafeRunSync() match {
                case Right(v) => complete(200, v)
                case Left(e) => complete(400, e)
              }
            }
          }
      }
  }
}
