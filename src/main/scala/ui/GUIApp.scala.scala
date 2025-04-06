package gui

import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control.{Alert, Button, ComboBox, Label, Tab, TabPane, TextArea, TextField}
import scalafx.scene.layout.VBox
import scalafx.geometry.Insets
import scalafx.application.Platform
import services.DatabaseService  // Service de gestion de la DB, incluant les méthodes de chargement, recherche, etc.
import services.ReportsService   // Service de génération de rapports
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object GUIApp extends JFXApp3 {

  override def start(): Unit = {
    // Initialisation de la base de données et chargement des données CSV
    DatabaseService.setupSchema()
    DatabaseService.loadData("data/countries.csv", "data/airports.csv", "data/runways.csv")

    stage = new PrimaryStage {
      title = "Explorateur d'Aéroports et Pistes"
      scene = new Scene(900, 600) {

        // Onglet Recherche
        val ongletRecherche = new Tab {
          text = "Recherche"
          content = new VBox {
            spacing = 10
            padding = Insets(10)
            val champRecherche = new TextField { promptText = "Nom ou code du pays" }
            val zoneResultat = new TextArea {
              editable = false
              prefHeight = 400
              wrapText = true
            }
            val boutonRecherche = new Button("Lancer la recherche") {
              onAction = _ => {
                val requete = champRecherche.text.value.trim
                if (requete.isEmpty) {
                  zoneResultat.text = "Veuillez saisir un nom ou code de pays."
                } else {
                  Future {
                    // Recherche floue via DatabaseService
                    val paysTrouves = DatabaseService.findCountries(requete)
                    if (paysTrouves.isEmpty) s"Aucun pays trouvé pour '$requete'"
                    else {
                      paysTrouves.map { pays =>
                        val entete = s"Pays trouvé : ${pays.name} [${pays.code}]"
                        val details = DatabaseService.getAirportsAndRunways(pays.code).map { case (aero, pistes) =>
                          val infoAero = s"  Aéroport : ${aero.name} [${aero.ident}]"
                          val infoPistes = if (pistes.isEmpty) "    Aucune piste trouvée"
                          else pistes.map { piste =>
                            s"    Piste ID : ${piste.id}, Surface : ${piste.surface.getOrElse("Inconnue")}, LE Ident : ${piste.leIdent.getOrElse("Inconnu")}"
                          }.mkString("\n")
                          s"$infoAero\n$infoPistes"
                        }.mkString("\n")
                        s"$entete\n$details"
                      }.mkString("\n\n")
                    }
                  }.onComplete { res =>
                    Platform.runLater {
                      zoneResultat.text = res.getOrElse("Erreur lors de la recherche")
                    }
                  }
                }
              }
            }
            children = Seq(new Label("Saisir le nom ou code d'un pays :"), champRecherche, boutonRecherche, zoneResultat)
          }
        }

        // Onglet Rapports
        val ongletRapports = new Tab {
          text = "Rapports"
          content = new VBox {
            spacing = 10
            padding = Insets(10)
            val comboRapports = new ComboBox[String] {
              id = "comboRapports"
              items = scalafx.collections.ObservableBuffer(
                "Top 10 pays par nombre d'aéroports",
                "Bottom 10 pays par nombre d'aéroports",
                "Surfaces de pistes par pays",
                "Top 10 des LE Ident les plus fréquents",
                "Longueur moyenne des pistes par pays",
                "Top 10 des aéroports avec le plus de pistes",
                "Nombre d'aéroports par type",
                "Pays sans pistes",
                "Top 10 pays par nombre total de pistes"
              )
              promptText = "Choisir un rapport"
            }
            val zoneResultRapport = new TextArea {
              id = "zoneResultRapport"
              editable = false
              prefHeight = 400
              wrapText = true
            }
            val boutonRapport = new Button("Afficher le rapport") {
              onAction = _ => {
                val selection = comboRapports.value.value
                if (selection == null || selection.isEmpty)
                  zoneResultRapport.text = "Veuillez sélectionner un rapport."
                else {
                  Future {
                    selection match {
                      case "Top 10 pays par nombre d'aéroports" =>
                        ReportsService.top10CountriesByAirports().map { case (nom, nb) => s"$nom : $nb aéroports" }.mkString("\n")
                      case "Bottom 10 pays par nombre d'aéroports" =>
                        ReportsService.bottom10CountriesByAirports().map { case (nom, nb) => s"$nom : $nb aéroports" }.mkString("\n")
                      case "Surfaces de pistes par pays" =>
                        ReportsService.runwaySurfacesPerCountry().map { case (nom, surfaces) => s"$nom : ${surfaces.mkString(", ")}" }.mkString("\n")
                      case "Top 10 des LE Ident les plus fréquents" =>
                        ReportsService.top10RunwayLeIdent().map { case (ident, nb) => s"$ident : $nb" }.mkString("\n")
                      case "Longueur moyenne des pistes par pays" =>
                        ReportsService.averageRunwayLengthPerCountry().map { case (nom, moy) => s"$nom : $moy ft" }.mkString("\n")
                      case "Top 10 des aéroports avec le plus de pistes" =>
                        ReportsService.top10AirportsByRunways().map { case (nom, ident, nb) => s"$nom [$ident] : $nb pistes" }.mkString("\n")
                      case "Nombre d'aéroports par type" =>
                        ReportsService.airportsCountByType().map { case (typ, nb) => s"$typ : $nb aéroports" }.mkString("\n")
                      case "Pays sans pistes" =>
                        ReportsService.countriesWithNoRunways().map { case (nom, code) => s"$nom [$code]" }.mkString("\n")
                      case "Top 10 pays par nombre total de pistes" =>
                        ReportsService.top10CountriesByRunways().map { case (nom, nb) => s"$nom : $nb pistes" }.mkString("\n")
                      case _ =>
                        "Rapport inconnu."
                    }
                  }.onComplete { res =>
                    Platform.runLater {
                      zoneResultRapport.text = res.getOrElse("Erreur lors de l'affichage du rapport")
                    }
                  }
                }
              }
            }
            children = Seq(new Label("Sélectionnez un rapport :"), comboRapports, boutonRapport, zoneResultRapport)
          }
        }

        // Création du TabPane contenant les onglets Recherche et Rapports
        root = new TabPane {
          tabs = Seq(ongletRecherche, ongletRapports)
        }
      }
    }
  }
}