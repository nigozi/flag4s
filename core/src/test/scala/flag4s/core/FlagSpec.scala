package flag4s.core

import scala.util.Try

import cats.effect.IO
import cats.syntax.applicative._
import org.scalatest.WordSpec

import io.circe.syntax._

class FlagSpec extends WordSpec with FeatureSpec with FlagOps {
  implicit val store: InMemoryStore = new InMemoryStore

  "flag" should {
    "find a flag" in {
      val key = randomKey
      for {
        _ <- store.put(key, true)
        res <- flag(key)
      } yield {
        res.isRight shouldBe true
        res.right.get.value shouldBe true.asJson
      }
    }
    "return proper response if flag doesn't exist" in {
      for {
        res <- flag("non-existing")
      } yield res.isLeft shouldBe true
    }
    "fatal read a flag" in {
      val key = randomKey
      for {
        _ <- store.put(key, true)
        flag <- fatalFlag(key)
      } yield {
        flag.key shouldBe key
        flag.value shouldBe true
      }
    }
    "fatal read returns proper response if flag doesn't exist" in {
      Try(fatalFlag("non-existing").unsafeRunSync()).isFailure shouldBe true
    }
    "return true if flag is enabled" in {
      val key = randomKey
      for {
        _ <- store.put(key, true)
        f <- fatalFlag(key)
        r <- enabled(f)
      } yield r shouldBe true
    }
    "return false if types mismatch" in {
      val key = randomKey
      for {
        _ <- store.put(key, "string")
        f <- fatalFlag(key)
        r <- enabled(f)
      } yield r shouldBe false
    }
    "execute function if flag is enabled" in {
      val key = randomKey
      for {
        _ <- store.put(key, true)
        f <- fatalFlag(key)
        r <- ifEnabled(f)("flag is on")
      } yield {
        r.isRight shouldBe true
        r.right.get shouldBe "flag is on"
      }
    }
    "return left if flag is disabled" in {
      val key = randomKey
      for {
        _ <- store.put(key, false)
        res <- ifEnabled(fatalFlag(key).unsafeRunSync())(true)
      } yield res.isLeft shouldBe true
    }
    "return left if types mismatch" in {
      val key = randomKey
      for {
        _ <- store.put(key, "string")
        f <- fatalFlag(key)
        r <- ifEnabled(f)(true)
      } yield r.isLeft shouldBe true
    }
    "return true if value matches" in {
      val key = randomKey
      for {
        _ <- store.put(key, "string")
        f <- fatalFlag(key)
        r <- is(f, "string")
      } yield r shouldBe true
    }
    "return false if value doesn't match" in {
      val key = randomKey
      for {
        _ <- store.put(key, "foo")
        f <- fatalFlag(key)
        r <- is(f, "bar")
      } yield r shouldBe false
    }
    "return left if types mismatch in value check" in {
      val key = randomKey
      for {
        _ <- store.put(key, true)
        f <- fatalFlag(key)
        r <- is(f, "string")
      } yield r shouldBe false
    }
    "execute function if values match" in {
      val key = randomKey
      for {
        _ <- store.put(key, "foo")
        f <- fatalFlag(key)
        r <- ifIs(f, "foo")(true)
      } yield {
        r.isRight shouldBe true
        r.right.get shouldBe true
      }
    }
    "not execute function if values don't match" in {
      val key = randomKey
      for {
        _ <- store.put(key, "foo")
        f <- fatalFlag(key)
        r <- ifIs(f, "bar")(true)
      } yield r.isRight shouldBe true
    }
    "set flag's value" in {
      val key = randomKey
      for {
        _ <- store.put(key, "foo")
        f <- fatalFlag(key)
        _ <- set(f, "bar")
        r <- store.get[String](key)
      } yield {
        r.isRight shouldBe true
        r.right.get shouldBe "bar"
      }
    }
    "return left if failed to set the flag's value" in {
      val key = randomKey
      for {
        _ <- store.put(key, "foo")
        f <- fatalFlag(key)
        r <- set(f, true)
      } yield r.isLeft shouldBe true
    }
    "create a new flag" in {
      val key = randomKey
      for {
        _ <- newFlag(key, true)
        res <- store.get[Boolean](key)
      } yield {
        res.isRight shouldBe true
        res.right.get shouldBe true
      }
    }
    "not create a new flag if it already exists" in {
      val key = randomKey
      for {
        _ <- store.put(key, true)
        res <- newFlag(key, true)
      } yield res.isLeft shouldBe true
    }
    "run the function if flag's value is true" in {
      val key = randomKey
      for {
        _ <- store.put(key, true)
        res <- withFlag(key, true)("flag is on!")
      } yield res.isRight shouldBe true
    }
    "not run the function if flag's value is false" in {
      val key = randomKey
      for {
        _ <- store.put(key, false)
        res <- withFlag(key, false)("flag is on!")
      } yield res.isLeft shouldBe true
    }
  }
}
