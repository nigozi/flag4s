package flag4s.core.store

import cats.effect._
import cats.implicits._
import flag4s.core.FeatureSpec
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.Method._
import org.http4s.Status._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.{HttpRoutes, Response}
import org.scalatest.wordspec.AnyWordSpec
import cats.effect.unsafe.implicits.global

class ConsulStoreSpec extends AnyWordSpec with FeatureSpec {
  val store = new ConsulStoreSpec.MockStore
  val failingStore = new ConsulStoreSpec.FailingStore

  "store" should {
    "get a flag" in {
      val res = store.get[String]("key").unsafeRunSync()

      res.isRight shouldBe true
      res.toOption.get shouldBe "value"
    }
    "fail to get a flag" in {
      failingStore.get("key").unsafeRunSync().isLeft shouldBe true
    }
    "put a flag" in {
      val res = store.put("key", "value").unsafeRunSync()

      res.isRight shouldBe true
      res.toOption.get shouldBe "value"
    }
    "fail to put a flag" in {
      failingStore.put("key", "value").unsafeRunSync().isLeft shouldBe true
    }
    "fail to get flags" in {
      failingStore.keys().unsafeRunSync().isLeft shouldBe true
    }
    "delete flag" in {
      store.remove("key").unsafeRunSync().isRight shouldBe true
    }
    "fail to delete flag" in {
      failingStore.remove("key").unsafeRunSync().isLeft shouldBe true
    }
  }
}

object ConsulStoreSpec {

  class MockStore extends ConsulStore(null, "host", 8500) {
    val service: HttpRoutes[IO] = HttpRoutes.of[IO] {
      case r if r.method == GET && r.uri.query.toString().contains("raw=true") =>
        Response[IO](Ok).withEntity(StoredValue("value".asJson).asJson).pure[IO]
      case r if r.method == GET && r.uri.query.toString().contains("keys") =>
        Response[IO](Ok).withEntity("[\"featureA\", \"featureB\"]".asJson).pure[IO]
      case r if r.method == PUT =>
        Response[IO](Ok).withBodyStream(r.body).pure[IO]
      case r if r.method == DELETE =>
        Response[IO](Ok).withBodyStream(r.body).pure[IO]
    }

    override val client: Client[IO] =
      Client.fromHttpApp(service.orNotFound)
  }

  class FailingStore extends ConsulStore(null, "host", 8500) {
    val service: HttpRoutes[IO] = HttpRoutes.of[IO] {
      case r if r.method == GET && r.uri.query.toString().contains("raw=true") => Response[IO](NotFound).pure[IO]
      case r if r.method == GET && r.uri.query.toString().contains("keys") => Response[IO](BadRequest).pure[IO]
      case r if r.method == PUT => Response[IO](BadRequest).pure[IO]
      case r if r.method == DELETE => Response[IO](BadRequest).pure[IO]
    }

    override val client: Client[IO] =
      Client.fromHttpApp(service.orNotFound)
  }

}
