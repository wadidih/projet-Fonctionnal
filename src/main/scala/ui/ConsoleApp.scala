package ui

import scala.io.StdIn
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import services.DatabaseService._
import services.ReportsService._

object ConsoleApp extends App {
  // Setup and load data
  println("Initializing database...")
  setupSchema()
  loadData("data/countries.csv", "data/airports.csv", "data/runways.csv")
  println("Data loaded.")

  println("Select option: ")
  println("1. Query")
  println("2. Reports")
  StdIn.readLine() match {
    case "1" =>
      println("Enter country name or code:")
      val input = StdIn.readLine().trim
      Future {
        val results = findCountries(input)
        if (results.isEmpty) s"No match found for '$input'"
        else results.map { country =>
          val header = s"Found country: ${country.name} [${country.code}]"
          val details = getAirportsAndRunways(country.code).map { case (apt, rws) =>
            val aptInfo = s"  Airport: ${apt.name} [${apt.ident}]"
            val rwInfo = if (rws.isEmpty) "    No runways found" else
              rws.map { rw =>
                s"    Runway ID: ${rw.id}, Surface: ${rw.surface.getOrElse("Unknown")}, LE Ident: ${rw.leIdent.getOrElse("Unknown")}"
              }.mkString("\n")
            s"$aptInfo\n$rwInfo"
          }.mkString("\n")
          s"$header\n$details"
        }.mkString("\n\n")
      }.onComplete {
        case scala.util.Success(text) => println(text)
        case scala.util.Failure(err)  => println(s"Error: ${err.getMessage}")
      }
      Thread.sleep(3000) // Wait briefly for the future to complete (for demo purposes)
    case "2" =>
      println("Select report:")
      println("1. Top 10 Countries by Airports")
      println("2. Bottom 10 Countries by Airports")
      println("3. Runway Surfaces per Country")
      println("4. Top 10 Most Common Runway LE Identifiers")
      println("5. Average Runway Length per Country")
      println("6. Top 10 Airports with Most Runways")
      println("7. Number of Airports by Airport Type")
      println("8. Countries with No Runways")
      println("9. Top 10 Countries by Total Number of Runways")
      StdIn.readLine() match {
        case "1" => Future { println(top10CountriesByAirports().map { case (n, c) => s"$n: $c airports" }.mkString("\n")) }
        case "2" => Future { println(bottom10CountriesByAirports().map { case (n, c) => s"$n: $c airports" }.mkString("\n")) }
        case "3" => Future { println(runwaySurfacesPerCountry().map { case (c, s) => s"$c: ${s.mkString(", ")}" }.mkString("\n")) }
        case "4" => Future { println(top10RunwayLeIdent().map { case (i, c) => s"$i: $c" }.mkString("\n")) }
        case "5" => Future { println(averageRunwayLengthPerCountry().map { case (c, avg) => s"$c: $avg ft" }.mkString("\n")) }
        case "6" => Future { println(top10AirportsByRunways().map { case (n, id, cnt) => s"$n [$id]: $cnt runways" }.mkString("\n")) }
        case "7" => Future { println(airportsCountByType().map { case (t, cnt) => s"$t: $cnt airports" }.mkString("\n")) }
        case "8" => Future { println(countriesWithNoRunways().map { case (n, code) => s"$n [$code]" }.mkString("\n")) }
        case "9" => Future { println(top10CountriesByRunways().map { case (c, cnt) => s"$c: $cnt runways" }.mkString("\n")) }
        case other => println(s"Invalid report selection: $other")
      }
      Thread.sleep(3000)
    case other =>
      println(s"Invalid option: $other")
  }
}