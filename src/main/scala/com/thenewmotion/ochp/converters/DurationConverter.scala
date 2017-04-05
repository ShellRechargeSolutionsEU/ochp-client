package com.thenewmotion.ochp
package converters

import com.github.nscala_time.time.Imports._
import org.joda.time.PeriodType

import scala.util.{Failure, Try}


trait DurationConverter {
  private val OchpFormat = """(\d{3}):(\d{2}):(\d{2})""".r

  def toOchp(d: Duration): String = {
    val p = d.toPeriod(PeriodType.time())

    val hours = "%03d".format(p.getHours)
    val minutes = "%02d".format(p.getMinutes)
    val seconds = "%02d".format(p.getSeconds)

    s"$hours:$minutes:$seconds"
  }

  def fromOchp(value: String): Try[Duration] = {
    value match {
      case OchpFormat(hours, minutes, seconds) =>
        for {
          hours <- Try(Duration.standardHours(hours.toLong))
          minutes <- Try(Duration.standardMinutes(minutes.toLong))
          seconds <- Try(Duration.standardSeconds(seconds.toLong))
        } yield seconds.plus(minutes).plus(hours)
      case _ => Failure(
        new IllegalArgumentException(
          s"Cannot parse $value as duration. Expected format is 000:00:00 (hours:minutes:seconds)"))
    }
  }
}

object DurationConverter extends DurationConverter
