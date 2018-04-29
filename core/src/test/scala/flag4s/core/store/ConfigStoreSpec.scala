package flag4s.core.store

import org.scalatest.WordSpec
import scala.concurrent.ExecutionContext.Implicits.global

import flag4s.core.FeatureSpec

class ConfigStoreSpec extends WordSpec with FeatureSpec {
  private val configFile = getClass.getResource("/config/feature_set_1.conf")
  private val store = ConfigStore(configFile.getPath)

  "store" should {
    "successfully read a boolean key" in {
      for {
        value <- store.get[Boolean]("boolFlag")
      } yield {
        value.isRight shouldBe true
        value.right.get shouldBe "true"
      }
    }
    "successfully read a string key" in {
      for {
        value <- store.get[String]("stringFlag")
      } yield {
      value.isRight shouldBe true
      value.right.get shouldBe "on"
      }
    }
    "successfully read a double key" in {
      for {
        value <- store.get[Double]("doubleFlag")
      } yield {
      value.isRight shouldBe true
      value.right.get shouldBe "1.5"
      }
    }
    "not be able to update a value" in {
      for {
        res <- store.put("boolFlag", false)
      } yield res.isLeft shouldBe true
    }
    "not be able to delete a value" in {
      for {
        res <- store.remove("boolFlag")
      } yield res.isLeft shouldBe true
    }
  }
}
