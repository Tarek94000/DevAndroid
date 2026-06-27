# PlaceTag

PlaceTag est une application Android Java/XML pour enregistrer des lieux visites avec localisation temps reel, carte OpenStreetMap, adresse geocodee, photo locale, stockage SQLite et synchronisation MySQL via une API PHP.

## Lancer l'application

1. Ouvrir le dossier `PlaceTag` dans Android Studio.
2. Lancer l'application sur emulateur ou telephone.

La carte utilise OpenStreetMap avec osmdroid. Aucune cle Google Maps n'est necessaire.

## Base externe

1. Creer la base avec `server/schema.sql`.
2. Copier `server/places.php` dans un serveur PHP local, par exemple `htdocs/placetag/places.php`.
3. Depuis l'emulateur Android, l'URL utilisee par l'app est `http://10.0.2.2/placetag/places.php`.

## Fonctionnalites implementees

- Interface avec toolbar et navigation drawer.
- Layout portrait et paysage.
- Ressources francais/anglais.
- Localisation temps reel avec latitude et longitude.
- Affichage sur OpenStreetMap avec osmdroid.
- Geocodage inverse vers une adresse.
- Prise de photo avec `FileProvider`.
- Sauvegarde locale SQLite.
- Liste, recherche, filtre, modification et suppression des lieux.
- Synchronisation metadata vers MySQL via PHP.

## Ameliorations qualite

- Recherche instantanee et robuste meme si un lieu importe n'a pas d'adresse ou de description.
- Liste des lieux plus lisible avec carte, statut colore, details et miniature photo locale.
- Gestion plus sure de la camera: une photo annulee ne remplace plus l'ancienne photo.
- Localisation plus tolerant: la position approximative suffit si l'utilisateur refuse la precision fine.
- Synchronisation HTTP plus robuste avec encodage UTF-8, fermeture des connexions et erreurs serveur detaillees.
- Endpoint PHP avec validation JSON/champs obligatoires et reponses HTTP coherentes.

## Verification

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```
