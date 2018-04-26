package flag4s

package object core {
  def flag(key: String): Flag = new Flag(key)
}
