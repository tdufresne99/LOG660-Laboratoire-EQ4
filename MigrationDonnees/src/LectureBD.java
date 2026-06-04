package src;

import java.io.FileInputStream;
import java.io.IOException;

import java.io.InputStream;

import java.sql.DriverManager;

import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class LectureBD {
   private java.sql.Connection connexion = null;

   // Objets PreparedStatement réutilisables pour éviter de les recréer à chaque ligne
   private java.sql.PreparedStatement pstmtPersonne = null;
   private java.sql.PreparedStatement pstmtFilm = null;
   private java.sql.PreparedStatement pstmtClient = null;
   private java.sql.PreparedStatement pstmtExemplaire = null;

   // PreparedStatement pour les tables de liaison / sous-tables
   private java.sql.PreparedStatement pstmtFilmGenre = null;
   private java.sql.PreparedStatement pstmtFilmPays = null;
   private java.sql.PreparedStatement pstmtBandeAnnonce = null;
   private java.sql.PreparedStatement pstmtRole = null;
   private java.sql.PreparedStatement pstmtScenariste = null;
   private java.sql.PreparedStatement pstmtAdresse = null;
   private java.sql.PreparedStatement pstmtCarte = null;

   // Compteurs pour gérer la taille des batchs
   private int batchSize = 1000;
   private int countPersonnes = 0;
   private int countFilms = 0;
   private int countClients = 0;

   // Caches en mémoire pour éviter les doublons et violations de clés uniques (Pays, Langue, Genre, Ville)
   private java.util.HashSet<String> paysInseres = new java.util.HashSet<>();
   private java.util.HashSet<String> languesInseres = new java.util.HashSet<>();
   private java.util.HashSet<String> genresInseres = new java.util.HashSet<>();
   private java.util.HashSet<String> villesInseres = new java.util.HashSet<>();
   private java.util.Set<Integer> realisateursInseres = new java.util.HashSet<>();
   private java.util.HashSet<String> adressesInseres = new java.util.HashSet<>();
   private java.util.HashSet<String> annoncesGlobales = new java.util.HashSet<>();
   private java.util.HashSet<String> rolesGlobales = new java.util.HashSet<>();

   public class Role {
      public Role(int i, String n, String p) {
         id = i;
         nom = n;
         personnage = p;
      }
      protected int id;
      protected String nom;
      protected String personnage;
   }

   public LectureBD() {
      connectionBD();
   }


   public void lecturePersonnes(String nomFichier){
      try {
         XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
         XmlPullParser parser = factory.newPullParser();

         InputStream is = new FileInputStream(nomFichier);
         parser.setInput(is, null);

         int eventType = parser.getEventType();

         String tag = null,
                 nom = null,
                 anniversaire = null,
                 lieu = null,
                 photo = null,
                 bio = null;

         int id = -1;

         while (eventType != XmlPullParser.END_DOCUMENT)
         {
            if(eventType == XmlPullParser.START_TAG)
            {
               tag = parser.getName();

               if (tag.equals("personne") && parser.getAttributeCount() == 1)
                  id = Integer.parseInt(parser.getAttributeValue(0));
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
               tag = null;

               if (parser.getName().equals("personne") && id >= 0)
               {
                  insertionPersonne(id,nom,anniversaire,lieu,photo,bio);

                  id = -1;
                  nom = null;
                  anniversaire = null;
                  lieu = null;
                  photo = null;
                  bio = null;
               }
            }
            else if (eventType == XmlPullParser.TEXT && id >= 0)
            {
               if (tag != null)
               {
                  if (tag.equals("nom"))
                     nom = parser.getText();
                  else if (tag.equals("anniversaire"))
                     anniversaire = parser.getText();
                  else if (tag.equals("lieu"))
                     lieu = parser.getText();
                  else if (tag.equals("photo"))
                     photo = parser.getText();
                  else if (tag.equals("bio"))
                     bio = parser.getText();
               }
            }

            eventType = parser.next();
         }
      }
      catch (XmlPullParserException e) {
         System.out.println(e);
      }
      catch (IOException e) {
         System.out.println("IOException while parsing " + nomFichier);
      }
   }

   public void lectureFilms(String nomFichier){
      try {
         XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
         XmlPullParser parser = factory.newPullParser();

         InputStream is = new FileInputStream(nomFichier);
         parser.setInput(is, null);

         int eventType = parser.getEventType();

         String tag = null,
                 titre = null,
                 langue = null,
                 poster = null,
                 roleNom = null,
                 rolePersonnage = null,
                 realisateurNom = null,
                 resume = null;

         ArrayList<String> pays = new ArrayList<String>();
         ArrayList<String> genres = new ArrayList<String>();
         ArrayList<String> scenaristes = new ArrayList<String>();
         ArrayList<Role> roles = new ArrayList<Role>();
         ArrayList<String> annonces = new ArrayList<String>();

         int id = -1,
                 annee = -1,
                 duree = -1,
                 roleId = -1,
                 realisateurId = -1;

         while (eventType != XmlPullParser.END_DOCUMENT)
         {
            if(eventType == XmlPullParser.START_TAG)
            {
               tag = parser.getName();

               if (tag.equals("film") && parser.getAttributeCount() == 1)
                  id = Integer.parseInt(parser.getAttributeValue(0));
               else if (tag.equals("realisateur") && parser.getAttributeCount() == 1)
                  realisateurId = Integer.parseInt(parser.getAttributeValue(0));
               else if (tag.equals("acteur") && parser.getAttributeCount() == 1)
                  roleId = Integer.parseInt(parser.getAttributeValue(0));
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
               tag = null;

               if (parser.getName().equals("film") && id >= 0)
               {
                  insertionFilm(id,titre,annee,pays,langue,
                          duree,resume,genres,realisateurNom,
                          realisateurId, scenaristes,
                          roles,poster,annonces);

                  id = -1;
                  annee = -1;
                  duree = -1;
                  titre = null;
                  langue = null;
                  poster = null;
                  resume = null;
                  realisateurNom = null;
                  roleNom = null;
                  rolePersonnage = null;
                  realisateurId = -1;
                  roleId = -1;

                  genres.clear();
                  scenaristes.clear();
                  roles.clear();
                  annonces.clear();
                  pays.clear();
               }
               if (parser.getName().equals("role") && roleId >= 0)
               {
                  roles.add(new Role(roleId, roleNom, rolePersonnage));
                  roleId = -1;
                  roleNom = null;
                  rolePersonnage = null;
               }
            }
            else if (eventType == XmlPullParser.TEXT && id >= 0)
            {
               if (tag != null)
               {
                  if (tag.equals("titre"))
                     titre = parser.getText();
                  else if (tag.equals("annee"))
                     annee = Integer.parseInt(parser.getText());
                  else if (tag.equals("pays"))
                     pays.add(parser.getText());
                  else if (tag.equals("langue"))
                     langue = parser.getText();
                  else if (tag.equals("duree"))
                     duree = Integer.parseInt(parser.getText());
                  else if (tag.equals("resume"))
                     resume = parser.getText();
                  else if (tag.equals("genre"))
                     genres.add(parser.getText());
                  else if (tag.equals("realisateur"))
                     realisateurNom = parser.getText();
                  else if (tag.equals("scenariste"))
                     scenaristes.add(parser.getText());
                  else if (tag.equals("acteur"))
                     roleNom = parser.getText();
                  else if (tag.equals("personnage"))
                     rolePersonnage = parser.getText();
                  else if (tag.equals("poster"))
                     poster = parser.getText();
                  else if (tag.equals("annonce"))
                     annonces.add(parser.getText());
               }
            }

            eventType = parser.next();
         }
      }
      catch (XmlPullParserException e) {
         System.out.println(e);
      }
      catch (IOException e) {
         System.out.println("IOException while parsing " + nomFichier);
      }
   }

   public void lectureClients(String nomFichier){
      try {
         XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
         XmlPullParser parser = factory.newPullParser();

         InputStream is = new FileInputStream(nomFichier);
         parser.setInput(is, null);

         int eventType = parser.getEventType();

         String tag = null,
                 nomFamille = null,
                 prenom = null,
                 courriel = null,
                 tel = null,
                 anniv = null,
                 adresse = null,
                 ville = null,
                 province = null,
                 codePostal = null,
                 carte = null,
                 noCarte = null,
                 motDePasse = null,
                 forfait = null;

         int id = -1,
                 expMois = -1,
                 expAnnee = -1;

         while (eventType != XmlPullParser.END_DOCUMENT)
         {
            if(eventType == XmlPullParser.START_TAG)
            {
               tag = parser.getName();

               if (tag.equals("client") && parser.getAttributeCount() == 1)
                  id = Integer.parseInt(parser.getAttributeValue(0));
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
               tag = null;

               if (parser.getName().equals("client") && id >= 0)
               {
                  insertionClient(id,nomFamille,prenom,courriel,tel,
                          anniv,adresse,ville,province,
                          codePostal,carte,noCarte,
                          expMois,expAnnee,motDePasse,forfait);

                  nomFamille = null;
                  prenom = null;
                  courriel = null;
                  tel = null;
                  anniv = null;
                  adresse = null;
                  ville = null;
                  province = null;
                  codePostal = null;
                  carte = null;
                  noCarte = null;
                  motDePasse = null;
                  forfait = null;

                  id = -1;
                  expMois = -1;
                  expAnnee = -1;
               }
            }
            else if (eventType == XmlPullParser.TEXT && id >= 0)
            {
               if (tag != null)
               {
                  if (tag.equals("nom-famille"))
                     nomFamille = parser.getText();
                  else if (tag.equals("prenom"))
                     prenom = parser.getText();
                  else if (tag.equals("courriel"))
                     courriel = parser.getText();
                  else if (tag.equals("tel"))
                     tel = parser.getText();
                  else if (tag.equals("anniversaire"))
                     anniv = parser.getText();
                  else if (tag.equals("adresse"))
                     adresse = parser.getText();
                  else if (tag.equals("ville"))
                     ville = parser.getText();
                  else if (tag.equals("province"))
                     province = parser.getText();
                  else if (tag.equals("code-postal"))
                     codePostal = parser.getText();
                  else if (tag.equals("carte"))
                     carte = parser.getText();
                  else if (tag.equals("no"))
                     noCarte = parser.getText();
                  else if (tag.equals("exp-mois"))
                     expMois = Integer.parseInt(parser.getText());
                  else if (tag.equals("exp-annee"))
                     expAnnee = Integer.parseInt(parser.getText());
                  else if (tag.equals("mot-de-passe"))
                     motDePasse = parser.getText();
                  else if (tag.equals("forfait"))
                     forfait = parser.getText();
               }
            }

            eventType = parser.next();
         }
         try {
            if (pstmtAdresse != null) pstmtAdresse.executeBatch();
            if (pstmtClient != null) pstmtClient.executeBatch();
            if (pstmtCarte != null) pstmtCarte.executeBatch();
         } catch (java.sql.SQLException e) {
            System.err.println("Erreur lors de l'exécution du dernier batch : " + e.getMessage());
         }
      }
      catch (XmlPullParserException e) {
         System.out.println(e);
      }
      catch (IOException e) {
         System.out.println("IOException while parsing " + nomFichier);
      }
   }

   private void insertionPersonne(int id, String nom, String anniv, String lieu, String photo, String bio) {
      try {
         // Séparer le prénom et le nom (ex: "John Wick" -> prenom: "John", nom: "Wick")
         String prenom = "";
         String nomFamille = "";
         if (nom != null && !nom.trim().isEmpty()) {
            String[] parties = nom.split(" ", 2);
            prenom = parties[0];
            if (parties.length > 1) {
               nomFamille = parties[1];
            } else {
               nomFamille = prenom; // Valeur de repli si pas de nom de famille
            }
         }

         // Conversion de la date
         java.sql.Date dateNaissance = null;
         if (anniv != null && !anniv.trim().isEmpty()) {
            dateNaissance = java.sql.Date.valueOf(anniv); // Format attendu: AAAA-MM-JJ
         }

         // Remplissage du PreparedStatement
         pstmtPersonne.setInt(1, id);
         pstmtPersonne.setString(2, nomFamille);
         pstmtPersonne.setString(3, prenom);

         if (dateNaissance != null) {
            pstmtPersonne.setDate(4, dateNaissance);
         } else {
            pstmtPersonne.setNull(4, java.sql.Types.DATE);
         }

         // On s'assure que le champ photo ne dépasse pas la limite VARCHAR2
         String photoSecurisee = photo;
         if (photoSecurisee != null && photoSecurisee.length() > 3000) {
            photoSecurisee = photoSecurisee.substring(0, 3000);
         }
         pstmtPersonne.setString(5, photoSecurisee);

         if (bio != null && !bio.isEmpty()) {
            java.sql.Clob clob = connexion.createClob();
            clob.setString(1, bio);
            pstmtPersonne.setClob(6, clob);
         } else {
            pstmtPersonne.setNull(6, java.sql.Types.CLOB);
         }

         // Ajout au batch
         pstmtPersonne.addBatch();
         countPersonnes++;

         // Exécution périodique du batch pour libérer la mémoire vive
         if (countPersonnes % batchSize == 0) {
            pstmtPersonne.executeBatch();
         }
      } catch (Exception e) {
         System.err.println("Erreur lors de la préparation de la personne ID " + id + " : " + e.getMessage());
      }
   }

   private void insertionFilm(int id, String titre, int annee,
                              ArrayList<String> pays, String langue, int duree, String resume,
                              ArrayList<String> genres, String realisateurNom, int realisateurId,
                              ArrayList<String> scenaristes,
                              ArrayList<Role> roles, String poster,
                              ArrayList<String> annonces) {
      try {
         String paysPrincipal = (pays != null && !pays.isEmpty()) ? pays.get(0) : "Inconnu";

         try (java.sql.Statement stmtRef = connexion.createStatement()) {

            // 1. Insertion des Langues
            if (langue != null && !langue.trim().isEmpty() && !languesInseres.contains(langue)) {
               try {
                  stmtRef.executeUpdate("INSERT INTO Langue (nomLangue) VALUES ('" + langue.replace("'", "''") + "')");
                  languesInseres.add(langue);
               } catch (java.sql.SQLException ignored) {}
            }

            // 2. Insertion du Pays
            if (!paysInseres.contains(paysPrincipal)) {
               try {
                  stmtRef.executeUpdate("INSERT INTO Pays (nomPays) VALUES ('" + paysPrincipal.replace("'", "''") + "')");
                  paysInseres.add(paysPrincipal);
               } catch (java.sql.SQLException ignored) {}
            }
         }

         // Insertion du réalisateur avec correspondance des identifiants
         if (!realisateursInseres.contains(realisateurId)) {
            try (java.sql.Statement stmtReal = connexion.createStatement()) {
               stmtReal.executeUpdate("INSERT INTO Realisateur (realisateurId, personneCinemaId) " +
                       "VALUES (" + realisateurId + ", " + realisateurId + ")");
               realisateursInseres.add(realisateurId);
            } catch (java.sql.SQLException e) {
               if (e.getErrorCode() == 1) {
                  realisateursInseres.add(realisateurId);
               } else if (e.getErrorCode() == 2291) {
                  try (java.sql.Statement stmtSecours = connexion.createStatement()) {
                     stmtSecours.executeUpdate("INSERT INTO PERSONNECINEMA (personneCinemaId, nom, prenom) " +
                             "VALUES (" + realisateurId + ", 'Inconnu', 'Réalisateur')");

                     stmtSecours.executeUpdate("INSERT INTO Realisateur (realisateurId, personneCinemaId) " +
                             "VALUES (" + realisateurId + ", " + realisateurId + ")");

                     realisateursInseres.add(realisateurId);
                  } catch (java.sql.SQLException ex) {
                     System.err.println("Impossible de générer le réalisateur de secours ID " + realisateurId + " : " + ex.getMessage());
                  }
               } else {
                  System.err.println("Erreur imprévue pour le réalisateur ID " + realisateurId + " : " + e.getMessage());
               }
            }
         }

         // 3. Remplissage de la table principale Film
         pstmtFilm.setInt(1, id);
         pstmtFilm.setString(2, titre);
         pstmtFilm.setInt(3, annee);
         pstmtFilm.setInt(4, duree);
         pstmtFilm.setString(5, resume != null ? resume : "");
         if (poster != null && !poster.trim().isEmpty()) {
            pstmtFilm.setString(6, poster);
         } else {
            pstmtFilm.setNull(6, java.sql.Types.VARCHAR);
         }
         pstmtFilm.setInt(7, realisateurId);
         if (langue != null && !langue.trim().isEmpty()) {
            pstmtFilm.setString(8, langue);
         } else {
            pstmtFilm.setNull(8, java.sql.Types.VARCHAR);
         }
         pstmtFilm.setString(9, paysPrincipal);
         pstmtFilm.addBatch();

         // 4. Gestion des tables de liaison à cardinalité multiple
         for (String g : genres) {
            if (!genresInseres.contains(g)) {
               try (java.sql.Statement s = connexion.createStatement()) {
                  s.executeUpdate("INSERT INTO Genre (nomGenre) VALUES ('" + g.replace("'", "''") + "')");
                  genresInseres.add(g);
               } catch (java.sql.SQLException ignored) {}
            }
            pstmtFilmGenre.setInt(1, id);
            pstmtFilmGenre.setString(2, g);
            pstmtFilmGenre.addBatch();
         }

         for (String p : pays) {
            if (!paysInseres.contains(p)) {
               try (java.sql.Statement s = connexion.createStatement()) {
                  s.executeUpdate("INSERT INTO Pays (nomPays) VALUES ('" + p.replace("'", "''") + "')");
                  paysInseres.add(p);
               } catch (java.sql.SQLException ignored) {}
            }
            pstmtFilmPays.setInt(1, id);
            pstmtFilmPays.setString(2, p);
            pstmtFilmPays.addBatch();
         }

         // Gestion GLOBALE des bandes-annonces pour éviter erreur (URL partagée entre plusieurs films)
         for (String urlAnnonce : annonces) {
            if (urlAnnonce != null && !urlAnnonce.trim().isEmpty()) {
               // On vérifie si cette URL exacte a déjà été insérée dans la base de données
               if (!annoncesGlobales.contains(urlAnnonce)) {
                  pstmtBandeAnnonce.setString(1, urlAnnonce);
                  pstmtBandeAnnonce.setInt(2, id);
                  pstmtBandeAnnonce.addBatch();
                  annoncesGlobales.add(urlAnnonce);
               }
            }
         }

         // Gestion GLOBALE des rôles pour éviter erreur (Rôle répété dans le XML)
         for (Role r : roles) {
            // La clé unique d'un rôle est la combinaison : Acteur + Film + Personnage
            String cleRole = r.id + "_" + id + "_" + r.personnage;
            if (!rolesGlobales.contains(cleRole)) {
               pstmtRole.setInt(1, r.id);
               pstmtRole.setInt(2, id);
               pstmtRole.setString(3, r.personnage);
               pstmtRole.addBatch();
               rolesGlobales.add(cleRole);
            }
         }

         // 4.5. Insertion des Scénaristes
         for (String nomScenariste : scenaristes) {
            try (java.sql.PreparedStatement psScen = connexion.prepareStatement(
                    "INSERT INTO Scenariste (personneCinemaId, filmId) " +
                            "SELECT personneCinemaId, ? FROM PersonneCinema WHERE nom = ? OR prenom || ' ' || nom = ? FETCH FIRST 1 ROWS ONLY"
            )) {
               psScen.setInt(1, id);
               psScen.setString(2, nomScenariste);
               psScen.setString(3, nomScenariste);
               psScen.executeUpdate();
            } catch (java.sql.SQLException ignored) {}
         }

         // 5. Génération aléatoire des copies (Exemplaires)
         java.util.Random rand = new java.util.Random();
         int nbCopies = rand.nextInt(100) + 1;
         for (int i = 1; i <= nbCopies; i++) {
            pstmtExemplaire.setInt(1, id);
            pstmtExemplaire.setString(2, "COPY-" + id + "-" + i);
            pstmtExemplaire.addBatch();
         }

         countFilms++;
         if (countFilms % batchSize == 0) {
            pstmtFilm.executeBatch();
            pstmtFilmGenre.executeBatch();
            pstmtFilmPays.executeBatch();
            pstmtBandeAnnonce.executeBatch();
            pstmtRole.executeBatch();
            pstmtExemplaire.executeBatch();
         }

      } catch (Exception e) {
         System.err.println("Erreur lors de la préparation du film ID " + id + " : " + e.getMessage());
      }
   }

   private void insertionClient(int id, String nomFamille, String prenom,
                                String courriel, String tel, String anniv,
                                String adresse, String ville, String province,
                                String codePostal, String carte, String noCarte,
                                int expMois, int expAnnee, String motDePasse,
                                String forfait) {
      try {
         // 1. Extraction du numéro civique et du nom de rue depuis l'adresse
         int numeroCivique = 0;
         String nomRue = adresse;
         if (adresse != null && !adresse.trim().isEmpty()) {
            String[] partiesAdresse = adresse.split(" ", 2);
            try {
               numeroCivique = Integer.parseInt(partiesAdresse[0]);
               if (partiesAdresse.length > 1) nomRue = partiesAdresse[1];
            } catch (NumberFormatException e) {
               numeroCivique = 0; // Valeur par défaut si non numérique au début
            }
         }

         // 2. Gestion de la Ville et de son pays parent
         String paysDefaut = "Canada";
         if (!paysInseres.contains(paysDefaut)) {
            try (java.sql.Statement s = connexion.createStatement()) {
               s.executeUpdate("INSERT INTO Pays (nomPays) VALUES ('" + paysDefaut + "')");
               paysInseres.add(paysDefaut);
            } catch (java.sql.SQLException ignored) {}
         }

         // Enregistrement de la ville si elle n'est pas connue
         String cleVille = (ville + "_" + province).toLowerCase();
         if (!villesInseres.contains(cleVille)) {
            try (java.sql.PreparedStatement psV = connexion.prepareStatement(
                    "INSERT INTO Ville (nom, provinceEtat, nomPays) VALUES (?, ?, ?)"
            )) {
               psV.setString(1, ville);
               psV.setString(2, province);
               psV.setString(3, paysDefaut);
               psV.executeUpdate();
               villesInseres.add(cleVille);
            } catch (java.sql.SQLException ignored) {}
         }

         // 3. Enregistrement de l'adresse avec gestion de cache et Batch
         String cleAdresse = (numeroCivique + "_" + nomRue + "_" + codePostal).toLowerCase();
         if (!adressesInseres.contains(cleAdresse)) {
            pstmtAdresse.setInt(1, numeroCivique);
            pstmtAdresse.setString(2, nomRue);
            pstmtAdresse.setString(3, codePostal);
            pstmtAdresse.setString(4, ville);
            pstmtAdresse.setString(5, province);
            pstmtAdresse.addBatch();

            adressesInseres.add(cleAdresse);
         }

         // 4. Insertion dans la table Utilisateurs
         String dateAnnivStr = (anniv != null && !anniv.trim().isEmpty()) ? anniv : "1970-01-01";

         pstmtClient.setInt(1, id);
         pstmtClient.setString(2, nomFamille);
         pstmtClient.setString(3, prenom);
         pstmtClient.setDate(4, java.sql.Date.valueOf(dateAnnivStr));
         pstmtClient.setString(5, courriel);
         pstmtClient.setString(6, tel);
         pstmtClient.setString(7, motDePasse);
         pstmtClient.setString(8, forfait);

         pstmtClient.setInt(9, numeroCivique);
         pstmtClient.setString(10, nomRue);
         pstmtClient.setString(11, codePostal);

         pstmtClient.addBatch();

         // 5. Insertion associée dans CarteDeCredit
         int anneeSecurise = (expAnnee > 0) ? expAnnee : 2099;
         int moisSecurise = (expMois >= 1 && expMois <= 12) ? expMois : 12; // Si mois=13, on le ramène à 12

         String dateExpStr = String.format("%04d-%02d-01", anneeSecurise, moisSecurise);
         java.sql.Date dateExpiration = java.sql.Date.valueOf(dateExpStr);

         pstmtCarte.setString(1, noCarte);
         pstmtCarte.setInt(2, id);
         pstmtCarte.setDate(3, dateExpiration);
         pstmtCarte.setString(4, "000");
         pstmtCarte.setString(5, prenom + " " + nomFamille);
         pstmtCarte.setString(6, carte != null ? carte.toUpperCase() : "INCONNU");
         pstmtCarte.addBatch();

         countClients++;
         if (countClients % batchSize == 0) {
            pstmtAdresse.executeBatch();
            pstmtClient.executeBatch();
            pstmtCarte.executeBatch();
         }

      } catch (Exception e) {
         System.err.println("Erreur lors de la préparation du client ID " + id + " : " + e.getMessage());
      }
   }

   private void connectionBD() {
      try {
         // Accès au serveur
         String url = "jdbc:oracle:thin:@//bdlog660.ens.ad.etsmtl.ca:1521/ORCLPDB1";
         String user = "EQUIPE204";
         String pass = "ELrS7IIQ";

         this.connexion = DriverManager.getConnection(url, user, pass);
         // Désactiver l'auto-commit pour permettre le batching de masse et accélérer l'exécution
         this.connexion.setAutoCommit(false);
         System.out.println("Connexion établie avec succès. Préparation des requêtes...");

         // Initialisation des requêtes préparées
         pstmtPersonne = connexion.prepareStatement(
                 "INSERT INTO PersonneCinema (personneCinemaId, nom, prenom, dateDeNaissance, photoUrl, biographie) VALUES (?, ?, ?, ?, ?, ?)"
         );

         pstmtFilm = connexion.prepareStatement(
                 "INSERT INTO Film (filmId, titre, anneeSortie, duree, description, affiche, realisateurId, nomLangue, nomPays) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
         );

         pstmtFilmGenre = connexion.prepareStatement("INSERT INTO FilmGenre (filmId, nomGenre) VALUES (?, ?)");
         pstmtFilmPays = connexion.prepareStatement("INSERT INTO FilmPays (filmId, nomPays) VALUES (?, ?)");
         pstmtBandeAnnonce = connexion.prepareStatement("INSERT INTO BandeAnnonce (url, filmId) VALUES (?, ?)");
         pstmtRole = connexion.prepareStatement("INSERT INTO Role (personneCinemaId, filmId, personnage) VALUES (?, ?, ?)");
         pstmtScenariste = connexion.prepareStatement("INSERT INTO Scenariste (personneCinemaId, filmId) VALUES (?, ?)");

         pstmtClient = connexion.prepareStatement(
                 "INSERT INTO Utilisateurs (utilisateurID, nom, prenom, dateDeNaissance, courriel, telephone, motDePasse, categorieUtilisateur, codeForfait, adresseId) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, 'CLIENT', ?, " +
                         "(SELECT adresseId FROM Adresse WHERE numeroCivique = ? AND rue = ? AND codePostal = ? FETCH FIRST 1 ROWS ONLY))"
         );

         pstmtCarte = connexion.prepareStatement(
                 "INSERT INTO CarteDeCredit (numero, utilisateurID, dateExpiration, codeSecurite, nomSurCarte, type) VALUES (?, ?, ?, ?, ?, ?)"
         );

         pstmtExemplaire = connexion.prepareStatement(
                 "INSERT INTO Exemplaire (filmId, numero, statut) VALUES (?, ?, 'DISPONIBLE')"
         );

         pstmtAdresse = connexion.prepareStatement(
                 "INSERT INTO Adresse (numeroCivique, rue, codePostal, villeId) " +
                         "VALUES (?, ?, ?, (SELECT villeId FROM Ville WHERE nom=? AND provinceEtat=? FETCH FIRST 1 ROWS ONLY))"
         );

      } catch (java.sql.SQLException e) {
         System.err.println("Erreur d'initialisation JDBC : " + e.getMessage());
      }
   }

   public static void main(String[] args) {
      if (args.length < 3) {
         System.out.println("Usage: java LectureBD personnes.xml films.xml clients.xml");
         return;
      }

      long startTime = System.currentTimeMillis();
      LectureBD lecture = new LectureBD();

      // ÉTAPE 1 : IMPORTATION DES PERSONNES
      try {
         System.out.println("Début de l'importation des Personnes...");
         lecture.lecturePersonnes(args[0]);
         lecture.pstmtPersonne.executeBatch();
         lecture.connexion.commit();
         System.out.println("Importation des Personnes terminée avec succès.");
      } catch (java.sql.SQLException e) {
         System.err.println("Erreur SQL fatale durant l'importation des PERSONNES. Annulation de cette étape...");
         try { lecture.connexion.rollback(); } catch (java.sql.SQLException ex) { ex.printStackTrace(); }
         e.printStackTrace();
      }

      // ÉTAPE 2 : IMPORTATION DES FILMS
      try {
         System.out.println("Début de l'importation des Films et Exemplaires...");
         lecture.lectureFilms(args[1]);

         // On vide tous les restes des batches de films
         lecture.pstmtFilm.executeBatch();
         lecture.pstmtFilmGenre.executeBatch();
         lecture.pstmtFilmPays.executeBatch();
         lecture.pstmtBandeAnnonce.executeBatch();
         lecture.pstmtRole.executeBatch();
         lecture.pstmtExemplaire.executeBatch();

         lecture.connexion.commit(); // Valide les films
         System.out.println("Importation des Films et Exemplaires terminée avec succès.");
      } catch (java.sql.SQLException e) {
         System.err.println("Erreur SQL fatale durant l'importation des FILMS. Annulation de cette étape...");
         try { lecture.connexion.rollback(); } catch (java.sql.SQLException ex) { ex.printStackTrace(); }
         e.printStackTrace();
      }

      // ÉTAPE 3 : IMPORTATION DES CLIENTS
      try {
         System.out.println("Début de l'importation des Clients...");
         lecture.lectureClients(args[2]);
         lecture.pstmtAdresse.executeBatch();
         lecture.pstmtClient.executeBatch();
         lecture.pstmtCarte.executeBatch();
         lecture.connexion.commit(); // Valide les clients
         System.out.println("Importation des Clients terminée avec succès.");
      } catch (java.sql.SQLException e) {
         System.err.println("Erreur SQL fatale durant l'importation des CLIENTS. Annulation de cette étape...");
         try { lecture.connexion.rollback(); } catch (java.sql.SQLException ex) { ex.printStackTrace(); }
         e.printStackTrace();
      } finally {
         // Fermeture de toutes les ressources ouvertes
         System.out.println("Fermeture des ressources de connexion...");
         try {
            if (lecture.pstmtPersonne != null) lecture.pstmtPersonne.close();
            if (lecture.pstmtFilm != null) lecture.pstmtFilm.close();
            if (lecture.pstmtFilmGenre != null) lecture.pstmtFilmGenre.close();
            if (lecture.pstmtFilmPays != null) lecture.pstmtFilmPays.close();
            if (lecture.pstmtBandeAnnonce != null) lecture.pstmtBandeAnnonce.close();
            if (lecture.pstmtRole != null) lecture.pstmtRole.close();
            if (lecture.pstmtExemplaire != null) lecture.pstmtExemplaire.close();
            if (lecture.pstmtClient != null) lecture.pstmtClient.close();
            if (lecture.pstmtCarte != null) lecture.pstmtCarte.close();
            if (lecture.pstmtAdresse != null) lecture.pstmtAdresse.close();
            if (lecture.connexion != null && !lecture.connexion.isClosed()) lecture.connexion.close();
         } catch (java.sql.SQLException e) {
            e.printStackTrace();
         }
      }

      long endTime = System.currentTimeMillis();
      System.out.println("Migration globale terminée. Temps d'exécution total : " + ((endTime - startTime) / 1000) + " secondes.");
   }
}
