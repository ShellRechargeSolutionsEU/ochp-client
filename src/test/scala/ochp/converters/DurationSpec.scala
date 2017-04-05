package com.newmotion.ochp
package converters

import converters.DurationConverter._


class DurationSpec extends Spec {
  "converting from Ochp and back yields the original value" >> {
    val duration = "123:45:59"

    fromOchp(duration).map(toOchp) must beSuccessfulTry(duration)
  }
}
