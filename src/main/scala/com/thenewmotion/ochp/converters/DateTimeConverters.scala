package com.thenewmotion.ochp
package converters

import api.DateTimeNoMillis
import com.thenewmotion.time.Imports._
import eu.ochp._1.{DateTimeType, LocalDateTimeType}
import org.joda.time.format.DateTimeFormatterBuilder


trait DateTimeConverters {
  object WithOffset {
    private val formatter =
      new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
        .appendTimeZoneOffset(
          /* what to print when offset is zero = */ null,
          /* always use separators = */ true,
          /* minimum precision = */ 2,
          /* maximum precision = */ 2)
        .toFormatter

    def fromOchp(dateTime: LocalDateTimeType) =
      DateTimeNoMillis(dateTime.getLocalDateTime)

    def toOchp(dateTime: DateTime): LocalDateTimeType = {
      new LocalDateTimeType {
        setLocalDateTime(formatter.print(dateTime))
      }
    }
  }

  object Utc {
    def toOchp(date: DateTime): DateTimeType = {
      new DateTimeType {
        setDateTime(DateTimeNoMillis(date.withZone(DateTimeZone.UTC)).toString)
      }
    }

    def fromOchp(value: DateTimeType): DateTime =
      DateTimeNoMillis(value.getDateTime)
  }
}

object DateTimeConverters extends DateTimeConverters