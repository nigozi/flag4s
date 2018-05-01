package flag4s.api

import cats.effect.IO
import cats.implicits._
import cats.instances.either._
import org.http4s.{EntityDecoder, HttpService, Uri}
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.io._
import org.scalatest.WordSpec

import flag4s.core._
import flag4s.core.store.Store
import io.circe.{Decoder, Json}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

class Http4sFlagApiSpec extends WordSpec with Http4sClientDsl[IO] with FeatureSpec {
  implicit val store: InMemoryStore = new InMemoryStore
  implicit val decoder: EntityDecoder[IO, Flag] = jsonOf[IO, Flag]
  val service: HttpService[IO] = flagService(store)

  "api" should {
    "read a flag" in {
      val key = randomKey
      val req = GET(Uri.unsafeFromString(s"/flags/$key")).unsafeRunSync()
      store.put(key, true).unsafeRunSync()
      val res = service.run(req).value.map(_.get)
      val expected = Json.obj(
        ("key", Json.fromString(key)),
        ("value", Json.fromBoolean(true))
      )

      statusCheck(res, Ok) shouldBe true
      bodyCheck[Json](res, expected) shouldBe true
    }
    "return 404 if flag not found" in {
      val key = randomKey
      val req = GET(Uri.unsafeFromString(s"/flags/$key")).unsafeRunSync()
      val res = service.run(req).value.map(_.get)

      statusCheck(res, NotFound) shouldBe true
    }
    "return list of flags" in {
      val s = new InMemoryStore
      val key1 = randomKey
      val key2 = randomKey
      val req = GET(Uri.unsafeFromString("/flags")).unsafeRunSync()
      s.put(key1, true).unsafeRunSync()
      s.put(key2, false).unsafeRunSync()
      val res = flagService(s).run(req).value.map(_.get)
      val expected = List(
        Flag(key1, Json.fromBoolean(true)),
        Flag(key2, Json.fromBoolean(false))
      )

      statusCheck(res, Ok) shouldBe true

      val body = res.unsafeRunSync().as[String].unsafeRunSync()
      decode[List[Flag]](body).toOption.get should contain allElementsOf expected
    }
    "save a new flag" in {
      val key = randomKey
      val req = PUT(Uri.unsafeFromString("/flags")).withBody(Flag(key, true.asJson).asJson).unsafeRunSync()
      val res = service.run(req).value.map(_.get)

      statusCheck(res, Ok) shouldBe true
      val saved = store.get[Boolean](key).unsafeRunSync()
      saved.isRight shouldBe true
      saved.right.get shouldBe true
    }
    "remove a flag" in {
      val key = randomKey
      val req = DELETE(Uri.unsafeFromString(s"/flags/$key")).unsafeRunSync()
      store.put(key, true).unsafeRunSync()
      val res = service.run(req).value.map(_.get)

      statusCheck(res, Ok) shouldBe true
      store.get[Boolean](key).unsafeRunSync().isLeft shouldBe true
    }
    "change a flag's value" in {
      val key = randomKey
      store.put(key, true).unsafeRunSync()

      val req = PUT(Uri.unsafeFromString("/flags")).withBody(Flag(key, false.asJson).asJson).unsafeRunSync()
      val res = service.run(req).value.map(_.get)

      statusCheck(res, Ok) shouldBe true

      val current = store.get[Boolean](key).unsafeRunSync()
      current.isRight shouldBe true
      current.right.get shouldBe false
    }
  }

  private def flagService(store: Store) = Http4sFlagApi.service()(store)
}
