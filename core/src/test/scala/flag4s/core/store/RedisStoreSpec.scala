package flag4s.core.store

import scala.concurrent.ExecutionContext.Implicits.global

import com.redis.RedisClient
import com.redis.serialization.{Format, Parse}
import org.scalatest.WordSpec

import flag4s.core.FeatureSpec

class RedisStoreSpec
  extends WordSpec with FeatureSpec {

  "store" should {
    "successfully store a key/value" in {
      implicit val s = store
      val key = randomKey
      for {
        res <- store.put(key, true)
      } yield res.isRight shouldBe true
    }

    "successfully get a key's value" in {
      implicit val s = store
      val key = randomKey
      for {
        res <- store.put(key, "value")
      } yield {
        res.isRight shouldBe true
        res.toOption.get shouldBe "value"
      }
    }

    "successfully get list of key/values" in {
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
  }

  def store: Store = new MockStore

  class MockClient extends RedisClient {
    override def initialize : Boolean = true

    override def get[A](key: Any)(implicit format: Format, parse: Parse[A]): Option[A] = Some("{\"value\": \"on\"}".asInstanceOf[A])

    override def set(key: Any, value: Any)(implicit format: Format): Boolean = true

    override def keys[A](pattern: Any = "*")(implicit format: Format, parse: Parse[A]): Option[List[Option[A]]] = Some(List(Some("key".asInstanceOf[A])))
  }

  class MockStore extends RedisStore("host", 6379) {
    override def redisClient = new MockClient
  }

}
