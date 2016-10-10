package com.thenewmotion.ochp

import com.thenewmotion.ochp.api.AuthMethod
import com.thenewmotion.ochp.client.OchpClient
import org.joda.time.DateTime

object CallProd extends App {

  val conf = OchpConfig(
    wsUri = "https://echs.e-clearing.net/service/ochp/v1.3",
    liveWsUri = "https://echs.e-clearing.net/service/ochp/v1.3/live",
    user = "thenewmotion.backend.0",
    password = "f9DzMXH47D",
    requestTimeout = 600
  )

  import sext._
  val client = OchpClient.createCxfClient(conf)

  val since = DateTime.now.minusHours(24)
//  println(s"Chargepoints since: ${since.toLocalDateTime}")
//  val cpList = client.chargePointListUpdate(since)
//  val tokenList = client.roamingAuthorisationList()
    val cpList = client.chargePointList()
    println(s"cps: ${cpList.items.size}")
    println(s"OCHP direct auth: ${cpList.items.count(_.authMethods.contains(AuthMethod.OchpDirectAuth))}")
    println(s"Operator auth: ${cpList.items.count(_.authMethods.contains(AuthMethod.OperatorAuth))}")
//  cpList.items.map{cp =>
//    println("\nChargePoint:")
//    println(cp.valueTreeString)}
//  tokenList.map{t =>
//    println("\ntoken:")
//    println(t.valueTreeString)}
}
