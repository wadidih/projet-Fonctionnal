package model

import csv.CSVParser

case class Country(id: Int, code: String, name: String, continent: Option[String], wikiLink: Option[String], keywords: Option[String])
object Country {
  // Expected CSV: id,code,name,continent,wikiLink,keywords
  def fromCSV(line: String): Option[Country] =
    CSVParser.splitLine(line) match {
      case idStr :: code :: name :: cont :: wiki :: key :: Nil =>
        idStr.toIntOption.map { id =>
          Country(id, code, name, nonEmpty(cont), nonEmpty(wiki), nonEmpty(key))
        }
      case _ => None
    }
  private def nonEmpty(s: String): Option[String] =
    if (s.trim.isEmpty) None else Some(s.trim)
}

case class Airport(id: Int, ident: String, airportType: String, name: String, latitude: Option[Double],
                   longitude: Option[Double], elevation: Option[Int], continent: Option[String],
                   isoCountry: Option[String], isoRegion: Option[String], municipality: Option[String],
                   scheduledService: Option[String], gpsCode: Option[String], iataCode: Option[String],
                   localCode: Option[String], homeLink: Option[String], wikiLink: Option[String],
                   keywords: Option[String])
object Airport {
  // Expected CSV with 18 columns
  def fromCSV(line: String): Option[Airport] =
    CSVParser.splitLine(line) match {
      case idStr :: ident :: aType :: name :: lat :: lon :: elev ::
        cont :: isoC :: isoR :: muni :: sched :: gps :: iata :: local ::
        home :: wiki :: key :: Nil =>
        for {
          id <- idStr.toIntOption
        } yield Airport(
          id, ident, aType, name,
          lat.toDoubleOption, lon.toDoubleOption, elev.toIntOption,
          nonEmpty(cont), nonEmpty(isoC), nonEmpty(isoR), nonEmpty(muni),
          nonEmpty(sched), nonEmpty(gps), nonEmpty(iata), nonEmpty(local),
          nonEmpty(home), nonEmpty(wiki), nonEmpty(key)
        )
      case _ => None
    }
  private def nonEmpty(s: String): Option[String] =
    if (s.trim.isEmpty) None else Some(s.trim)
}

case class Runway(id: Int, airportRef: Int, airportIdent: String, lengthFt: Option[Int],
                  widthFt: Option[Int], surface: Option[String], lighted: Option[Int],
                  closed: Option[Int], leIdent: Option[String])
object Runway {
  // Expected CSV: at least 9 columns
  def fromCSV(line: String): Option[Runway] =
    CSVParser.splitLine(line) match {
      case idStr :: aptRef :: aptIdent :: len :: wid :: surf :: light :: closed :: leId :: _ =>
        for {
          id <- idStr.toIntOption
          aptRefInt <- aptRef.toIntOption
        } yield Runway(id, aptRefInt, aptIdent, len.toIntOption, wid.toIntOption,
          nonEmpty(surf), light.toIntOption, closed.toIntOption, nonEmpty(leId))
      case _ => None
    }
  private def nonEmpty(s: String): Option[String] =
    if (s.trim.isEmpty) None else Some(s.trim)
}