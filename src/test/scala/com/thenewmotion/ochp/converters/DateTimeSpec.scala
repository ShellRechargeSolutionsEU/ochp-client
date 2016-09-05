package com.thenewmotion.ochp
package converters

import converters.DateTimeConverters._
import eu.ochp._1.{DateTimeType, LocalDateTimeType}
import org.joda.time.{DateTime, DateTimeZone}


class DateTimeSpec extends Spec {
  "DateTimeType is intended only for UTC datetimes" >> {
    import Utc._

    val now = "2016-09-05T15:30:16Z"
    val dtt = new DateTimeType {
      setDateTime(now)
    }

    toOchp(fromOchp(dtt)).getDateTime mustEqual dtt.getDateTime
  }

  "LocalDateTimeType" >> {
    import WithOffset._

    "is intended only for datetimes with offset" >> {
      val ldt = new LocalDateTimeType {
        setLocalDateTime("2016-09-05T15:30:16+02:00")
      }
      val res = toOchp(fromOchp(ldt)).getLocalDateTime

      new DateTime(res) mustEqual new DateTime(ldt.getLocalDateTime)
    }

    "requires GMT datetimes to be written as '+00:00', instead of 'Z'" >> {
      val dt = new DateTime(2016, 9, 5, 16, 10, 25, DateTimeZone.UTC)
      val res = toOchp(dt).getLocalDateTime

      new DateTime(res) mustEqual new DateTime("2016-09-05T16:10:25+00:00")

      res must contain("+00:00")
      res must not contain("Z")
    }
  }

}