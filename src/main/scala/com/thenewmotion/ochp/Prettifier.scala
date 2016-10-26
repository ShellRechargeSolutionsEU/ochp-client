package com.thenewmotion.ochp

import eu.ochp._1._
import scala.collection.JavaConverters._


object Prettifier {
  def prettyPrint(c: CDRInfo) =
    s"""
    {
      id: ${c.getCdrId},
      chargePointType: ${c.getChargePointType},
      contractId: ${c.getContractId},
      duration: ${c.getDuration},
      emtId: ${emtIdToString(c.getEmtId)},
      liveAuthId: ${c.getLiveAuthId},
      startDateTime: ${c.getStartDateTime.getLocalDateTime},
      endDateTime: ${c.getEndDateTime.getLocalDateTime},
      evseId: ${c.getEvseId},
      maxSocketPower: ${c.getMaxSocketPower},
      connectorType: ${connectorTypeToString(c.getConnectorType)},
      status: ${c.getStatus.getCdrStatusType},
      country: ${c.getCountry},
      city: ${c.getCity},
      address: ${c.getAddress},
      houseNumber: ${c.getHouseNumber},
      zipCode: ${c.getZipCode},
      productType: ${c.getProductType},
      meterId: ${c.getMeterId},
      chargingPeriods: ${c.getChargingPeriods.asScala.map(periodToString)}
    }
    """

  private def emtIdToString(e: EmtId) =
    s"""
      {
        instance: ${e.getInstance},
        tokenType: ${e.getTokenType},
        tokenSubType: ${e.getTokenSubType}
      }
    """

  private def connectorTypeToString(t: ConnectorType) =
    s"""
      {
        standard: ${t.getConnectorStandard.getConnectorStandard},
        format: ${t.getConnectorFormat.getConnectorFormat}
      }
    """

  private def periodToString(p: CdrPeriodType) =
    s"""
      {
        startDateTime: ${p.getStartDateTime.getLocalDateTime},
        endDateTime: ${p.getEndDateTime.getLocalDateTime},
        billingItem: ${p.getBillingItem.getBillingItemType},
        billingValue: ${p.getBillingValue},
        currency: ${p.getCurrency},
        price: ${p.getItemPrice},
        cost: ${p.getPeriodCost}
      }
    """
}