CREATE TABLE utilisateur (
    id BIGSERIAL PRIMARY KEY,
    email_normalise VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    role VARCHAR(30) NOT NULL CHECK (role IN ('ADMIN', 'ENSEIGNANT', 'RESPONSABLE')),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP
);

CREATE TABLE eleve (
    id BIGSERIAL PRIMARY KEY,
    numero_dossier VARCHAR(50) NOT NULL UNIQUE,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    date_naissance DATE NOT NULL,
    email VARCHAR(180),
    telephone VARCHAR(30),
    photo_nom_stockage VARCHAR(255),
    photo_type_mime VARCHAR(50),
    photo_taille BIGINT,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE classe (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    nom VARCHAR(100) NOT NULL,
    niveau VARCHAR(100) NOT NULL,
    annee_scolaire VARCHAR(9) NOT NULL CHECK (annee_scolaire ~ '^[0-9]{4}-[0-9]{4}$'),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_classe_nom_annee UNIQUE (nom, annee_scolaire)
);

CREATE TABLE enseignant (
    id BIGSERIAL PRIMARY KEY,
    matricule VARCHAR(50) NOT NULL UNIQUE,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    utilisateur_id BIGINT UNIQUE REFERENCES utilisateur(id),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE matiere (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    nom VARCHAR(100) NOT NULL UNIQUE,
    coefficient_defaut NUMERIC(8,2) NOT NULL CHECK (coefficient_defaut > 0),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE responsable (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    telephone VARCHAR(30),
    utilisateur_id BIGINT UNIQUE REFERENCES utilisateur(id),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE inscription (
    id BIGSERIAL PRIMARY KEY,
    eleve_id BIGINT NOT NULL REFERENCES eleve(id),
    classe_id BIGINT NOT NULL REFERENCES classe(id),
    annee_scolaire VARCHAR(9) NOT NULL CHECK (annee_scolaire ~ '^[0-9]{4}-[0-9]{4}$'),
    date_inscription DATE NOT NULL,
    date_fin DATE,
    statut VARCHAR(20) NOT NULL CHECK (statut IN ('ACTIVE', 'TERMINEE', 'ANNULEE')),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CHECK (date_fin IS NULL OR date_fin >= date_inscription)
);
CREATE UNIQUE INDEX uk_inscription_active_eleve_annee
    ON inscription(eleve_id, annee_scolaire) WHERE statut = 'ACTIVE';

CREATE TABLE enseignement (
    id BIGSERIAL PRIMARY KEY,
    enseignant_id BIGINT NOT NULL REFERENCES enseignant(id),
    matiere_id BIGINT NOT NULL REFERENCES matiere(id),
    classe_id BIGINT NOT NULL REFERENCES classe(id),
    annee_scolaire VARCHAR(9) NOT NULL CHECK (annee_scolaire ~ '^[0-9]{4}-[0-9]{4}$'),
    coefficient_matiere NUMERIC(8,2) NOT NULL CHECK (coefficient_matiere > 0),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_enseignement_affectation UNIQUE (enseignant_id, matiere_id, classe_id, annee_scolaire)
);

CREATE TABLE note (
    id BIGSERIAL PRIMARY KEY,
    inscription_id BIGINT NOT NULL REFERENCES inscription(id),
    enseignement_id BIGINT NOT NULL REFERENCES enseignement(id),
    periode VARCHAR(20) NOT NULL CHECK (periode IN ('TRIMESTRE_1', 'TRIMESTRE_2', 'TRIMESTRE_3')),
    valeur NUMERIC(5,2) NOT NULL CHECK (valeur >= 0 AND valeur <= 20),
    bareme NUMERIC(5,2) NOT NULL DEFAULT 20.00 CHECK (bareme = 20.00),
    coefficient NUMERIC(8,2) NOT NULL CHECK (coefficient > 0),
    date_evaluation DATE NOT NULL,
    libelle VARCHAR(150),
    commentaire VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE bulletin (
    id BIGSERIAL PRIMARY KEY,
    inscription_id BIGINT NOT NULL REFERENCES inscription(id),
    periode VARCHAR(20) NOT NULL CHECK (periode IN ('TRIMESTRE_1', 'TRIMESTRE_2', 'TRIMESTRE_3')),
    date_generation TIMESTAMP NOT NULL,
    statut VARCHAR(20) NOT NULL CHECK (statut IN ('BROUILLON', 'PUBLIE')),
    moyenne_generale NUMERIC(5,2) NOT NULL CHECK (moyenne_generale >= 0 AND moyenne_generale <= 20),
    appreciation VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_bulletin_inscription_periode UNIQUE (inscription_id, periode)
);

CREATE TABLE bulletin_ligne (
    id BIGSERIAL PRIMARY KEY,
    bulletin_id BIGINT NOT NULL REFERENCES bulletin(id) ON DELETE CASCADE,
    code_matiere VARCHAR(50) NOT NULL,
    nom_matiere VARCHAR(100) NOT NULL,
    moyenne NUMERIC(5,2) NOT NULL CHECK (moyenne >= 0 AND moyenne <= 20),
    coefficient NUMERIC(8,2) NOT NULL CHECK (coefficient > 0),
    nombre_notes INTEGER NOT NULL CHECK (nombre_notes > 0),
    CONSTRAINT uk_bulletin_ligne_matiere UNIQUE (bulletin_id, code_matiere)
);

CREATE TABLE eleve_responsable (
    id BIGSERIAL PRIMARY KEY,
    eleve_id BIGINT NOT NULL REFERENCES eleve(id) ON DELETE CASCADE,
    responsable_id BIGINT NOT NULL REFERENCES responsable(id) ON DELETE CASCADE,
    lien_parente VARCHAR(30) NOT NULL CHECK (lien_parente IN ('PERE', 'MERE', 'TUTEUR', 'AUTRE')),
    responsable_principal BOOLEAN NOT NULL DEFAULT FALSE,
    autorite_parentale BOOLEAN NOT NULL DEFAULT TRUE,
    contact_urgence BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_eleve_responsable UNIQUE (eleve_id, responsable_id)
);

CREATE INDEX idx_inscription_eleve ON inscription(eleve_id);
CREATE INDEX idx_inscription_classe ON inscription(classe_id);
CREATE INDEX idx_enseignement_enseignant ON enseignement(enseignant_id);
CREATE INDEX idx_enseignement_classe ON enseignement(classe_id);
CREATE INDEX idx_note_inscription ON note(inscription_id);
CREATE INDEX idx_note_enseignement ON note(enseignement_id);
CREATE INDEX idx_bulletin_inscription ON bulletin(inscription_id);
