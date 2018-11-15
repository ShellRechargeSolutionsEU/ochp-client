package com.newmotion.ochp
package client

import converters.Converters._
import converters.CDRConverter
import converters.DateTimeConverters._
import java.util.{HashMap => JMap}
import javax.xml.namespace.QName
import javax.xml.ws.Service
import javax.xml.ws.soap.SOAPBinding
import javax.security.auth.callback.{Callback, CallbackHandler}
import api.{CDR, ChargePoint, EvseStatus, ChargeToken}
import eu.ochp._1._
import eu.ochp._1_3.{OCHP13, OCHP13Live}
import org.apache.cxf.endpoint.Endpoint
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.interceptor.{LoggingInInterceptor, LoggingOutInterceptor}
import org.apache.cxf.staxutils.StaxUtils
import org.apache.cxf.transport.http.HTTPConduit
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor
import org.apache.wss4j.common.{ConfigurationConstants, WSS4JConstants}
import org.apache.wss4j.common.ext.WSPasswordCallback
import org.joda.time.DateTime
import scala.collection.JavaConverters._
import scala.language.postfixOps
import scala.util.Try


/**
 * @param cxfClient The SOAP client generated by CXF
 */
class OchpClient(cxfClient: OCHP13) {

  def setRoamingAuthorisationList(info: Seq[ChargeToken]): Result[ChargeToken] = {
    val req = new SetRoamingAuthorisationListRequest()
    req.getRoamingAuthorisationInfoArray.addAll(info.map(implicitly[RoamingAuthorisationInfo](_)).asJava)
    val resp = cxfClient.setRoamingAuthorisationList(req)
    Result(resp.getResult.getResultCode.getResultCode, resp.getResult.getResultDescription,
      resp.getRefusedRoamingAuthorisationInfo.asScala.toList.map(implicitly[ChargeToken](_)))
  }

  def roamingAuthorisationList() = {
    val resp = cxfClient.getRoamingAuthorisationList(
      new GetRoamingAuthorisationListRequest)
    resp.getRoamingAuthorisationInfoArray.asScala.toList.map(implicitly[ChargeToken](_))
  }

  def setRoamingAuthorisationListUpdate(info: Seq[ChargeToken]): Result[ChargeToken] = {
    val req = new UpdateRoamingAuthorisationListRequest()
    require(info.nonEmpty, "need at least one ChargeToken to send!")
    req.getRoamingAuthorisationInfoArray.addAll(info.map(implicitly[RoamingAuthorisationInfo](_)).asJava)
    val resp = cxfClient.updateRoamingAuthorisationList(req)
    Result(resp.getResult.getResultCode.getResultCode, resp.getResult.getResultDescription,
        resp.getRefusedRoamingAuthorisationInfo.asScala.toList.map(implicitly[ChargeToken](_)))
  }

  def roamingAuthorisationListUpdate(lastUpdate: DateTime) = {
    val req = new GetRoamingAuthorisationListUpdatesRequest
    req.setLastUpdate(Utc.toOchp(lastUpdate))
    val resp = cxfClient.getRoamingAuthorisationListUpdates( req )
    resp.getRoamingAuthorisationInfo.asScala.toList.map(implicitly[ChargeToken](_))
  }

  def getCdrs() = {
    val resp: GetCDRsResponse = cxfClient.getCDRs(
      new GetCDRsRequest)
    resp.getCdrInfoArray.asScala.toList.flatMap(CDRConverter.fromOchp)
  }

  def addCdrs(cdrs: Seq[CDR]) = {
    val req: AddCDRsRequest = new AddCDRsRequest()
    req.getCdrInfoArray.addAll(cdrs.map(implicitly[CDRInfo](_)).asJava)
    val resp = cxfClient.addCDRs(req)
    Result[CDR](resp.getResult.getResultCode.getResultCode, resp.getResult.getResultDescription,
      resp.getImplausibleCdrsArray.asScala.toList.flatMap(CDRConverter.fromOchp))
  }

  def confirmCdrs(approvedCdrs: Seq[CDR], declinedCdrs: Seq[CDR]): Result[Nothing] = {
    val req = new ConfirmCDRsRequest()
    req.getApproved.addAll(approvedCdrs.map(implicitly[CDRInfo](_)).asJava)
    req.getDeclined.addAll(declinedCdrs.map(implicitly[CDRInfo](_)).asJava)
    val resp = cxfClient.confirmCDRs(req)
    Result(resp.getResult.getResultCode.getResultCode, resp.getResult.getResultDescription, Nil)
  }

  def setChargePointList(info: Seq[ChargePoint]): Result[ChargePoint] = {
    val req = new SetChargePointListRequest()
    req.getChargePointInfoArray.addAll(info.map(implicitly[ChargePointInfo](_)).asJava)
    val resp = cxfClient.setChargepointList(req)
    Result[ChargePoint](resp.getResult.getResultCode.getResultCode, resp.getResult.getResultDescription,
        resp.getRefusedChargePointInfo.asScala.toList.flatMap(implicitly[Option[ChargePoint]](_)))
  }

  def chargePointList(): Result[ChargePoint] = {
    val resp = cxfClient.getChargePointList(
      new GetChargePointListRequest)

    Result[ChargePoint](
      resp.getResult.getResultCode.getResultCode,
      resp.getResult.getResultDescription,
      resp.getChargePointInfoArray.asScala.toList.flatMap(implicitly[Option[ChargePoint]](_)))
  }

  def setChargePointListUpdate(info: Seq[ChargePoint]): Result[ChargePoint] = {
    val req = new UpdateChargePointListRequest()
    req.getChargePointInfoArray.addAll(info.map(implicitly[ChargePointInfo](_)).asJava)
    val resp = cxfClient.updateChargePointList(req)
    Result(resp.getResult.getResultCode.getResultCode, resp.getResult.getResultDescription,
      resp.getRefusedChargePointInfo.asScala.toList.flatMap(implicitly[Option[ChargePoint]](_)))
  }

  def chargePointListUpdate(lastUpdate: DateTime): Result[ChargePoint] = {
    val req = new GetChargePointListUpdatesRequest
    req.setLastUpdate(Utc.toOchp(lastUpdate))
    val resp = cxfClient.getChargePointListUpdates(req)

    Result(
      resp.getResult.getResultCode.getResultCode,
      resp.getResult.getResultDescription,
      resp.getChargePointInfoArray.asScala.toList.flatMap(implicitly[Option[ChargePoint]](_)))
  }

}

class OchpLiveClient(cxfLiveClient: OCHP13Live) {
  /**
   * Only implements setting the timeToLive for the whole list,
   * not individually.
   *
   * @param evseStats
   * @param timeToLive
   */
  def updateStatus(evseStats: List[EvseStatus], timeToLive: Option[DateTime] = None) = {
    def toStatusType(evseStat: EvseStatus): EvseStatusType = {
      val est = new EvseStatusType
      est.setEvseId(evseStat.evseId.value)
      est.setMajor(evseStat.majorStatus.toString)
      evseStat.minorStatus foreach {minStat=> est.setMinor(minStat.toString)}
      est
    }
    val req  = new UpdateStatusRequest
    req.getEvse.addAll(evseStats map toStatusType asJavaCollection)
    timeToLive foreach {ttl=>req.setTtl(Utc.toOchp(ttl))}

    val resp = cxfLiveClient.updateStatus(req)
    Result(resp.getResult.getResultCode.getResultCode, resp.getResult.getResultDescription, List())
  }

  def getStatus(since: Option[DateTime] = None) = {
    val r   = new GetStatusRequest
    val req = since.fold(r){x => r.setStartDateTime(Utc.toOchp(x));r}
    val resp = cxfLiveClient.getStatus(req)

    resp.getEvse.asScala.toList.flatMap(implicitly[Option[EvseStatus]](_))
  }

}

case class Result[A](status: ResultCode.Value, description: String, items: List[A])

object Result  {
  def apply[A](code: String, desc: String, items: List[A]) = {
    new Result(
      Try(ResultCode.withName(code.toLowerCase)).getOrElse(ResultCode.unknown),
      desc, items
    )
  }
}

object ResultCode extends Enumeration {
  type ResultCode = Value
  val ok = Value("ok")
  val unknown = Value("unknown")
  val partly = Value("partly")
  val notAuthorized = Value("not-authorized")
  val invalidId = Value("invalid-id")
  val server = Value("server")
  val format = Value("format")
}

object OchpClient {

  def createCxfClient(conf: OchpConfig): OchpClient = {
    require(conf.wsUri != "", "need endpoint uri!")
    val (servicePort: QName, service: Service) = createClient(conf, conf.wsUri)
    val cxfClient = addConfig(
      addWssHeaders(conf, service.getPort(servicePort, classOf[OCHP13])),
      conf)
    new OchpClient(cxfClient)
  }

  def createCxfLiveClient(conf: OchpConfig): OchpLiveClient = {
    require(conf.liveWsUri != "", "need live endpoint uri!")
    val (servicePort: QName, service: Service) = createClient(conf, conf.liveWsUri)
    val cxfLiveClient = addConfig(
      addWssHeaders(conf, service.getPort(servicePort, classOf[OCHP13Live])),
      conf)
    new OchpLiveClient(cxfLiveClient)
  }

  private def createClient(conf: OchpConfig, endpoint_address: String): (QName, Service) = {
    val servicePort: QName = new QName(endpoint_address, "service port")
    val service: Service = Service.create(servicePort)
    service.addPort(servicePort, SOAPBinding.SOAP11HTTP_BINDING, endpoint_address)
    (servicePort, service)
  }

  private def addWssHeaders[T](conf: OchpConfig, port: T): T = {
    val cxfEndpoint: Endpoint = ClientProxy.getClient(port).getEndpoint
    val pwCallbackHandler = new CallbackHandler {
      def handle(cs: Array[Callback]) =
        cs(0).asInstanceOf[WSPasswordCallback].setPassword(conf.password)
    }

    cxfEndpoint.put(StaxUtils.MAX_CHILD_ELEMENTS, int2Integer(conf.maxTokensInMessage))

    val outProps = new JMap[String, Object] {
      put(ConfigurationConstants.ACTION, ConfigurationConstants.USERNAME_TOKEN)
      put(ConfigurationConstants.USER, conf.user)
      put(ConfigurationConstants.PASSWORD_TYPE, WSS4JConstants.PW_TEXT)
      put(ConfigurationConstants.PW_CALLBACK_REF, pwCallbackHandler)
    }
    val wssOut = new WSS4JOutInterceptor(outProps)

    cxfEndpoint.getOutInterceptors.add(wssOut)
    cxfEndpoint.getOutInterceptors.add(new LoggingOutInterceptor)
    cxfEndpoint.getInInterceptors.add(new LoggingInInterceptor)
    port
  }

  /** @see org.apache.cxf.transports.http.configuration.HTTPClientPolicy#getReceiveTimeout
    */
  private val InfinityInHttpClientPolicyWay = 0

  private def addConfig[T](port: T, conf: OchpConfig): T = {
    val client = ClientProxy.getClient(port)
    val http: HTTPConduit = client.getConduit.asInstanceOf[HTTPConduit]
    val httpClientPolicy = new HTTPClientPolicy()
    httpClientPolicy.setAllowChunking(false)
    httpClientPolicy.setReceiveTimeout(
      if (conf.requestTimeout.isFinite) conf.requestTimeout.toMillis
      else InfinityInHttpClientPolicyWay
    )
    http.setClient(httpClientPolicy)
    port
  }

}