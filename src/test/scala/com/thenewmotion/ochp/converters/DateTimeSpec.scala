package com.thenewmotion.ochp
package converters

import converters.DateTimeConverters._
import eu.ochp._1.{DateTimeType, LocalDateTimeType}
import org.joda.time.{DateTime, DateTimeZone}


class DateTimeSpec extends Spec {
  "DateTimeType is intended only for UTC date times" >> {
    import Utc._

    val now = "2016-09-05T15:30:16Z"
    val dtt = new DateTimeType {
      setDateTime(now)
    }

    toOchp(fromOchp(dtt)).getDateTime mustEqual dtt.getDateTime
  }

  "LocalDateTimeType" >> {
    import WithOffset._

    "is intended only for date times with offset" >> {
      val ldt = new LocalDateTimeType {
        setLocalDateTime("2016-09-05T15:30:16+02:00")
      }

      toOchp(fromOchp(ldt)).getLocalDateTime mustEqual ldt.getLocalDateTime
    }

    "GMT date times must be printed with an offset as well!" >> {
      val dt = new DateTime(2016, 9, 5, 16, 10, 25).withZone(DateTimeZone.UTC)

      toOchp(dt).getLocalDateTime mustEqual "2016-09-05T14:10:25+00:00"
    }
  }

}