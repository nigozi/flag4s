package flag4s

package object core extends FlagOps {
  def error(message: String): Throwable = new RuntimeException(message)
}
