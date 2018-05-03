package flag4s.core.store

import scala.concurrent.ExecutionContext.Implicits.global

import com.redis.RedisClient
import com.redis.serialization.{Format, Parse}
import org.scalatest.WordSpec

import flag4s.core.FeatureSpec

class RedisStoreSpec
  extends WordSpec with FeatureSpec {

  "store" should {
    "store a key/val" in {
      implicit val s = store
      val key = randomKey
      for {
        res <- store.put(key, true)
      } yield res.isRight shouldBe true
    }

    "fail to store a key/val" in {
      implicit val s = store
      val key = randomKey
      for {
        res <- failingStore.put(key, true)
      } yield res.isLeft shouldBe true
    }

    "find a key" in {
      implicit val s = store
      val key = randomKey
      for {
        res <- store.put(key, "value")
      } yield {
        res.isRight shouldBe true
        res.toOption.get shouldBe "value"
      }
    }

    "fail to find a key" in {
      implicit val s = store
      val key = randomKey
      for {
        res <- failingStore.put(key, "value")
      } yield res.isLeft shouldBe true
    }

    "get key/vals" in {
      implicit val s = store
      val key1 = randomKey
      val key2 = randomKey
      for {
        _ <- store.put(key1, true)
        _ <- store.put(key2, true)
        res <- store.keys()
      } yield {
        res.isRight shouldBe true
        res.right.get should contain allElementsOf List(key1, key2)
      }
    }

    "fail to get key/vals" in {
      implicit val s = store
      for {
        res <- failingStore.keys()
      } yield res.isLeft shouldBe true
    }
  }

  def store: Store = new MockStore
  def failingStore: Store = new MockStore

  class MockClient extends RedisClient {
    override def initialize : Boolean = true

    override def get[A](key: Any)(implicit format: Format, parse: Parse[A]): Option[A] = Some("{\"value\": \"on\"}".asInstanceOf[A])

    override def set(key: Any, value: Any)(implicit format: Format): Boolean = true

    override def del(key: Any, keys: Any*)(implicit format: Format): Option[Long] = Some(1l)

    override def keys[A](pattern: Any = "*")(implicit format: Format, parse: Parse[A]): Option[List[Option[A]]] = Some(List(Some("key".asInstanceOf[A])))
  }

  class FailingClient extends RedisClient {
    override def initialize : Boolean = true

    override def get[A](key: Any)(implicit format: Format, parse: Parse[A]): Option[A] = None

    override def set(key: Any, value: Any)(implicit format: Format): Boolean = false

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
