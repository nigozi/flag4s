package flag4s.parser

import flag4s.core.{BooleanValue, DoubleValue, Flag, FlagValue, StringValue}
import io.circe.Json
import cats.syntax.either._

trait FlagParser {
  def jsonToFlag(key: String, value: Json): Either[Throwable, Flag] = {
    def parseValue(js: Json): Either[Throwable, Json] = Either.fromOption(js.hcursor.downField("value").focus, new RuntimeException("failed to parse the value"))

    for {
      v <- parseValue(value)
      dv <- jsonToFlagValue(v).map(v => Flag(key, v))
    } yield dv
  }

  def jsonToFlagValue(v: Json): Either[Throwable, FlagValue] = {
    def convert[A](js: Json)(f: Json => Option[A]) = Either.fromOption(f.apply(js), new RuntimeException("failed to parse the value"))

    v.name match {
      case "Boolean" => convert(v)(js => js.asBoolean.map(BooleanValue))
      case "String" => convert(v)(js => js.asString.map(StringValue))
      case "Number" => convert(v)(js => js.asNumber.map(_.toDouble).map(DoubleValue))
      case _ => Left(new RuntimeException("unknown type"))
    }
  }
}
