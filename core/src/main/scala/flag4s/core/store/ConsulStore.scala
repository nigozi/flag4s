package flag4s.core.store

import scala.concurrent.ExecutionContext

import cats.effect._
import org.http4s.Uri
import org.http4s.circe._
import org.http4s.client.{blaze, Client}
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.io.{GET, _}

import io.circe.{Encoder, Json}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

class ConsulStore(
  host: String,
  port: Int,
  basePath: String = "",
  aclToken: Option[String] = None)(implicit ec: ExecutionContext) extends Http4sClientDsl[IO] with Store {

  private val baseUri = s"http://$host:$port/v1/kv"
  private val base = if (basePath.isEmpty) baseUri else s"$baseUri/$basePath"

  val client: Client[IO] = blaze.Http1Client[IO]().unsafeRunSync()

  override def put[A: Encoder](key: String, value: A): IO[Either[Throwable, A]] = {
    val req = PUT(Uri.unsafeFromString(s"$base/$key")).withBody(StoredValue(value.asJson).asJson)
    for {
      resp <- client.expect[String](req).attempt
    } yield resp.map(_ => value)
  }

  override def rawValue(key: String): IO[Either[Throwable, Json]] =
    for {
      resp <- client.expect[Json](GET(Uri.unsafeFromString(s"$base/$key?raw=true"))).attempt
    } yield resp.flatMap(r => r.as[StoredValue]).map(_.value)

  override def keys(): IO[Either[Throwable, List[String]]] = {
    val req = GET(Uri.unsafeFromString(s"$base/?keys"))
    for {
      resp <- client.expect[String](req).attempt
    } yield resp.flatMap(r => decode[List[String]](r))
  }

  override def remove(key: String): IO[Either[Throwable, Unit]] = {
    val req = DELETE(Uri.unsafeFromString(s"$base/$key"))
    for {
      resp <- client.expect[String](req).attempt
    } yield resp.map(_ => (): Unit)
  }
}

object ConsulStore {
  def apply(
    host: String,
    port: Int,
    basePath: String = "",
    aclToken: Option[String] = None)(implicit ec: ExecutionContext): ConsulStore =
    new ConsulStore(host, port, basePath, aclToken)
}
