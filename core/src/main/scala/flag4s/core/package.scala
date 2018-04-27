package flag4s

package object core {
  def flag(key: String): Flags = new Flags(key)
}
