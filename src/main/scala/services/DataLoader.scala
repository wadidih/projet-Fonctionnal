package services

import scala.io.Source
import model.{Country, Airport, Runway}

object DataLoader {
  def loadCountries(path: String): List[Country] =
    Source.fromFile(path).getLines().drop(1).toList.flatMap(Country.fromCSV)

  def loadAirports(path: String): List[Airport] =
    Source.fromFile(path).getLines().drop(1).toList.flatMap(Airport.fromCSV)

  def loadRunways(path: String): List[Runway] =
    Source.fromFile(path).getLines().drop(1).toList.flatMap(Runway.fromCSV)
}