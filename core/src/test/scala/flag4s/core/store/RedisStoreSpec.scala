package flag4s.core.store

import com.github.sebruck.EmbeddedRedis
import org.scalatest.WordSpec
import scala.concurrent.ExecutionContext.Implicits.global

import redis.embedded.RedisServer

class RedisStoreSpec
  extends WordSpec with FeatureSpec with EmbeddedRedis {
  val port = 6378
  val redisServer = new RedisServer(port)

  override def beforeAll(): Unit = {
    super.beforeAll()
    redisServer.start()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    redisServer.stop()
  }

  "store" should {
    "successfully store a key/value" in {
      implicit val store = RedisStore("localhost", port)
      val key = randomKey
      for {
        res <- store.put(key, true)
      } yield res.isRight shouldBe true
    }

    "successfully get a key's value" in {
      implicit val store = RedisStore("localhost", port)
      val key = randomKey
      for {
        res <- store.put(key, "value")
      } yield {
        res.isRight shouldBe true
        res.toOption.get shouldBe "value"
      }
    }

    "successfully get list of key/values" in {
      implicit val store = RedisStore("localhost", port)
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
}
