package com.newmotion.ochp.api

import com.newmotion.ochp.api.CDR
import eu.ochp._1.Result

case class CdrsMappedResponse(result: Result, cdrInfoArray: List[CDR])