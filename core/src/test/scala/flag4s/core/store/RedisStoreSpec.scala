package flag4s.core.store

import com.redis.RedisClient
import com.redis.api.StringApi
import com.redis.serialization.{Format, Parse}
import flag4s.core.FeatureSpec
import org.scalatest.wordspec.AnyWordSpec

import cats.effect.unsafe.implicits.global
import scala.concurrent.duration.Duration

class RedisStoreSpec
  extends AnyWordSpec with FeatureSpec {

  "store" should {
    "store a key/val" in {
      store.put("key", true).unsafeRunSync().isRight shouldBe true
    }

    "fail to store a key/val" in {
      failingStore.put("key", true).unsafeRunSync().isLeft shouldBe true
    }

    "find a key" in {
      val res = store.put("key", "value").unsafeRunSync()

      res.isRight shouldBe true
      res.toOption.get shouldBe "value"
    }

    "fail to find a key" in {
      failingStore.put("key", "value").unsafeRunSync().isLeft shouldBe true
    }

    "get key/vals" in {
      val res = store.keys().unsafeRunSync()

      res.isRight shouldBe true
      res.toOption.get should contain allElementsOf List("key")
    }

    "fail to get key/vals" in {
      failingStore.keys().unsafeRunSync().isLeft shouldBe true
    }
  }

  def store: Store = new MockStore

  def failingStore: Store = new FailingStore

  class MockClient extends RedisClient {
    override def connect: Boolean = true

    override def get[A](key: Any)(implicit format: Format, parse: Parse[A]): Option[A] = Some("{\"value\": \"on\"}".asInstanceOf[A])

    override def set(key: Any, value: Any, whenSet: StringApi.SetBehaviour, expire: Duration)(implicit format: Format): Boolean = true

    override def del(key: Any, keys: Any*)(implicit format: Format): Option[Long] = Some(1L)

    override def keys[A](pattern: Any = "*")(implicit format: Format, parse: Parse[A]): Option[List[Option[A]]] = Some(List(Some("key".asInstanceOf[A])))
  }

  class FailingClient extends RedisClient {
    override def connect: Boolean = true

    override def get[A](key: Any)(implicit format: Format, parse: Parse[A]): Option[A] = None

    override def set(key: Any, value: Any, whenSet: StringApi.SetBehaviour, expire: Duration)(implicit format: Format): Boolean = false

    override def del(key: Any, keys: Any*)(implicit format: Format): Option[Long] = None

    override def keys[A](pattern: Any = "*")(implicit format: Format, parse: Parse[A]): Option[List[Option[A]]] = None
  }

  class MockStore extends RedisStore("host", 6379) {
    override def redisClient = new MockClient
  }

  class FailingStore extends RedisStore("host", 6379) {
    override def redisClient = new FailingClient
  }

}
