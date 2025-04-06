package services

import slick.jdbc.H2Profile.api._
import scala.concurrent.Await
import scala.concurrent.duration._
import model.Country

object ReportsService {
  import DatabaseService._

  // Report 1: Top 10 countries by number of airports.
  def top10CountriesByAirports(): Seq[(String, Int)] = {
    val joinQuery = for {
      (apt, c) <- airports join countries on (_.isoCountry === _.code)
    } yield (c.name, apt.id)
    val groupQuery = joinQuery.groupBy(_._1).map { case (name, group) => (name, group.length) }
    val sorted = groupQuery.sortBy(_._2.desc).take(10)
    Await.result(db.run(sorted.result), Duration.Inf)
  }

  // Report 2: Bottom 10 countries by number of airports.
  def bottom10CountriesByAirports(): Seq[(String, Int)] = {
    val joinQuery = for {
      (apt, c) <- airports join countries on (_.isoCountry === _.code)
    } yield (c.name, apt.id)
    val groupQuery = joinQuery.groupBy(_._1).map { case (name, group) => (name, group.length) }
    val sorted = groupQuery.sortBy(_._2.asc).take(10)
    Await.result(db.run(sorted.result), Duration.Inf)
  }

  // Report 3: Runway surfaces per country.
  def runwaySurfacesPerCountry(): Seq[(String, Seq[String])] = {
    val query = for {
      ((apt, rw), c) <- airports join runways on (_.id === _.airportRef) join countries on (_._1.isoCountry === _.code)
    } yield (c.name, rw.surface)
    val results: Seq[(String, Option[String])] = Await.result(db.run(query.result), Duration.Inf)
    results.groupBy(_._1).map { case (name, list) =>
      (name, list.flatMap(_._2).distinct)
    }.toSeq
  }

  // Report 4: Top 10 most common runway LE Identifiers.
  def top10RunwayLeIdent(): Seq[(String, Int)] = {
    val query = runways.filter(_.leIdent.isDefined).map(_.leIdent)
    val identifiers: Seq[String] = Await.result(db.run(query.result), Duration.Inf).flatten
    identifiers.groupBy(identity).map { case (id, seq) => (id, seq.size) }
      .toSeq.sortBy(-_._2).take(10)
  }

  // Report 5: Average runway length per country.
  def averageRunwayLengthPerCountry(): Seq[(String, Double)] = {
    val query = for {
      ((apt, rw), c) <- airports join runways on (_.id === _.airportRef) join countries on (_._1.isoCountry === _.code)
      if rw.lengthFt.isDefined
    } yield (c.name, rw.lengthFt.get)
    val grouped = query.groupBy(_._1).map { case (name, group) => (name, group.map(_._2).avg) }
    Await.result(db.run(grouped.result), Duration.Inf).flatMap {
      case (name, Some(avg)) => Some((name, avg))
      case _ => None
    }
  }

  // Report 6: Top 10 airports with most runways.
  def top10AirportsByRunways(): Seq[(String, String, Int)] = {
    val query = for {
      (apt, rw) <- airports join runways on (_.id === _.airportRef)
    } yield (apt.name, apt.ident, rw.id)
    val grouped = query.groupBy { case (name, ident, _) => (name, ident) }
      .map { case ((name, ident), group) => (name, ident, group.length) }
      .sortBy(_._3.desc).take(10)
    Await.result(db.run(grouped.result), Duration.Inf)
  }

  // Report 7: Number of airports by airport type.
  def airportsCountByType(): Seq[(String, Int)] = {
    val query = airports.groupBy(_.airportType).map { case (t, group) => (t, group.length) }
    Await.result(db.run(query.result), Duration.Inf)
  }

  // Report 8: Countries with no runways.
  def countriesWithNoRunways(): Seq[(String, String)] = {
    val countriesWithRW: Set[String] = Await.result(
      db.run((for {
        (apt, rw) <- airports join runways on (_.id === _.airportRef)
        c <- countries if apt.isoCountry === c.code
      } yield c.code).distinct.result), Duration.Inf
    ).toSet
    val allCountries = Await.result(db.run(countries.result), Duration.Inf)
    allCountries.filter(c => !countriesWithRW.contains(c.code)).map(c => (c.name, c.code))
  }

  // Report 9: Top 10 countries by total number of runways.
  def top10CountriesByRunways(): Seq[(String, Int)] = {
    val query = for {
      (apt, rw) <- airports join runways on (_.id === _.airportRef)
      c <- countries if apt.isoCountry === c.code
    } yield (c.name, rw.id)
    val grouped = query.groupBy(_._1).map { case (name, group) => (name, group.length) }
    val sorted = grouped.sortBy(_._2.desc).take(10)
    Await.result(db.run(sorted.result), Duration.Inf)
  }
}