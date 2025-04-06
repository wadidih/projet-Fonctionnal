package services

import model.{Country, Airport, Runway}
import slick.jdbc.H2Profile.api._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future

object DatabaseService {
  // Table definitions
  class CountriesTable(tag: Tag) extends Table[Country](tag, "COUNTRIES") {
    def id = column[Int]("ID", O.PrimaryKey)
    def code = column[String]("CODE")
    def name = column[String]("NAME")
    def continent = column[Option[String]]("CONTINENT")
    def wikiLink = column[Option[String]]("WIKI_LINK")
    def keywords = column[Option[String]]("KEYWORDS")
    def * = (id, code, name, continent, wikiLink, keywords) <> ((Country.apply _).tupled, Country.unapply)
  }
  val countries = TableQuery[CountriesTable]

  class AirportsTable(tag: Tag) extends Table[Airport](tag, "AIRPORTS") {
    def id = column[Int]("ID", O.PrimaryKey)
    def ident = column[String]("IDENT")
    def airportType = column[String]("TYPE")
    def name = column[String]("NAME")
    def latitude = column[Option[Double]]("LATITUDE")
    def longitude = column[Option[Double]]("LONGITUDE")
    def elevation = column[Option[Int]]("ELEVATION")
    def continent = column[Option[String]]("CONTINENT")
    def isoCountry = column[Option[String]]("ISO_COUNTRY")
    def isoRegion = column[Option[String]]("ISO_REGION")
    def municipality = column[Option[String]]("MUNICIPALITY")
    def scheduledService = column[Option[String]]("SCHEDULED_SERVICE")
    def gpsCode = column[Option[String]]("GPS_CODE")
    def iataCode = column[Option[String]]("IATA_CODE")
    def localCode = column[Option[String]]("LOCAL_CODE")
    def homeLink = column[Option[String]]("HOME_LINK")
    def wikiLink = column[Option[String]]("WIKI_LINK")
    def keywords = column[Option[String]]("KEYWORDS")
    def * = (id, ident, airportType, name, latitude, longitude, elevation,
      continent, isoCountry, isoRegion, municipality, scheduledService,
      gpsCode, iataCode, localCode, homeLink, wikiLink, keywords) <> ((Airport.apply _).tupled, Airport.unapply)
  }
  val airports = TableQuery[AirportsTable]

  class RunwaysTable(tag: Tag) extends Table[Runway](tag, "RUNWAYS") {
    def id = column[Int]("ID", O.PrimaryKey)
    def airportRef = column[Int]("AIRPORT_REF")
    def airportIdent = column[String]("AIRPORT_IDENT")
    def lengthFt = column[Option[Int]]("LENGTH_FT")
    def widthFt = column[Option[Int]]("WIDTH_FT")
    def surface = column[Option[String]]("SURFACE")
    def lighted = column[Option[Int]]("LIGHTED")
    def closed = column[Option[Int]]("CLOSED")
    def leIdent = column[Option[String]]("LE_IDENT")
    def * = (id, airportRef, airportIdent, lengthFt, widthFt, surface, lighted, closed, leIdent) <> ((Runway.apply _).tupled, Runway.unapply)
  }
  val runways = TableQuery[RunwaysTable]

  // Create an in-memory H2 database.
  val db = Database.forConfig("h2mem1")

  def setupSchema(): Unit = {
    val schema = countries.schema ++ airports.schema ++ runways.schema
    Await.result(db.run(schema.createIfNotExists), Duration.Inf)
  }

  // Insert data from CSV using DataLoader.
  def loadData(countryPath: String, airportPath: String, runwayPath: String): Unit = {
    val countriesData = DataLoader.loadCountries(countryPath)
    val airportsData = DataLoader.loadAirports(airportPath)
    val runwaysData = DataLoader.loadRunways(runwayPath)
    val actions = DBIO.seq(
      countries ++= countriesData,
      airports ++= airportsData,
      runways ++= runwaysData
    )
    Await.result(db.run(actions.transactionally), Duration.Inf)
  }

  // Fuzzy search for countries (by code or name containing query, case-insensitive)
  def findCountries(query: String): Seq[Country] = {
    val qLower = query.toLowerCase
    val q = countries.filter { c =>
      c.name.toLowerCase.like(s"%$qLower%") || c.code.toLowerCase === qLower
    }
    Await.result(db.run(q.result), Duration.Inf)
  }

  // Get airports and associated runways for a given country code.
  def getAirportsAndRunways(countryCode: String): Seq[(Airport, Seq[Runway])] = {
    val aptQuery = airports.filter(_.isoCountry === countryCode.toUpperCase)
    val aptList = Await.result(db.run(aptQuery.result), Duration.Inf)
    aptList.map { apt =>
      val rwQuery = runways.filter(_.airportRef === apt.id)
      val rwList = Await.result(db.run(rwQuery.result), Duration.Inf)
      (apt, rwList)
    }
  }
}