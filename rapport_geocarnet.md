# Rapport PlaceTag

## Liste des fonctions implementees

- Localisation temps reel avec `FusedLocationProviderClient`.
- Affichage de la position sur OpenStreetMap avec osmdroid.
- Affichage latitude, longitude et adresse via `Geocoder`.
- Prise de photo et sauvegarde dans le dossier prive de l'application.
- Sauvegarde locale des lieux dans SQLite avec `SQLiteOpenHelper`.
- Lecture, modification, suppression, recherche et filtrage des lieux locaux.
- Synchronisation avec une base MySQL via une API PHP.
- Interface ergonomique avec toolbar et navigation drawer.
- Adaptation portrait/paysage avec layouts dedies.
- Traduction francais/anglais via ressources Android.

## Liste des fonctions non implementees

- Upload du fichier photo vers le serveur distant: seule la reference locale de la photo est synchronisee.
- Authentification utilisateur.
- Suppression distante appelee depuis l'application.

## Ameliorations possibles

- Ajouter un vrai upload multipart des photos.
- Ajouter une authentification et un compte utilisateur.
- Ajouter une vue detail separee avec itineraire.
- Ajouter une synchronisation automatique en arriere-plan.
- Remplacer le filtrage simple par des categories de lieux.

## Architecture, implementation et captures

L'application est organisee en quatre parties principales:

- `MainActivity`: interface, navigation, carte OpenStreetMap, permissions, photo, liste et synchronisation.
- `model/Place.java`: modele de donnees principal.
- `data/PlaceDbHelper.java` et `data/PlaceDao.java`: stockage local SQLite.
- `remote/PlaceRemoteDataSource.java`: appels HTTP JSON vers l'API PHP/MySQL.

La table SQLite locale `places` contient l'identifiant local, l'identifiant distant optionnel, le titre, la description, la latitude, la longitude, l'adresse, le chemin photo, la date et le statut de synchronisation.

Captures a ajouter:

- Ecran carte en portrait.
- Ecran nouveau lieu avec photo.
- Liste des lieux.
- Ecran synchronisation.
- Ecran paysage.
