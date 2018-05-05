package flag4s.core.store

import scala.concurrent.ExecutionContext.Implicits.global

import cats.effect._
import cats.implicits._
import org.http4s.{HttpService, Response}
import org.http4s.Method._
import org.http4s.Status._
import org.http4s.circe._
import org.http4s.client.Client
import org.scalatest.WordSpec

import flag4s.core.FeatureSpec
import io.circe.generic.auto._
import io.circe.syntax._

class ConsulStoreSpec extends WordSpec with FeatureSpec {
  val store = new MockStore
  val failingStore = new FailingStore

  "store" should {
    "get a flag" in {
      val res = store.get[String]("key").unsafeRunSync()

      res.isRight shouldBe true
      res.right.get shouldBe "value"
    }
    "fail to get a flag" in {
      failingStore.get("key").unsafeRunSync().isLeft shouldBe true
    }
    "put a flag" in {
      val res = store.put("key", "value").unsafeRunSync()

      res.isRight shouldBe true
      res.right.get shouldBe "value"
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

class MockStore extends ConsulStore("host", 8500) {
  val service = HttpService[IO] {
    case r if r.method == GET && r.uri.query.toString().contains("raw=true") => Response[IO](Ok).withBody(StoredValue("value".asJson).asJson)
    case r if r.method == GET && r.uri.query.toString().contains("keys") => Response[IO](Ok).withBody("[\"featureA\", \"featureB\"]".asJson)
    case r if r.method == PUT => Response[IO](Ok).withBodyStream(r.body).pure[IO]
    case r if r.method == DELETE => Response[IO](Ok).withBodyStream(r.body).pure[IO]
  }

  override val client = Client.fromHttpService(service)
}

class FailingStore extends ConsulStore("host", 8500) {
  val service = HttpService[IO] {
    case r if r.method == GET && r.uri.query.toString().contains("raw=true") => Response[IO](NotFound).pure[IO]
    case r if r.method == GET && r.uri.query.toString().contains("keys") => Response[IO](BadRequest).pure[IO]
    case r if r.method == PUT => Response[IO](BadRequest).pure[IO]
    case r if r.method == DELETE => Response[IO](BadRequest).pure[IO]
  }

  override val client = Client.fromHttpService(service)
}


