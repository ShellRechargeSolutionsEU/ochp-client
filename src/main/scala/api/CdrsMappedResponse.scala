package com.newmotion.ochp.api

import com.newmotion.ochp.api.CDR
import com.newmotion.ochp.converters.CDRConverter
import eu.ochp._1.{GetCDRsResponse, Result}
import scala.collection.JavaConverters._


sealed trait CdrsMappedResponse

object CdrsMappedResponse {
  def apply(response:GetCDRsResponse) = {
    val result = response.getResult
    if (result.getResultCode.getResultCode == "ok") {
      SuccessfulCdrsResponse(response.getCdrInfoArray.asScala.toList.flatMap(CDRConverter.fromOchp))
    } else {
      FailedCdrsResponse(result.getResultCode.getResultCode,result.getResultDescription)
    }
  }
  case class SuccessfulCdrsResponse(cdrInfoArray: List[CDR]) extends CdrsMappedResponse
  case class FailedCdrsResponse(code: String, description: String) extends CdrsMappedResponse

}