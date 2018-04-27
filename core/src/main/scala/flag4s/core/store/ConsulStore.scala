package flag4s.core.store

import scala.concurrent.ExecutionContext

import cats.effect._
import org.http4s.Uri
import org.http4s.circe._
import org.http4s.client.{blaze, Client}
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.io._

import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

class ConsulStore(
  host: String,
  port: Int,
  basePath: String = "/",
  aclToken: Option[String] = None)(implicit ec: ExecutionContext) extends Http4sClientDsl[IO] with Store {

  val base_uri = s"http://$host:$port/v1/kv"

  val client: Client[IO] = blaze.Http1Client[IO]().unsafeRunSync()

  override def put[T: Encoder](key: String, value: T): IO[Either[Throwable, T]] = {
    val req = PUT(Uri.unsafeFromString(s"$base_uri$basePath$key")).withBody(StoredValue(value).asJson)
    for {
      resp <- client.expect[String](req).attempt
    } yield resp.map(_ => value)
  }

  override def get[T: Decoder](key: String): IO[Either[Throwable, T]] = {
    val req = GET(Uri.unsafeFromString(s"$base_uri$basePath$key?raw=true"))
    for {
      resp <- client.expect[String](req).attempt
    } yield resp.flatMap(r => decode[StoredValue[T]](r).map(_.value))
  }

  override def keys(): IO[Either[Throwable, List[String]]] = {
    val req = GET(Uri.unsafeFromString(s"$base_uri$basePath?keys"))
    for {
      resp <- client.expect[String](req).attempt
    } yield resp.flatMap(r => decode[List[String]](r))
  }

  override def remove(key: String): IO[Either[Throwable, Unit]] = {
    val req = DELETE(Uri.unsafeFromString(s"$base_uri$basePath$key"))
    for {
      resp <- client.expect[String](req).attempt
    } yield resp.map(_ => (): Unit)
  }
}

object ConsulStore {
  def apply(
    host: String,
    port: Int,
    basePath: String = "/",
    aclToken: Option[String] = None)(implicit ec: ExecutionContext): ConsulStore =
    new ConsulStore(host, port, basePath, aclToken)
}