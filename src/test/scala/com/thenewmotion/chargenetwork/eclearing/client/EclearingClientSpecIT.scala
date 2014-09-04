package com.thenewmotion.chargenetwork.eclearing.client

import com.thenewmotion.chargenetwork.eclearing.api.{ExpiryDate, TokenSubType, Card}
import com.thenewmotion.chargenetwork.eclearing.{EclearingConfig, Converters}
import com.typesafe.config._
import eu.ochp._1.RoamingAuthorisationInfo
import org.specs2.mutable.SpecificationWithJUnit


/**
 * EclearingClient integration tests.
 * need a soapUI mock service running on 8088.
 * The respective project can be found in
 * test/resources/soapui/E-Clearing-soapui-project.xml (SoauUI v5.0.0) *
 *
 *
 * @author Christoph Zwirello
 */
class EclearingClientSpecIT extends SpecificationWithJUnit with TestData{
  args(sequential = true)




  "EclearingClient" should {


//    "receive cdrs" >> {
//      client.cdrs()
//      success
//    }.pendingUntilFixed

//    "add CDRs" >> {
//      client.addCdrs(Seq(CDRInfo(
//        cdrId = "01001851_139038927377890",
//        startDatetime = "2014-01-22 10:16:38",
//        endDatetime = "2014-01-22 12:14:33",
//        duration = "7074",
//        volume = "4.6400",
//        chargePointAddress = "Roggestraat 111-163",
//        chargePointZip = "7311",
//        chargePointCity = "Apeldoorn",
//        chargePointCountry = "NLD",
//        chargePointType = "2",
//        productType = "0",
//        tariffType = "A0",
//        authenticationId = "",
//        evcoId = "NL-ANW-133094-6",
//        meterId = "01000851",
//        obisCode = "",
//        chargePointId = "01000851",
//        serviceProviderId = "ANWB",
//        infraProviderId = "TheNewMotion",
//        evseId = "01000851")))
//      success
//    }.pendingUntilFixed("CDR operations giving inexplicable errors")

    " indicate in result that WS is not reachable" >> {
      val authList = faultyClient.roamingAuthorisationList()
      val cards = authList.map(Converters.roamingAuthorisationInfoToCard)
//      cards(0).evcoId === "YYABCC00000003"
      success
    }

    " receive roamingAuthorisationList" >> {
      val authList = client.roamingAuthorisationList()
      val cards = authList.map(Converters.roamingAuthorisationInfoToCard)
      cards(0).evcoId === "YYABCC00000003"
    }

    " send roamingAuthorisationList" >> {
      val cards = List(card1)
      val rais = cards.map(Converters.cardToRoamingAuthorisationInfo)
      val result = client.setRoamingAuthorisationList(rais)
      result.resultCode === "ok"
    }

    " return an error for rejected roamingAuthorisationList" >> {
      val cards = List(card2)
      val rais = cards.map(Converters.cardToRoamingAuthorisationInfo)
      val result = client.setRoamingAuthorisationList(rais)
      result.resultCode === "nok"
    }

//    "receive chargepointList" >> {
//      val cps = client.chargepointList()
//      System.out.println(cps.mkString("\n"))
//      success
//    }

//    "set charge point list" >> {
//      client.setChargepointList(Seq(ChargepointInfo(evseId = "02000001",
//        locationName = "The New Motion Office",
//        locationNameLang = "en",
//        houseNumber = "452",
//        streetName = "Keizersgracht",
//        city = "Amsterdam",
//        postalCode = "1016 GD",
//        taLat = "52.0",
//        taLon = "5.0",
//        taLatEntranceExit = "",
//        taLonEntranceExit = "",
//        openingTimes = "",
//        powerOutletStatus = "in use",
//        energyProviderId = "GreenChoice",
//        roamingHubId = "The New Motion",
//        telephoneNumber = "",
//        floorLevel = "0",
//        paymentMethod = "",
//        evChargingReceptacleType = "Mennekes Type II"
//      )))
//      success
//    }

//    "use another URL if given" >> {
//      val client = new EclearingClient("", "", None, Some(new URI("http://example.org/test_override")))
//
//      client.soapBindings.baseAddress.toString mustEqual "http://example.org/test_override"
//    }
  }
}

trait TestData {

  val conf: Config = ConfigFactory.load()

  val client = EclearingClient(
    new EclearingConfig(
      conf.getString("e-clearing.service-uri"),
      conf.getString("e-clearing.user"),
      conf.getString("e-clearing.password"))
  )

  val faultyClient = EclearingClient(
    new EclearingConfig(
      "http://localhost:8",
      conf.getString("e-clearing.user"),
      conf.getString("e-clearing.password"))
  )

  val card1 = Card(
    evcoId = "YYABCC00000003",
    tokenSubType = TokenSubType.withName("mifareCls"),
    tokenId = "96B0149B4EA098BE769EFDE5BD6A7403F3A25BA0",
    printedNumber = "YYABCC00000003J",
    expiryDate = ExpiryDate("2014-07-14T00:00:00Z")
  )

  val card2 = Card(
    evcoId = "YYABCC00000003",
    tokenSubType = TokenSubType.withName("mifareCls"),
    tokenId = "96B0149B4EA098BE769EFDE5BD6A7403F3A25BA1", // cf. last digit!
    printedNumber = "YYABCC00000003J",
    expiryDate = ExpiryDate("2014-07-14T00:00:00Z")
  )
}
