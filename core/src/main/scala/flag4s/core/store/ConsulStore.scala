package flag4s.core.store

import cats.effect._
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Encoder, Json}
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.{Request, Uri}
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.Client
import org.http4s.dsl.io._


class ConsulStore(
                   val client: Client[IO],
                   host: String,
                   port: Int,
                   basePath: String = "",
                   aclToken: Option[String] = None
                 ) extends Http4sClientDsl[IO] with Store {
  private val baseUri = s"http://$host:$port/v1/kv"
  private val base = if (basePath.isEmpty) baseUri else s"$baseUri/$basePath"

  override def put[A: Encoder](key: String, value: A): IO[Either[Throwable, A]] = {
    val req: Request[IO] = PUT(Uri.unsafeFromString(s"$base/$key")).withEntity(StoredValue(value.asJson).asJson)
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
             aclToken: Option[String] = None
           ): Resource[IO, ConsulStore] = {
    BlazeClientBuilder[IO].resource.map(new ConsulStore(_, host, port, basePath, aclToken))
  }
}
