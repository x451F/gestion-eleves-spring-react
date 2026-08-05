ALTER TABLE bulletin DROP CONSTRAINT uk_bulletin_inscription_periode;
ALTER TABLE bulletin DROP CONSTRAINT bulletin_statut_check;
ALTER TABLE bulletin ADD CONSTRAINT ck_bulletin_statut CHECK (statut IN ('BROUILLON', 'PUBLIE', 'REMPLACE'));
ALTER TABLE bulletin ADD COLUMN version_precedente_id BIGINT REFERENCES bulletin(id) ON DELETE RESTRICT;
CREATE UNIQUE INDEX uk_bulletin_publie_courant ON bulletin(inscription_id, periode) WHERE statut = 'PUBLIE';
