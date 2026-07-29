# Contrat de l’API REST

Base locale : `http://localhost:8080/api`

L’API est volontairement sans authentification dans cette version. La colonne
« rôle futur » documente l’intention de la prochaine phase et n’est pas encore
appliquée. Les corps et réponses sont en JSON, sauf les photos et PDF.

## Conventions

- `201 Created` : ressource créée, avec en-tête `Location` ;
- `200 OK` : lecture ou modification réussie ;
- `204 No Content` : suppression réussie ;
- `400 Bad Request` : validation ou règle métier ;
- `404 Not Found` : identifiant ou fichier absent ;
- `409 Conflict` : unicité ou association dupliquée ;
- `413 Payload Too Large` : photo trop volumineuse ;
- `415 Unsupported Media Type` : image non admise.

Une erreur JSON contient `timestamp`, `status`, `code`, `message`, `path` et
`fieldErrors`. Les dates suivent ISO-8601. Les montants académiques utilisent
des nombres décimaux.

## Élèves

| Méthode | Chemin | Requête / réponse | Succès | Rôle futur |
|---|---|---|---|---|
| POST | `/eleves` | `EleveRequest` → `EleveResponse` | 201 | ADMIN |
| GET | `/eleves` | — → liste | 200 | ADMIN, ENSEIGNANT |
| GET | `/eleves/{id}` | — → détail | 200 | ADMIN, ENSEIGNANT, RESPONSABLE associé |
| PUT | `/eleves/{id}` | `EleveRequest` → détail | 200 | ADMIN |
| DELETE | `/eleves/{id}` | — | 204 | ADMIN |
| GET | `/eleves/{id}/inscriptions` | — → inscriptions | 200 | ADMIN, ENSEIGNANT |
| GET | `/eleves/{id}/notes` | — → notes | 200 | ADMIN, ENSEIGNANT, RESPONSABLE associé |
| GET | `/eleves/{id}/responsables` | — → associations | 200 | ADMIN |

`EleveRequest` :

```json
{
  "numeroDossier": "DOS-2026-001",
  "nom": "Martin",
  "prenom": "Lina",
  "dateNaissance": "2012-04-02",
  "email": "lina.martin@example.fr",
  "telephone": "0611223344",
  "actif": true
}
```

Le numéro de dossier est unique, les nom et prénom sont obligatoires, la date
de naissance doit être passée et l’e-mail doit être valide.

## Classes

| Méthode | Chemin | Requête / réponse | Succès | Rôle futur |
|---|---|---|---|---|
| POST | `/classes` | `ClasseRequest` → `ClasseResponse` | 201 | ADMIN |
| GET | `/classes` | — → liste | 200 | ADMIN, ENSEIGNANT |
| GET | `/classes/{id}` | — → détail | 200 | ADMIN, ENSEIGNANT |
| PUT | `/classes/{id}` | `ClasseRequest` → détail | 200 | ADMIN |
| DELETE | `/classes/{id}` | — | 204 | ADMIN |
| GET | `/classes/{id}/inscriptions` | — → inscriptions | 200 | ADMIN, ENSEIGNANT |
| GET | `/classes/{id}/enseignements` | — → enseignements | 200 | ADMIN, ENSEIGNANT |

```json
{
  "code": "6A-2026",
  "nom": "6e A",
  "niveau": "6e",
  "anneeScolaire": "2026-2027",
  "actif": true
}
```

Le code est unique, le couple nom/année est unique et l’année respecte
`YYYY-YYYY`.

## Inscriptions

| Méthode | Chemin | Requête / réponse | Succès | Rôle futur |
|---|---|---|---|---|
| POST | `/inscriptions` | `InscriptionRequest` → détail | 201 | ADMIN |
| GET | `/inscriptions` | — → liste | 200 | ADMIN |
| GET | `/inscriptions/{id}` | — → détail | 200 | ADMIN, ENSEIGNANT |
| PUT | `/inscriptions/{id}` | `InscriptionRequest` → détail | 200 | ADMIN |
| DELETE | `/inscriptions/{id}` | — | 204 | ADMIN |
| GET | `/inscriptions/{id}/notes` | — → notes | 200 | ADMIN, ENSEIGNANT, RESPONSABLE associé |
| GET | `/inscriptions/{id}/moyennes?periode=TRIMESTRE_1` | — → moyennes | 200 | ADMIN, ENSEIGNANT, RESPONSABLE associé |
| GET | `/inscriptions/{id}/bulletins` | — → bulletins | 200 | ADMIN, ENSEIGNANT, RESPONSABLE associé |

```json
{
  "eleveId": 1,
  "classeId": 1,
  "anneeScolaire": "2026-2027",
  "dateInscription": "2026-07-29",
  "dateFin": null,
  "statut": "ACTIVE"
}
```

Valeurs de statut : `ACTIVE`, `TERMINEE`, `ANNULEE`. L’élève et la classe
doivent exister, les années doivent correspondre et une seule inscription
active est permise par élève et par année. Une date de fin ne précède pas la
date d’inscription.

## Enseignants

| Méthode | Chemin | Requête / réponse | Succès | Rôle futur |
|---|---|---|---|---|
| POST | `/enseignants` | `EnseignantRequest` → détail | 201 | ADMIN |
| GET | `/enseignants` | — → liste | 200 | ADMIN |
| GET | `/enseignants/{id}` | — → détail | 200 | ADMIN |
| PUT | `/enseignants/{id}` | `EnseignantRequest` → détail | 200 | ADMIN |
| DELETE | `/enseignants/{id}` | — | 204 | ADMIN |
| GET | `/enseignants/{id}/enseignements` | — → enseignements | 200 | ADMIN, enseignant concerné |

```json
{
  "matricule": "ENS-001",
  "nom": "Durand",
  "prenom": "Alice",
  "email": "alice.durand@example.fr",
  "actif": true
}
```

Matricule et e-mail sont uniques. Aucun compte utilisateur n’est obligatoire
dans cette phase.

## Matières

| Méthode | Chemin | Requête / réponse | Succès | Rôle futur |
|---|---|---|---|---|
| POST | `/matieres` | `MatiereRequest` → détail | 201 | ADMIN |
| GET | `/matieres` | — → liste | 200 | ADMIN, ENSEIGNANT |
| GET | `/matieres/{id}` | — → détail | 200 | ADMIN, ENSEIGNANT |
| PUT | `/matieres/{id}` | `MatiereRequest` → détail | 200 | ADMIN |
| DELETE | `/matieres/{id}` | — | 204 | ADMIN |

```json
{
  "code": "MATH",
  "nom": "Mathématiques",
  "coefficientDefaut": 2.0,
  "actif": true
}
```

Code et nom sont uniques ; le coefficient est strictement positif.

## Enseignements

| Méthode | Chemin | Requête / réponse | Succès | Rôle futur |
|---|---|---|---|---|
| POST | `/enseignements` | `EnseignementRequest` → détail | 201 | ADMIN |
| GET | `/enseignements` | — → liste | 200 | ADMIN |
| GET | `/enseignements/{id}` | — → détail | 200 | ADMIN, enseignant concerné |
| PUT | `/enseignements/{id}` | `EnseignementRequest` → détail | 200 | ADMIN |
| DELETE | `/enseignements/{id}` | — | 204 | ADMIN |

```json
{
  "enseignantId": 1,
  "matiereId": 1,
  "classeId": 1,
  "anneeScolaire": "2026-2027",
  "coefficientMatiere": 2.0
}
```

Toutes les références doivent exister, l’année correspond à celle de la classe,
le coefficient est positif et l’affectation enseignant/matière/classe/année ne
peut être dupliquée.

## Notes et moyennes

| Méthode | Chemin | Requête / réponse | Succès | Rôle futur |
|---|---|---|---|---|
| POST | `/notes` | `NoteRequest` → détail | 201 | ADMIN, ENSEIGNANT |
| GET | `/notes` | — → liste | 200 | ADMIN |
| GET | `/notes/{id}` | — → détail | 200 | ADMIN, ENSEIGNANT, RESPONSABLE associé |
| PUT | `/notes/{id}` | `NoteRequest` → détail | 200 | ADMIN, ENSEIGNANT |
| DELETE | `/notes/{id}` | — | 204 | ADMIN, ENSEIGNANT |

```json
{
  "inscriptionId": 1,
  "enseignementId": 1,
  "periode": "TRIMESTRE_1",
  "valeur": 15.0,
  "bareme": 20.0,
  "coefficient": 1.0,
  "dateEvaluation": "2026-07-29",
  "libelle": "Contrôle 1",
  "commentaire": "Travail sérieux"
}
```

Périodes : `TRIMESTRE_1`, `TRIMESTRE_2`, `TRIMESTRE_3`. La valeur est comprise
entre 0 et 20, le barème vaut 20, le coefficient est positif et la date n’est
pas future. L’inscription et l’enseignement doivent viser la même classe et la
même année.

Les moyennes de matière pondèrent les notes par leur coefficient. La moyenne
générale pondère les moyennes de matière par `coefficientMatiere`. Tous les
résultats sont arrondis à deux décimales avec `HALF_UP`.

## Responsables et associations

| Méthode | Chemin | Requête / réponse | Succès | Rôle futur |
|---|---|---|---|---|
| POST | `/responsables` | `ResponsableRequest` → détail | 201 | ADMIN |
| GET | `/responsables` | — → liste | 200 | ADMIN |
| GET | `/responsables/{id}` | — → détail | 200 | ADMIN |
| PUT | `/responsables/{id}` | `ResponsableRequest` → détail | 200 | ADMIN |
| DELETE | `/responsables/{id}` | — | 204 | ADMIN |
| POST | `/responsables/{responsableId}/eleves/{eleveId}` | métadonnées → association | 201 | ADMIN |
| DELETE | `/responsables/{responsableId}/eleves/{eleveId}` | — | 204 | ADMIN |
| GET | `/responsables/{id}/eleves` | — → associations | 200 | ADMIN, responsable concerné |

Création d’un responsable :

```json
{
  "nom": "Martin",
  "prenom": "Sophie",
  "email": "sophie.martin@example.fr",
  "telephone": "0601020304",
  "actif": true
}
```

Métadonnées d’association :

```json
{
  "lienParente": "MERE",
  "responsablePrincipal": true,
  "autoriteParentale": true,
  "contactUrgence": true
}
```

Liens admis : `PERE`, `MERE`, `TUTEUR`, `AUTRE`. Une association identique
retourne `409`.

## Bulletins et PDF

| Méthode | Chemin | Requête / réponse | Succès | Rôle futur |
|---|---|---|---|---|
| POST | `/bulletins/generate` | `BulletinGenerateRequest` → bulletin | 201 | ADMIN, ENSEIGNANT |
| GET | `/bulletins` | — → liste | 200 | ADMIN |
| GET | `/bulletins/{id}` | — → détail et lignes figées | 200 | ADMIN, ENSEIGNANT, RESPONSABLE associé |
| POST | `/bulletins/{id}/publier` | — → bulletin publié | 200 | ADMIN |
| DELETE | `/bulletins/{id}` | — | 204 | ADMIN, brouillon uniquement |
| GET | `/bulletins/{id}/pdf` | — → `application/pdf` | 200 | ADMIN, ENSEIGNANT, RESPONSABLE associé |

```json
{
  "inscriptionId": 1,
  "periode": "TRIMESTRE_1",
  "appreciation": "Bon trimestre"
}
```

La génération exige au moins une note, calcule et persiste un instantané, et
refuse un second bulletin pour la même inscription et période. Un bulletin
publié ne peut pas être supprimé. Le téléchargement renvoie
`Content-Disposition: attachment` avec un nom normalisé.

## Photo

| Méthode | Chemin | Requête / réponse | Succès | Rôle futur |
|---|---|---|---|---|
| POST | `/eleves/{id}/photo` | multipart, champ `file` → métadonnées | 200 | ADMIN |
| GET | `/eleves/{id}/photo` | — → octets et type image | 200 | ADMIN, ENSEIGNANT, RESPONSABLE associé |
| DELETE | `/eleves/{id}/photo` | — | 204 | ADMIN |

JPEG, PNG et WebP sont admis. Le fichier doit être non vide, sa taille respecter
la limite et sa signature binaire correspondre au MIME annoncé. Le client ne
choisit jamais le chemin de stockage.

## Utilisateurs préparatoires

| Méthode | Chemin | Requête / réponse | Succès | Rôle futur |
|---|---|---|---|---|
| GET | `/utilisateurs` | — → réponses sans `passwordHash` | 200 | ADMIN |
| GET | `/utilisateurs/{id}` | — → réponse sans `passwordHash` | 200 | ADMIN |

Ces routes sont uniquement en lecture. Il n’existe ni login, ni inscription,
ni entrée de mot de passe dans cette version.
