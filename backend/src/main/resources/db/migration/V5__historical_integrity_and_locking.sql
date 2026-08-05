DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM inscription WHERE statut = 'ACTIVE' GROUP BY eleve_id HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot migrate inscription: a pupil has multiple ACTIVE legacy registrations'
            USING ERRCODE = '23514';
    END IF;
    IF EXISTS (
        SELECT 1 FROM eleve_responsable
        WHERE responsable_principal
        GROUP BY eleve_id HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot migrate eleve_responsable: a pupil has multiple active principal guardians'
            USING ERRCODE = '23514';
    END IF;
END $$;

ALTER TABLE inscription DROP CONSTRAINT inscription_statut_check;
DROP INDEX uk_inscription_active_eleve_annee;
UPDATE inscription SET statut = 'EN_COURS' WHERE statut = 'ACTIVE';
ALTER TABLE inscription
    ADD CONSTRAINT ck_inscription_statut CHECK (statut IN ('EN_COURS', 'TERMINEE', 'ANNULEE')),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE UNIQUE INDEX uk_inscription_en_cours_eleve ON inscription(eleve_id) WHERE statut = 'EN_COURS';

ALTER TABLE eleve_responsable
    ADD COLUMN valid_from DATE,
    ADD COLUMN valid_to DATE,
    ADD CONSTRAINT ck_eleve_responsable_validity CHECK (valid_to IS NULL OR valid_to >= valid_from);
UPDATE eleve_responsable SET valid_from = created_at::date;
ALTER TABLE eleve_responsable ALTER COLUMN valid_from SET NOT NULL;
CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE eleve_responsable
    ADD CONSTRAINT ex_eleve_responsable_principal_validity
    EXCLUDE USING gist (
        eleve_id WITH =,
        daterange(valid_from, CASE WHEN valid_to IS NULL THEN NULL ELSE valid_to + 1 END, '[)') WITH &&
    ) WHERE (responsable_principal);

ALTER TABLE eleve_responsable DROP CONSTRAINT eleve_responsable_eleve_id_fkey;
ALTER TABLE eleve_responsable DROP CONSTRAINT eleve_responsable_responsable_id_fkey;
ALTER TABLE eleve_responsable
    ADD CONSTRAINT fk_eleve_responsable_eleve FOREIGN KEY (eleve_id) REFERENCES eleve(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_eleve_responsable_responsable FOREIGN KEY (responsable_id) REFERENCES responsable(id) ON DELETE RESTRICT;

ALTER TABLE bulletin_ligne DROP CONSTRAINT bulletin_ligne_bulletin_id_fkey;
ALTER TABLE bulletin_ligne
    ADD CONSTRAINT fk_bulletin_ligne_bulletin FOREIGN KEY (bulletin_id) REFERENCES bulletin(id) ON DELETE RESTRICT;

ALTER TABLE note ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE bulletin ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE enseignement ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
