package com.newmotion.ochp
package converters

import Converters._
import CDRConverter._
import api._
import com.github.nscala_time.time.Imports._

class CDRSpec extends Spec {
  "converting a CDR into CDRInfo and back returns the original value" >> {
    val cdr = CDR(
      cdrId = "123456someId123456",
      evseId = "FR*A23*E45B*78C",
      emtId = EmtId(
        tokenId = "96B0149B4EA098BE769EFDE5BD6A7403F3A25BA0",
        tokenType = TokenType.withName("rfid"),
        tokenSubType = Some(TokenSubType.withName("mifareCls")),
        representation = TokenRepresentation.sha256),
      contractId = "DE-LND-C00001516-E",
      liveAuthId = Some("abc"),
      status = CdrStatus.withName("new"),
      startDateTime = DateTimeNoMillis("2014-08-08T10:10:10+01:00"),
      endDateTime = DateTimeNoMillis("2014-08-08T18:10:10+01:00"),
      duration = Some(Duration.standardMinutes(10)),
      houseNumber = Some("585"),
      address = Some("Keizersgracht"),
      zipCode = Some("1017 DR"),
      city = Some("Amsterdam"),
      country = "NL",
      chargePointType = "AC",
      connectorType = Connector(
        connectorStandard = ConnectorStandard.`TESLA-R`,
        connectorFormat = ConnectorFormat.Socket),
      maxSocketPower = 16,
      productType = Some("abc"),
      meterId = Some("1234"),
      chargingPeriods = List(
        CdrPeriod(
          startDateTime = DateTimeNoMillis("2014-08-08T10:10:10+01:00"),
          endDateTime = DateTimeNoMillis("2014-08-08T18:10:10+01:00"),
          billingItem = BillingItem.withName("power"),
          billingValue = 1,
          currency = "EUR",
          itemPrice = 6,
          periodCost = Some(5))))

    fromOchp(cdrToCdrInfo(cdr)) must beSome(cdr)
  }
}
