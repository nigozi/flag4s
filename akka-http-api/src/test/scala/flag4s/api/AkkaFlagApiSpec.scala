package flag4s.api

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{ContentTypes, MessageEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import flag4s.core._
import flag4s.core.store.Store
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.wordspec.AnyWordSpec
import cats.effect.unsafe.implicits.global


class AkkaFlagApiSpec extends AnyWordSpec with ScalatestRouteTest with FailFastCirceSupport with FeatureSpec {
  implicit val store: InMemoryStore = new InMemoryStore
  val r = flagRoute(store)

  "api" should {
    "read a flag" in {
      val key = randomKey
      val req = Get(s"/flags/$key")
      store.put(key, true).unsafeRunSync()

      val expected = Json.obj(
        ("key", Json.fromString(key)),
        ("value", Json.fromBoolean(true))
      )
      req ~> r ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        responseAs[Json] shouldBe expected
      }
    }
    "return 404 if flag not found" in {
      val key = randomKey
      val req = Get(s"/flags/$key")

      req ~> r ~> check {
        status should ===(StatusCodes.NotFound)
      }
    }
    "return list of flags" in {
      val s = new InMemoryStore
      val key1 = randomKey
      val key2 = randomKey
      val req = Get("/flags")
      s.put(key1, true).unsafeRunSync()
      s.put(key2, false).unsafeRunSync()
      val expected = List(
        Flag(key1, Json.fromBoolean(true)),
        Flag(key2, Json.fromBoolean(false))
      )

      req ~> flagRoute(s) ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        responseAs[List[Flag]] should contain allElementsOf expected
      }
    }
    "save a new flag" in {
      val key = randomKey
      val entity = Marshal(Flag(key, true.asJson)).to[MessageEntity].futureValue
      val req = Put("/flags").withEntity(entity)

      req ~> r ~> check {
        status should ===(StatusCodes.OK)
      }

      val saved = store.get[Boolean](key).unsafeRunSync()
      saved.isRight shouldBe true
      saved.toOption.get shouldBe true
    }
    "remove a flag" in {
      val key = randomKey
      val req = Delete(s"/flags/$key")
      store.put(key, true).unsafeRunSync()

      req ~> r ~> check {
        status should ===(StatusCodes.OK)
      }

      store.get[Boolean](key).unsafeRunSync().isLeft shouldBe true
    }
  }

  def flagRoute(store: Store): Route = AkkaFlagApi.route()(store)
}
