package com.newmotion.ochp
package cmdline

import com.newmotion.ochp.api.ChargeToken
import com.newmotion.ochp.client.OchpClient
import org.joda.time.format.DateTimeFormat

object Main extends App {

  if (args.length != 3) {
    System.err.println("Usage: ochp <URL> <username> <password>")
  } else {
    downloadTokens(serviceUri = args(0), user = args(1), password = args(2))
  }

  private def downloadTokens(serviceUri: String, user: String, password: String): Unit = {

    val ochp = new OchpService {
      val conf = OchpConfig(
        wsUri = serviceUri,
        user = user,
        password = password
      )

      def client = OchpClient.createCxfClient(conf)
    }

    val eclTokens = ochp.recvAllTokens()

    System.err.println(s"${eclTokens.size} downloaded from e-clearing")

    val cards = eclTokens.sortBy(t => t.contractId + "::" + t.emtId.tokenId)
    cards.foreach(c => println(csvLine(c)))
  }

  private def csvLine(tok: ChargeToken): String = {
    val cells: Seq[String] = Seq(
      tok.contractId,
      tok.emtId.tokenType.toString,
      tok.emtId.tokenSubType.fold("")(_.toString),
      tok.emtId.tokenId,
      tok.emtId.representation.toString,
      tok.printedNumber.getOrElse(""),
      DateTimeFormat.forPattern("YYYY-MM-dd").print(tok.expiryDate)
    )

    cells.map(quote).mkString(",")
  }

  private def quote(str: String): String =
    if (str.isEmpty) str else '"' + str + '"'
}
