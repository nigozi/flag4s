package flag4s.core.store

import org.scalatest.WordSpec
import scala.concurrent.ExecutionContext.Implicits.global

import flag4s.core.FeatureSpec

class ConfigStoreSpec extends WordSpec with FeatureSpec {
  private val configFile = getClass.getResource("/config/feature_set_1.conf")
  private val store = ConfigStore(configFile.getPath)

  "store" should {
    "read a boolean key" in {
      val value = store.get[String]("boolFlag").unsafeRunSync()
      value.isRight shouldBe true
      value.right.get shouldBe "true"
    }
    "read a string key" in {
      val value = store.get[String]("stringFlag").unsafeRunSync()

      value.isRight shouldBe true
      value.right.get shouldBe "on"
    }
    "read a double key" in {
      val value = store.get[String]("doubleFlag").unsafeRunSync()

      value.isRight shouldBe true
      value.right.get shouldBe "1.5"
    }
    "not be able to update a value" in {
      val res = store.put("boolFlag", false).unsafeRunSync()

      res.isLeft shouldBe true
    }
    "not be able to delete a value" in {
      val res = store.remove("boolFlag").unsafeRunSync()

      res.isLeft shouldBe true
    }
  }
}
