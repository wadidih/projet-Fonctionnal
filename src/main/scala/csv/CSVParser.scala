package csv

object CSVParser {
  // A pure function that splits a CSV line into fields, removing enclosing quotes.
  def splitLine(line: CharSequence): List[String] = {
    val regex = """,(?=(?:[^"]*"[^"]*")*[^"]*$)"""
    val pattern = java.util.regex.Pattern.compile(regex)
    pattern.split(line.toString, -1).toList.map(_.trim.stripPrefix("\"").stripSuffix("\""))
  }
}