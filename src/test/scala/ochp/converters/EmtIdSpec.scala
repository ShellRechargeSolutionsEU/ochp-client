package com.newmotion.ochp
package converters

import api.{EmtId, TokenRepresentation, TokenSubType, TokenType}
import converters.EmtIdConverter._


class EmtIdSpec extends Spec {
  "converting to Ochp and back yields the original value" >> {
    val emtId = EmtId(
      tokenId = "NLTNMC00000609",
      tokenType = TokenType.rfid,
      tokenSubType = Some(TokenSubType.mifareCls),
      representation = TokenRepresentation.sha160)

    fromOchp(toOchp(emtId)) mustEqual emtId
  }
}
