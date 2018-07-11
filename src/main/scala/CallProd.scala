package com.thenewmotion.ochp

import com.newmotion.ochp.OchpConfig
import com.newmotion.ochp.api.AuthMethod
import com.newmotion.ochp.client.OchpClient
import org.joda.time.DateTime
import scala.concurrent.duration._

/**
  * doing `sbt run` will run this App
  * it will download all chargepoints
  * and dynamic data since the date specified below
  * and print the raw data received to the console.
  */
object CallProd extends App {

  val conf = OchpConfig(
    wsUri = "https://echs.e-clearing.net/service/ochp/v1.3",
    liveWsUri = "https://echs.e-clearing.net/live/ochp/v1.3",
    user = "thenewmotion.backend.0",
    password = "f9DzMXH47D",
    requestTimeout = 20.minute
  )
  val client = OchpClient.createCxfClient(conf)
  val liveClient = OchpClient.createCxfLiveClient(conf)

  val since = DateTime.now.minusHours(24)
//  println(s"Chargepoints since: ${since.toLocalDateTime}")
//  val cpList = client.chargePointListUpdate(since)
//  val tokenList = client.roamingAuthorisationList()
//  tokenList.map{t =>
//    println("\ntoken:")
//    println(t.valueTreeString)}

    val cpList = client.chargePointList()
//    println(s"cps: ${cpList.items.size}")
//    println(s"OCHP direct auth: ${cpList.items.count(_.authMethods.contains(AuthMethod.OchpDirectAuth))}")
//    println(s"Operator auth: ${cpList.items.count(_.authMethods.contains(AuthMethod.OperatorAuth))}")
//  cpList.items.map{cp =>
//    println("\nChargePoint:")
//    println(cp)}

  val statuses = liveClient.getStatus(Some(DateTime.parse("2018-07-01T08:00:00Z")))
//  println(statuses.mkString("\n"))


}
