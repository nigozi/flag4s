package flag4s.core.store

import scala.concurrent.ExecutionContext.Implicits.global

import cats.effect._
import cats.implicits._
import org.http4s.{HttpService, Response}
import org.http4s.Method._
import org.http4s.Status.Ok
import org.http4s.circe._
import org.http4s.client.Client
import org.scalatest.WordSpec

import flag4s.core.FeatureSpec
import io.circe.generic.auto._
import io.circe.syntax._

class ConsulStoreSpec extends WordSpec with FeatureSpec {
  val store = new MockStore

  "store" should {
    "get a flag" in {
      for {
        res <- store.get("key")
      } yield {
        res.isRight shouldBe true
        res.right.get shouldBe "value"
      }
    }
    "put a flag" in {
      for {
        res <- store.put("key", "value")
      } yield {
        res.isRight shouldBe true
        res.right.get shouldBe "value"
      }
    }
    "get flags" in {
      for {
        res <- store.keys()
      } yield {
        res.isRight shouldBe true
        res.right.get.size shouldBe 2
      }
    }
    "delete flag" in {
      for {
        res <- store.remove("key")
      } yield res.isRight shouldBe true
    }
  }
}

class MockStore extends ConsulStore("host", 8500) {
  val service = HttpService[IO] {
    case r if r.method == GET && r.uri.query.toString().contains("raw=true") => Response[IO](Ok).withBody(StoredValue("value".asJson).asJson)
    case r if r.method == GET && r.uri.query.toString().contains("keys") => Response[IO](Ok).withBody("[\"key1\", \"key2\"]".asJson)
    case r if r.method == PUT => Response[IO](Ok).withBodyStream(r.body).pure[IO]
    case r if r.method == DELETE => Response[IO](Ok).withBodyStream(r.body).pure[IO]
  }

  override val client = Client.fromHttpService(service)
}


