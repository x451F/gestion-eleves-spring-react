# Modèle logique de données

Notation : `#` clé primaire, `*` clé étrangère, `U` unique, `NN` non nul.

| Relation | Attributs et contraintes principales |
|---|---|
| UTILISATEUR | `#id`, `email_normalise U NN`, `password_hash`, `role NN`, `actif NN`, audit |
| ELEVE | `#id`, `numero_dossier U NN`, identité, contact, métadonnées photo, `actif NN` |
| CLASSE | `#id`, `code U NN`, `nom NN`, `niveau NN`, `annee_scolaire NN`, `actif NN`, `U(nom,annee_scolaire)` |
| INSCRIPTION | `#id`, `*eleve_id NN`, `*classe_id NN`, dates, `annee_scolaire NN`, `statut NN`; une seule ACTIVE par élève/année |
| ENSEIGNANT | `#id`, `matricule U NN`, identité, `email U NN`, `*utilisateur_id U`, `actif NN` |
| MATIERE | `#id`, `code U NN`, `nom U NN`, `coefficient_defaut > 0`, `actif NN` |
| ENSEIGNEMENT | `#id`, `*enseignant_id`, `*matiere_id`, `*classe_id`, `annee_scolaire`, `coefficient_matiere > 0`, unicité du quadruplet |
| NOTE | `#id`, `*inscription_id`, `*enseignement_id`, `periode`, `0 <= valeur <= 20`, `bareme=20`, `coefficient>0`, date, libellé, commentaire |
| BULLETIN | `#id`, `*inscription_id`, `periode`, date, statut, moyenne, appréciation, `U(inscription_id,periode)` |
| BULLETIN_LIGNE | `#id`, `*bulletin_id`, code/nom matière figés, moyenne, coefficient, nombre de notes |
| RESPONSABLE | `#id`, identité, `email U NN`, téléphone, `*utilisateur_id U`, actif |
| ELEVE_RESPONSABLE | `#id`, `*eleve_id`, `*responsable_id`, lien et indicateurs, `U(eleve_id,responsable_id)` |

Les relations n:n Élève–Responsable sont transformées en `ELEVE_RESPONSABLE`. `INSCRIPTION` et `ENSEIGNEMENT` sont également des associations porteuses d'attributs métier.
