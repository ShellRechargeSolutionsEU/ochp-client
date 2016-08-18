package com.thenewmotion.ochp
package converters

import api._
import eu.ochp._1.CDRInfo
import DateTimeConverters._

import org.slf4j.LoggerFactory
import scala.collection.JavaConverters._
import scala.util.{Failure, Success}


object CDRConverter extends Common {
  private val logger = LoggerFactory.getLogger(CDRConverter.getClass)

  def fromOchp(cdrinfo: CDRInfo): Option[CDR] = {
    val cdr = for {
      duration <- safeReadWith(cdrinfo.getDuration)(DurationConverter.fromOchp(_))
    } yield CDR (
      cdrId = cdrinfo.getCdrId,
      evseId = cdrinfo.getEvseId,
      emtId = EmtId(
        tokenId = cdrinfo.getEmtId.getInstance,
        tokenType = TokenType.withName(cdrinfo.getEmtId.getTokenType),
        tokenSubType = Option(cdrinfo.getEmtId.getTokenSubType) map {TokenSubType.withName}
      ),
      contractId = cdrinfo.getContractId,
      liveAuthId = toNonEmptyOption(cdrinfo.getLiveAuthId),
      status = CdrStatus.withName(cdrinfo.getStatus.getCdrStatusType),
      startDateTime = WithOffset.fromOchp(cdrinfo.getStartDateTime),
      endDateTime = WithOffset.fromOchp(cdrinfo.getEndDateTime),
      duration = duration,
      houseNumber = toNonEmptyOption(cdrinfo.getHouseNumber),
      address = toNonEmptyOption(cdrinfo.getAddress),
      zipCode = toNonEmptyOption(cdrinfo.getZipCode),
      city = toNonEmptyOption(cdrinfo.getCity),
      country = cdrinfo.getCountry,
      chargePointType = cdrinfo.getChargePointType,
      connectorType = Connector(
        connectorStandard = ConnectorStandard.withName(
          cdrinfo.getConnectorType.getConnectorStandard.getConnectorStandard),
        connectorFormat = ConnectorFormat.withName(
          cdrinfo.getConnectorType.getConnectorFormat.getConnectorFormat)),
      maxSocketPower = cdrinfo.getMaxSocketPower,
      productType = toNonEmptyOption(cdrinfo.getProductType),
      meterId = toNonEmptyOption(cdrinfo.getMeterId),
      chargingPeriods = cdrinfo.getChargingPeriods.asScala.toList.map( cdrPeriod=> {
        val cost = cdrPeriod.getPeriodCost
        CdrPeriod(
          startDateTime = WithOffset.fromOchp(cdrPeriod.getStartDateTime),
          endDateTime = WithOffset.fromOchp(cdrPeriod.getEndDateTime),
          billingItem = BillingItem.withName(cdrPeriod.getBillingItem.getBillingItemType),
          billingValue = cdrPeriod.getBillingValue,
          currency = cdrPeriod.getCurrency,
          itemPrice = cdrPeriod.getItemPrice,
          periodCost = Option(cost).map(_.toFloat)
        )
      })
    )

    cdr match {
      case Success(x) => Some(x)
      case Failure(e) => logger.error("CDR conversion failure", e); None
    }
  }
}
