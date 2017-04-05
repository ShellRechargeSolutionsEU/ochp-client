package com.thenewmotion.ochp
package converters

import api._
import ChargePointStatus.ChargePointStatus
import com.github.nscala_time.time.Imports._
import DateTimeConverters._
import DurationConverter._
import GeoPointConverters._
import eu.ochp.{_1 => Gen}
import org.slf4j.LoggerFactory
import scala.util.{Try, Success, Failure}
import scala.language.{implicitConversions, postfixOps}
import scala.collection.JavaConverters._

trait Common {

  def safeRead[T, U](possiblyNull: T)(convert: T => U): Try[Option[U]] = {
    Option(possiblyNull) match {
      case None => Success(None)
      case Some(value) =>
        Try(convert(value)).map(Some(_))
      }
  }

  def safeReadWith[T, U](possiblyNull: T)(convert: T => Try[U]): Try[Option[U]] =
    Option(possiblyNull) match {
      case None => Success(None)
      case Some(value) =>
        convert(value).map(Some(_))
      }

  def toNonEmptyOption(value: String): Option[String] =
    Option(value).filter(_.nonEmpty)

}


/**
 *
 * Convert between cxf-generated java classes and nice scala case classes
 *
 */
object Converters extends Common {

  private val logger = LoggerFactory.getLogger(Converters.getClass)

  implicit def roamingAuthorisationInfoToToken(rai: Gen.RoamingAuthorisationInfo): ChargeToken = {
    ChargeToken(
      contractId = rai.getContractId,
      emtId = EmtIdConverter.fromOchp(rai.getEmtId),
      printedNumber = Option(rai.getPrintedNumber),
      expiryDate = DateTimeNoMillis(rai.getExpiryDate.getDateTime)
    )
  }

  implicit def tokenToRoamingAuthorisationInfo(token: ChargeToken): Gen.RoamingAuthorisationInfo = {
    val rai = new Gen.RoamingAuthorisationInfo
    rai.setContractId(token.contractId)
    rai.setEmtId(EmtIdConverter.toOchp(token.emtId))
    token.printedNumber.foreach(pn => rai.setPrintedNumber(pn.toString))
    rai.setExpiryDate(Utc.toOchp(token.expiryDate))
    rai
  }

  private def toRegularHours (rh: Gen.RegularHoursType): Option[RegularHours] = {

    val normalize: PartialFunction[String, String] = {
      case "24:00" => "23:59"
      case x => x
    }

    def toTime(t: String): Option[TimeNoSecs] =
      Option(t).flatMap{v => Try(TimeNoSecs(normalize(v))) match {
        case Success(x) => Some(x)
        case Failure(e) => logger.error("Time value parsing failure", e); None
      }}

    for {
      beg <- toTime(rh.getPeriodBegin)
      end <- toTime(rh.getPeriodEnd)
    } yield RegularHours(rh.getWeekday, beg, end)
  }

  private def toChargePointStatusOption(value: Gen.ChargePointStatusType): Option[ChargePointStatus] =
    Option(value).flatMap(v => Try(ChargePointStatus.withName(v.getChargePointStatusType)) match {
      case Success(x) => Some(x)
      case Failure(e) => logger.error("Charge point status parsing failure", e); None
    })

  private[ochp] def regularOpeningsAreDefined(value: Gen.HoursType) = {
    def prettyPrint(ht: Gen.HoursType) =
      s"""HoursType(
        |regularHours = ${ht.getRegularHours},
        | twentyfourseven = ${ht.isTwentyfourseven},
        | exceptionalOpenings = ${ht.getExceptionalOpenings},
        | exceptionalClosings = ${ht.getExceptionalClosings})
      """.stripMargin.replaceAll("\n", "")

    val `missing 24/7` = Option(value.isTwentyfourseven).isEmpty
    val `illegal 24/7` = Option(value.isTwentyfourseven).exists { _ == false }
    val missingHours =
      value.getRegularHours.isEmpty &&
      value.getExceptionalOpenings.isEmpty &&
      value.getExceptionalClosings.isEmpty

    val invalid = (`missing 24/7` && missingHours) || `illegal 24/7`

    if(invalid)
      Failure(new IllegalArgumentException(
        s"Provided hoursType ${prettyPrint(value)} cannot be accepted because it does not define 24/7 nor any hours"))
    else Success(value)
  }

  private[ochp] def toHoursOption(value: Gen.HoursType): Try[Option[Hours]] = {
    def toPeriod(ept: Gen.ExceptionalPeriodType) =
      ExceptionalPeriod(
        Utc.fromOchp(ept.getPeriodBegin),
        Utc.fromOchp(ept.getPeriodEnd))

    def fromJava(v: Gen.HoursType) =
      Hours(
        regularHoursOrTwentyFourSeven = regularHours(v),
        exceptionalOpenings =
          v.getExceptionalOpenings.asScala.toList.map(toPeriod),
        exceptionalClosings =
          v.getExceptionalClosings.asScala.toList.map(toPeriod))

    def regularHours(v: Gen.HoursType): Either[List[RegularHours], Boolean] =
      Option(v.isTwentyfourseven)
        .map(tfs => Right(tfs == true))
        .getOrElse(Left(v.getRegularHours.asScala.toList.flatMap(toRegularHours)))

    safeReadWith(value)(regularOpeningsAreDefined(_).map(fromJava))
  }

  implicit def cdrToCdrInfo(cdr: CDR): Gen.CDRInfo = {
    def ifNonEmptyThen(opt: Option[String])(f: String => Unit) =
      opt.filter(_.nonEmpty).foreach(f)

    import cdr._
    val cdrInfo = new Gen.CDRInfo
    ifNonEmptyThen(cdr.address)(cdrInfo.setAddress)
    cdrInfo.setCdrId(cdr.cdrId)
    cdrInfo.setChargePointType(cdr.chargePointType)

    val cType = new Gen.ConnectorType
    val cFormat = new Gen.ConnectorFormat
    cFormat.setConnectorFormat(cdr.connectorType.connectorFormat.toString)
    cType.setConnectorFormat(cFormat)
    val cStandard = new Gen.ConnectorStandard
    cStandard.setConnectorStandard(cdr.connectorType.connectorStandard.toString)
    cType.setConnectorStandard(cStandard)
    cdrInfo.setConnectorType(cType)
    cdrInfo.setContractId(cdr.contractId)
    ifNonEmptyThen(cdr.houseNumber)(cdrInfo.setHouseNumber)
    ifNonEmptyThen(cdr.zipCode)(cdrInfo.setZipCode)
    ifNonEmptyThen(cdr.city)(cdrInfo.setCity)
    cdrInfo.setCountry(cdr.country)
    cdr.duration.map(d => cdrInfo.setDuration(toOchp(d)))
    cdrInfo.setEmtId(EmtIdConverter.toOchp(cdr.emtId))
    cdrInfo.setStartDateTime(WithOffset.toOchp(startDateTime))
    cdrInfo.setEndDateTime(WithOffset.toOchp(endDateTime))
    cdrInfo.setEvseId(cdr.evseId)

    ifNonEmptyThen(cdr.liveAuthId)(cdrInfo.setLiveAuthId)
    cdrInfo.setMaxSocketPower(cdr.maxSocketPower)
    ifNonEmptyThen(cdr.meterId)(cdrInfo.setMeterId)
    ifNonEmptyThen(cdr.productType)(cdrInfo.setProductType)

    val cdrStatus = new Gen.CdrStatusType
    cdrStatus.setCdrStatusType(cdr.status.toString)
    cdrInfo.setStatus(cdrStatus)
    cdrInfo.getChargingPeriods.addAll(
      cdr.chargingPeriods.map {chargePeriodToGenCp} asJavaCollection)
    cdrInfo
  }

  private def chargePeriodToGenCp(gcp: CdrPeriod): Gen.CdrPeriodType = {
    val period1 = new Gen.CdrPeriodType
    period1.setStartDateTime(WithOffset.toOchp(gcp.startDateTime))
    period1.setEndDateTime(WithOffset.toOchp(gcp.endDateTime))
    val billingItem = new Gen.BillingItemType
    billingItem.setBillingItemType(gcp.billingItem.toString)
    period1.setBillingItem(billingItem)
    period1.setBillingValue(gcp.billingValue)
    period1.setCurrency(gcp.currency)
    period1.setItemPrice(gcp.itemPrice)
    gcp.periodCost.foreach {period1.setPeriodCost(_)}
    period1
  }

  private def toDateTimeZone(tz: String): Try[Option[DateTimeZone]] =
    safeRead(tz)(DateTimeZone.forID)

  implicit def cpInfoToChargePoint(genCp: Gen.ChargePointInfo): Option[ChargePoint] = {
    val cp = for {
      openingHours <- toHoursOption(genCp.getOperatingTimes)
      accessHours <- toHoursOption(genCp.getAccessTimes)
      timeZone <- toDateTimeZone(genCp.getTimeZone)

      chargePoint <- Try(ChargePoint(
        evseId = EvseId(genCp.getEvseId),
        locationId = genCp.getLocationId,
        timestamp = Option(genCp.getTimestamp).map(Utc.fromOchp),
        locationName = genCp.getLocationName,
        locationNameLang = genCp.getLocationNameLang,
        images = genCp.getImages.asScala.toList map {genImage => EvseImageUrl(
          uri = genImage.getUri,
          thumbUri = toNonEmptyOption(genImage.getThumbUri),
          clazz = ImageClass.withName(genImage.getClazz),
          `type` = genImage.getType,
          width = Option(genImage.getWidth),
          height = Option(genImage.getHeight)
        )},
        relatedResources =
          genCp.getRelatedResource.asScala.toList.map(RelatedResourceConverter.fromOchp),
        address = CpAddress(
          houseNumber = toNonEmptyOption(genCp.getHouseNumber),
          address =  genCp.getAddress,
          city = genCp.getCity,
          zipCode = genCp.getZipCode,
          country = genCp.getCountry
        ),
        chargePointLocation = GeoPointConverter.fromOchp(genCp.getChargePointLocation),
        relatedLocations =
          genCp.getRelatedLocation.asScala.toList.map(AdditionalGeoPointConverter.fromOchp),
        timeZone = timeZone,
        category = toNonEmptyOption(genCp.getCategory),
        operatingTimes = openingHours,
        accessTimes = accessHours,
        status = toChargePointStatusOption(genCp.getStatus),
        statusSchedule =
          genCp.getStatusSchedule.asScala.toList.map(ChargePointScheduleConverter.fromOchp),
        telephoneNumber = toNonEmptyOption(genCp.getTelephoneNumber),
        location = GeneralLocation.withName(genCp.getLocation.getGeneralLocationType),
        floorLevel = toNonEmptyOption(genCp.getFloorLevel),
        parkingSlotNumber = toNonEmptyOption(genCp.getParkingSlotNumber),
        parkingRestriction = genCp.getParkingRestriction.asScala.toList map {pr =>
          ParkingRestriction.withName(pr.getParkingRestrictionType)},
        authMethods = genCp.getAuthMethods.asScala.toList map {am =>
          AuthMethod.withName(am.getAuthMethodType)},
        connectors = genCp.getConnectors.asScala.toList map {con =>
          Connector(
            connectorStandard = ConnectorStandard.withName(
              con.getConnectorStandard.getConnectorStandard),
            connectorFormat = ConnectorFormat.withName(
              con.getConnectorFormat.getConnectorFormat))},
        ratings = Option(genCp.getRatings).map(RatingsConverter.fromOchp),
        userInterfaceLang = genCp.getUserInterfaceLang.asScala.toList
      ))
    } yield chargePoint

    cp match {
      case Success(x) => Some(x)
      case Failure(e) => logger.error("Charge point conversion failure", e); None
    }
  }

  private def imagesToGenImages(image: EvseImageUrl): Gen.EvseImageUrlType  = {
    val iut = new Gen.EvseImageUrlType
    iut.setClazz(image.clazz.toString)
    image.height foreach iut.setHeight
    image.width foreach iut.setWidth
    image.thumbUri foreach iut.setThumbUri
    iut.setType(image.`type`)
    iut.setUri(image.uri)
    iut
  }

  private[ochp] def hoursOptionToHoursType(maybeHours: Option[Hours]): Gen.HoursType = {
    def regHoursToRegHoursType(regHours: RegularHours): Gen.RegularHoursType = {
      val regularHoursType = new Gen.RegularHoursType
      regularHoursType.setWeekday(regHours.weekday)
      regularHoursType.setPeriodBegin(regHours.periodBegin.toString)
      regularHoursType.setPeriodEnd(regHours.periodEnd.toString)
      regularHoursType
    }

    def excPeriodToExcPeriodType(ep: ExceptionalPeriod): Gen.ExceptionalPeriodType = {
      val ept = new Gen.ExceptionalPeriodType
      ept.setPeriodBegin(Utc.toOchp(ep.periodBegin))
      ept.setPeriodEnd(Utc.toOchp(ep.periodEnd))
      ept
    }

    maybeHours.map { hours =>
      val hoursType = new Gen.HoursType

      hours.regularHoursOrTwentyFourSeven.fold(
        regHours =>
          hoursType.getRegularHours.addAll(
            regHours.map(regHoursToRegHoursType).asJavaCollection),
        twentyFourSeven =>
          hoursType.setTwentyfourseven(twentyFourSeven))
      hoursType.getExceptionalOpenings.addAll(
        hours.exceptionalOpenings map excPeriodToExcPeriodType asJavaCollection)
      hoursType.getExceptionalClosings.addAll(
        hours.exceptionalClosings map excPeriodToExcPeriodType asJavaCollection)

      hoursType
    }.getOrElse(null)
  }

  private def parkRestrToGenParkRestr(pRestr: ParkingRestriction.Value): Gen.ParkingRestrictionType = {
    val prt = new Gen.ParkingRestrictionType
    prt.setParkingRestrictionType(pRestr.toString)
    prt
  }

  private def authMethodToGenAuthMethod(authMethod: AuthMethod.Value): Gen.AuthMethodType = {
    val amt = new Gen.AuthMethodType
    amt.setAuthMethodType(authMethod.toString)
    amt
  }

  private def connToGenConn(connector: Connector): Gen.ConnectorType = {
    val ct = new Gen.ConnectorType
    val cs = new Gen.ConnectorStandard
    val cf = new Gen.ConnectorFormat
    cs.setConnectorStandard(connector.connectorStandard.toString)
    ct.setConnectorStandard(cs)
    cf.setConnectorFormat(connector.connectorFormat.toString)
    ct.setConnectorFormat(cf)
    ct
  }

  implicit def chargePointToCpInfo(cp: ChargePoint): Gen.ChargePointInfo = {
    val cpi = new Gen.ChargePointInfo
    cpi.setEvseId(cp.evseId.value)
    cpi.setLocationId(cp.locationId)
    cp.timestamp foreach {t =>
      cpi.setTimestamp(Utc.toOchp(t))}
    cpi.setLocationName(cp.locationName)
    cpi.setLocationNameLang(cp.locationNameLang)
    cpi.getImages.addAll(cp.images.map {imagesToGenImages} asJavaCollection)
    cpi.getRelatedResource.addAll(
      cp.relatedResources.map(RelatedResourceConverter.toOchp).asJavaCollection)
    cp.address.houseNumber foreach {hn => cpi.setHouseNumber(hn)}
    cpi.setAddress(cp.address.address)
    cpi.setZipCode(cp.address.zipCode)
    cpi.setCity(cp.address.city)
    cpi.setCountry(cp.address.country)
    cpi.setChargePointLocation(GeoPointConverter.toOchp(cp.chargePointLocation))
    cpi.getRelatedLocation.addAll(
      cp.relatedLocations.map(AdditionalGeoPointConverter.toOchp).asJavaCollection)
    cp.timeZone.map(tz => cpi.setTimeZone(tz.toString))
    cp.category.map(cpi.setCategory)
    cpi.setOperatingTimes(hoursOptionToHoursType(cp.operatingTimes))
    cpi.setAccessTimes(hoursOptionToHoursType(cp.accessTimes))
    cp.status.foreach { st =>
      val status = new Gen.ChargePointStatusType
      status.setChargePointStatusType(st.toString)
      cpi.setStatus(status)
    }
    cpi.getStatusSchedule.addAll(cp.statusSchedule.map(ChargePointScheduleConverter.toOchp).asJavaCollection)
    cp.telephoneNumber foreach cpi.setTelephoneNumber
    cpi.setLocation(new Gen.GeneralLocationType {
      setGeneralLocationType(cp.location.toString)
    })
    cp.floorLevel foreach cpi.setFloorLevel
    cp.parkingSlotNumber foreach cpi.setParkingSlotNumber
    cpi.getParkingRestriction.addAll(cp.parkingRestriction.map {parkRestrToGenParkRestr} asJavaCollection)
    cpi.getAuthMethods.addAll(cp.authMethods.map {authMethodToGenAuthMethod} asJavaCollection)
    cpi.getConnectors.addAll(cp.connectors.map {connToGenConn} asJavaCollection)
    cp.ratings.foreach(r => cpi.setRatings(RatingsConverter.toOchp(r)))
    cpi.getUserInterfaceLang.addAll(cp.userInterfaceLang asJavaCollection)
    cpi
  }

  implicit def toEvseStatus(s: Gen.EvseStatusType): Option[EvseStatus] = Try {
    EvseStatus(
      evseId = EvseId(s.getEvseId),
      majorStatus = EvseStatusMajor.findByName(s.getMajor).getOrElse(EvseStatusMajor.unknown),
      minorStatus = Option(s.getMinor).flatMap(EvseStatusMinor.findByName))
  } match {
    case Success(x) => Some(x)
    case Failure(e) => logger.error("Evse status conversion failure", e); None
  }

}
