package flag4s

import cats.effect.IO
import cats.syntax.either._

import flag4s.core.store.Store
import io.circe.Encoder
import io.circe.syntax._

package object core extends FlagOps {
  def error(message: String): Throwable = new RuntimeException(message)

  def checkType[A: Encoder](flag: Flag, value: A): IO[Either[Throwable, Unit]] = {
    val org = flag.value.name
    val des = value.asJson.name
    IO.pure(if (org == des) Right((): Unit) else error(s"type mismatch! can't replace $org with $des").asLeft)
  }

  def switchFlag[A: Encoder](key: String, value: A)(implicit store: Store): IO[Either[Throwable, A]] = {
    val valid = flag(key).map {
      case Right(f) => checkType(f, value).unsafeRunSync()
      case _ => Right((): Unit)
    }

    IO.pure {
      for {
        _ <- valid.unsafeRunSync()
        res <- store.put(key, value).unsafeRunSync()
      } yield res
    }
  }
}
