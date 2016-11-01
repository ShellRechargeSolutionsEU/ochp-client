package com.thenewmotion.ochp
package converters

import api.{EmtId, TokenRepresentation, TokenSubType, TokenType}

import eu.ochp._1.{EmtId => GenEmtId}


trait EmtIdConverter {
  def toOchp(id: EmtId): GenEmtId = {
    val emtId = new GenEmtId {
      setInstance(id.tokenId)
      setTokenType(id.tokenType.toString)
      setRepresentation(id.representation.toString)
    }

    id.tokenSubType.foreach(st => emtId.setTokenSubType(st.toString))

    emtId
  }

  def fromOchp(value: GenEmtId): EmtId = {
    EmtId(
      tokenId = value.getInstance,
      tokenType = TokenType.withName(value.getTokenType),
      tokenSubType = Option(value.getTokenSubType).map(TokenSubType.withName),
      representation = TokenRepresentation.withName(value.getRepresentation))
  }
}

object EmtIdConverter extends EmtIdConverter