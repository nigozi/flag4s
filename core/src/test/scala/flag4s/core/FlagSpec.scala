package flag4s.core

import scala.util.Try

import cats.effect.IO
import cats.syntax.applicative._
import org.scalatest.WordSpec

class FlagSpec extends WordSpec with FeatureSpec with FlagOps {
  implicit val store: InMemoryStore = new InMemoryStore

  "flag" should {
    "read a flag" in {
      val key = randomKey
      for {
        _ <- store.put(key, true)
        res <- flag(key)
      } yield {
        res.isRight shouldBe true
        res.right.get.v shouldBe BooleanValue
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
        res <- Some(fatalFlag(key)).pure[IO]
      } yield res.nonEmpty shouldBe true
    }
    "fatal read returns proper response if flag doesn't exist" in {
      Try(fatalFlag("non-existing")).isFailure shouldBe true
    }
    "return true if flag is enabled" in {
      val key = randomKey
      for {
        _ <- store.put(key, true)
        res <- enabled(fatalFlag(key))
      } yield {
        res.isRight shouldBe true
        res.right.get shouldBe true
      }
    }
    "return false if types mismatch" in {
      val key = randomKey
      for {
        _ <- store.put(key, "string")
        res <- enabled(fatalFlag(key))
      } yield res.isLeft shouldBe true
    }
    "execute function if flag is enabled" in {
      val key = randomKey
      for {
        _ <- store.put(key, true)
        res <- ifEnabled(fatalFlag(key))(true)
      } yield {
        res.isRight shouldBe true
        res.right.get shouldBe true
      }
    }
    "return left if flag is disabled" in {
      val key = randomKey
      for {
        _ <- store.put(key, false)
        res <- ifEnabled(fatalFlag(key))(true)
      } yield res.isLeft shouldBe true
    }
    "return left if types mismatch" in {
      val key = randomKey
      for {
        _ <- store.put(key, "string")
        res <- ifEnabled(fatalFlag(key))(true)
      } yield res.isLeft shouldBe true
    }
    "return true if value matches" in {
      val key = randomKey
      for {
        _ <- store.put(key, "string")
        res <- is(fatalFlag(key), "string")
      } yield {
        res.isRight shouldBe true
        res.right.get shouldBe true
      }
    }
    "return false if value doesn't match" in {
      val key = randomKey
      for {
        _ <- store.put(key, "foo")
        res <- is(fatalFlag(key), "bar")
      } yield {
        res.isRight shouldBe true
        res.right.get shouldBe false
      }
    }
    "return left if types mismatch in value check" in {
      val key = randomKey
      for {
        _ <- store.put(key, true)
        res <- is(fatalFlag(key), "string")
      } yield res.isLeft shouldBe true
    }
    "execute function if values match" in {
      val key = randomKey
      for {
        _ <- store.put(key, "foo")
        res <- ifIs(fatalFlag(key), "foo")(true)
      } yield {
        res.isRight shouldBe true
        res.right.get shouldBe true
      }
    }
    "not execute function if values don't match" in {
      val key = randomKey
      for {
        _ <- store.put(key, "foo")
        res <- ifIs(fatalFlag(key), "bar")(true)
      } yield res.isRight shouldBe true
    }
    "set flag value" in {
      val key = randomKey
      for {
        _ <- store.put(key, "foo")
        _ <- set(fatalFlag(key), "bar")
        res <- store.get[String](key)
      } yield {
        res.isRight shouldBe true
        res.right.get shouldBe "bar"
      }
    }
    "return left if failed to set the flag's value" in {
      val key = randomKey
      for {
        _ <- store.put(key, "foo")
        res <- set(fatalFlag(key), true)
      } yield res.isLeft shouldBe true
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
  }
}
