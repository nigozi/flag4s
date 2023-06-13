package flag4s.core

import io.circe.syntax._
import org.scalatest.wordspec.AnyWordSpec
import cats.effect.unsafe.implicits.global

import scala.util.Try

class FlagSpec extends AnyWordSpec with FeatureSpec with FlagOps {
  implicit val store: InMemoryStore = new InMemoryStore

  "flag" should {
    "find a flag" in {
      val key = randomKey
      store.put(key, true).unsafeRunSync()
      val res = flag(key).unsafeRunSync()

      res.isRight shouldBe true
      res.toOption.get.value shouldBe true.asJson
    }
    "return proper response if flag doesn't exist" in {
      flag("non-existing").unsafeRunSync().isLeft shouldBe true
    }
    "fatal read a flag" in {
      val key = randomKey
      store.put(key, true).unsafeRunSync()
      val flag = fatalFlag(key).unsafeRunSync()

      flag.key shouldBe key
      flag.value shouldBe true.asJson
    }
    "fatal read returns proper response if flag doesn't exist" in {
      Try(fatalFlag("non-existing").unsafeRunSync()).isFailure shouldBe true
    }
    "return true if flag is enabled" in {
      val key = randomKey
      store.put(key, true).unsafeRunSync()
      val f = fatalFlag(key).unsafeRunSync()

      enabled(f).unsafeRunSync() shouldBe true
    }
    "return false if types mismatch" in {
      val key = randomKey
      store.put(key, "string").unsafeRunSync()
      val f = fatalFlag(key).unsafeRunSync()

      enabled(f).unsafeRunSync() shouldBe false
    }
    "execute function if flag is enabled" in {
      val key = randomKey
      store.put(key, true).unsafeRunSync()
      val f = fatalFlag(key).unsafeRunSync()
      val r = ifEnabled(f)("flag is on").unsafeRunSync()

      r.isRight shouldBe true
      r.toOption.get shouldBe "flag is on"
    }
    "return left if flag is disabled" in {
      val key = randomKey
      store.put(key, false).unsafeRunSync()

      ifEnabled(fatalFlag(key).unsafeRunSync())(true).unsafeRunSync().isLeft shouldBe true
    }
    "return left if types mismatch" in {
      val key = randomKey
      store.put(key, "string").unsafeRunSync()
      val f = fatalFlag(key).unsafeRunSync()

      ifEnabled(f)(true).unsafeRunSync().isLeft shouldBe true
    }
    "return true if value matches" in {
      val key = randomKey
      store.put(key, "string").unsafeRunSync()
      val f = fatalFlag(key).unsafeRunSync()

      is(f, "string").unsafeRunSync() shouldBe true
    }
    "return false if value doesn't match" in {
      val key = randomKey
      store.put(key, "foo").unsafeRunSync()
      val f = fatalFlag(key).unsafeRunSync()

      is(f, "bar").unsafeRunSync() shouldBe false
    }
    "return left if types mismatch in value check" in {
      val key = randomKey
      store.put(key, true).unsafeRunSync()
      val f = fatalFlag(key).unsafeRunSync()

      is(f, "string").unsafeRunSync() shouldBe false
    }
    "execute function if values match" in {
      val key = randomKey
      store.put(key, "foo").unsafeRunSync()
      val f = fatalFlag(key).unsafeRunSync()
      val r = ifIs(f, "foo")(true).unsafeRunSync()

      r.isRight shouldBe true
      r.toOption.get shouldBe true
    }
    "not execute function if values don't match" in {
      val key = randomKey
      store.put(key, "foo").unsafeRunSync()
      val f = fatalFlag(key).unsafeRunSync()

      ifIs(f, "bar")(true).unsafeRunSync().isLeft shouldBe true
    }
    "set flag's value" in {
      val key = randomKey
      store.put(key, "foo").unsafeRunSync()
      val f = fatalFlag(key).unsafeRunSync()
      set(f, "bar").unsafeRunSync()
      val r = store.get[String](key).unsafeRunSync()

      r.isRight shouldBe true
      r.toOption.get shouldBe "bar"
    }
    "return left if failed to set the flag's value" in {
      val key = randomKey
      store.put(key, "foo").unsafeRunSync()
      val f = fatalFlag(key).unsafeRunSync()

      set(f, true).unsafeRunSync().isLeft shouldBe true
    }
    "create a new flag" in {
      val key = randomKey
      newFlag(key, true).unsafeRunSync()
      val res = store.get[Boolean](key).unsafeRunSync()

      res.isRight shouldBe true
      res.toOption.get shouldBe true
    }
    "not create a new flag if it already exists" in {
      val key = randomKey
      store.put(key, true).unsafeRunSync()

      newFlag(key, true).unsafeRunSync().isLeft shouldBe true
    }
    "run the function if flag's value is true" in {
      val key = randomKey
      store.put(key, true).unsafeRunSync()

      withFlag(key, true)("flag is on!").unsafeRunSync().isRight shouldBe true
    }
    "not run the function if flag's value is false" in {
      val key = randomKey
      store.put(key, false).unsafeRunSync()

      withFlag(key, true)("flag is on!").unsafeRunSync().isLeft shouldBe true
    }
    "switchFlag should change the flag's value" in {
      val key = randomKey
      store.put(key, false).unsafeRunSync()

      switchFlag(key, true).unsafeRunSync().isRight shouldBe true
      store.get[Boolean](key).unsafeRunSync().toOption.get shouldBe true
    }
    "switchFlag should not change the flag's value if types mismatch" in {
      val key = randomKey
      store.put(key, false).unsafeRunSync()

      switchFlag(key, "on").unsafeRunSync().isLeft shouldBe true
    }
  }
}
