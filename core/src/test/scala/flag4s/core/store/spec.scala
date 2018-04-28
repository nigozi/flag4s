package flag4s.core.store

import java.util.UUID

import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers}

trait FeatureSpec
  extends Matchers
    with MockFactory
    with BeforeAndAfterEach
    with BeforeAndAfterAll {
  def randomKey: String = UUID.randomUUID().toString.take(10)
}
