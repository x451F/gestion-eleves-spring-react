# Gestion des élèves

API REST pédagogique de gestion d’un établissement scolaire. Le projet couvre
les élèves, classes, inscriptions, enseignants, matières, enseignements, notes,
responsables légaux, bulletins et photos d’élèves.

## Objectifs et fonctionnalités

L’application permet de :

- gérer les référentiels scolaires par API REST ;
- inscrire un élève dans une classe pour une année scolaire ;
- affecter un enseignant et une matière à une classe ;
- saisir des notes sur 20 et calculer des moyennes pondérées ;
- associer plusieurs responsables à un élève ;
- produire un bulletin figé par trimestre et le télécharger en PDF ;
- stocker localement une photo contrôlée sans exposer le système de fichiers.

## Architecture

Le backend suit une architecture en couches :

```text
Controller REST → DTO → Service transactionnel → Repository JPA → PostgreSQL
                         ↘ stockage photo
                         ↘ génération PDF
```

Les entités JPA ne sont jamais exposées directement. Les règles métier résident
dans les services, les conversions dans un mapper manuel et les erreurs dans un
gestionnaire global. Flyway constitue la source de vérité du schéma ;
Hibernate l’examine au démarrage avec `ddl-auto=validate`.

## Technologies

- Java 21 ;
- Spring Boot 3.5 ;
- Spring Web, Spring Data JPA et Bean Validation ;
- PostgreSQL 15 et Flyway ;
- OpenPDF ;
- Maven Wrapper ;
- JUnit 5, Mockito, AssertJ, MockMvc et Testcontainers.

## Prérequis

- JDK 21 ou supérieur ;
- Docker Desktop ou Docker Engine avec Docker Compose ;
- ports locaux `5432` et `8080` disponibles.

## Structure

```text
backend/                 application Spring Boot et tests
docs/api/                contrat REST et collection Postman
docs/merise/             MCD, MLD et MPD
docs/uml/                cas d’utilisation, classes et séquence
docker-compose.yml       PostgreSQL local
.env.example             exemple de configuration
```

Les répertoires `uploads/`, `target/`, les secrets et les données PostgreSQL
locales sont exclus du dépôt.

## Configuration

Les variables disponibles sont documentées dans `.env.example` :

| Variable | Valeur locale par défaut | Usage |
|---|---|---|
| `POSTGRES_DB` | `gestion_eleves` | base créée par Docker |
| `POSTGRES_USER` | `gestion_eleves` | compte PostgreSQL |
| `POSTGRES_PASSWORD` | `gestion_eleves_dev` | mot de passe local |
| `POSTGRES_PORT` | `5432` | port publié |
| `DB_URL` | `jdbc:postgresql://localhost:5432/gestion_eleves` | URL JDBC |
| `DB_USERNAME` | `gestion_eleves` | utilisateur JDBC |
| `DB_PASSWORD` | `gestion_eleves_dev` | mot de passe JDBC |
| `UPLOAD_DIR` | `uploads/eleves` | stockage des photos |
| `FRONTEND_ORIGIN` | `http://localhost:5173` | origine CORS autorisée |
| `MAX_PHOTO_SIZE` | `5MB` | limite multipart Spring |
| `MAX_PHOTO_SIZE_BYTES` | `5242880` | taille métier maximale |
| `SERVER_PORT` | `8080` | port HTTP |

Ne jamais versionner un fichier `.env` réel.

## Démarrage de PostgreSQL

Depuis la racine :

```bash
docker compose config
docker compose up -d
docker compose ps
```

Attendre l’état `healthy` avant de lancer l’API. Le volume nommé
`postgres_data` conserve les données entre les redémarrages.

## Démarrage du backend

```bash
cd backend
./mvnw spring-boot:run
```

L’API écoute sur `http://localhost:8080/api`. Au premier démarrage, Flyway
applique `V1__initial_schema.sql`, puis Hibernate vérifie sa cohérence avec le
modèle JPA.

## Tests et paquetage

Docker doit fonctionner pour les tests d’intégration PostgreSQL :

```bash
cd backend
./mvnw test
./mvnw clean package
```

Les tests unitaires isolent les services. Les tests d’intégration exercent
l’API avec MockMvc et une instance PostgreSQL éphémère Testcontainers.

## Vue d’ensemble REST

| Ressource | Base |
|---|---|
| Élèves | `/api/eleves` |
| Classes | `/api/classes` |
| Inscriptions | `/api/inscriptions` |
| Enseignants | `/api/enseignants` |
| Matières | `/api/matieres` |
| Enseignements | `/api/enseignements` |
| Notes | `/api/notes` |
| Responsables | `/api/responsables` |
| Bulletins | `/api/bulletins` |
| Utilisateurs préparatoires, lecture seule | `/api/utilisateurs` |

Chaque ressource métier principale propose les opérations CRUD pertinentes.
Les requêtes transverses sont disponibles sous les élèves, classes,
enseignants et inscriptions. Le contrat détaillé et tous les statuts figurent
dans [docs/api/endpoints.md](docs/api/endpoints.md).

## Photo d’un élève

Formats acceptés : JPEG, PNG et WebP, dans la limite configurée. Le type réel
est vérifié par signature binaire et le nom de stockage est généré par UUID.

```bash
curl -F "file=@portrait.png;type=image/png" \
  http://localhost:8080/api/eleves/1/photo

curl --output portrait-retour.png \
  http://localhost:8080/api/eleves/1/photo

curl -X DELETE http://localhost:8080/api/eleves/1/photo
```

## Bulletin et PDF

Un bulletin est un instantané des moyennes d’une inscription et d’une période.
Il ne peut être généré sans notes et un seul bulletin est autorisé par couple
inscription/période.

```bash
curl -X POST http://localhost:8080/api/bulletins/generate \
  -H 'Content-Type: application/json' \
  -d '{"inscriptionId":1,"periode":"TRIMESTRE_1","appreciation":"Bon trimestre"}'

curl -OJ http://localhost:8080/api/bulletins/1/pdf
```

Le PDF est généré en mémoire, en français, avec un nom de téléchargement
normalisé.

## Modélisation

- MCD : `docs/merise/mcd.svg` et sa source DOT ;
- MLD : `docs/merise/mld.md` ;
- MPD : `docs/merise/mpd.sql` ;
- UML : `docs/uml/use-case.svg`, `class-diagram.svg` et
  `sequence-add-note.svg`, avec leurs sources DOT.

## État de la sécurité

L’API est intentionnellement non sécurisée à ce stade. L’entité
`Utilisateur`, les rôles et les liens facultatifs vers les acteurs sont
préparés, mais aucun mot de passe brut n’est accepté et aucune route de
connexion ou d’inscription n’existe.

La phase suivante ajoutera Spring Security et JWT après validation complète de
ce socle métier.

## Limites connues

- pas encore d’authentification ni d’autorisation ;
- stockage photo local, prévu pour être monté sur un volume ;
- pas de pagination ni de tri avancé ;
- pas d’interface web ;
- bulletin limité aux trois trimestres et aux notes sur 20 ;
- pas d’envoi d’e-mail ni de stockage objet distant.

## Prochaine phase

La prochaine phase recommandée est l’ajout isolé de Spring Security : hachage
des mots de passe, création contrôlée des comptes, authentification JWT,
restrictions par rôle et tests `401/403`. Elle ne doit pas modifier les règles
métier déjà couvertes par les tests.
