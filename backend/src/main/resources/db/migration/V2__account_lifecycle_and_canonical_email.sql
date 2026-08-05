-- Refuse ambiguous legacy identities before canonicalising the authentication email.
DO $$
DECLARE
    affected_ids TEXT;
BEGIN
    SELECT string_agg(id::text, ', ' ORDER BY id) INTO affected_ids
    FROM utilisateur
    WHERE email_normalise IS NULL OR btrim(email_normalise) = '';
    IF affected_ids IS NOT NULL THEN
        RAISE EXCEPTION 'Cannot migrate utilisateur: blank canonical email for utilisateur ids [%]', affected_ids
            USING ERRCODE = '23514';
    END IF;

    SELECT string_agg(id::text, ', ' ORDER BY id) INTO affected_ids
    FROM utilisateur
    WHERE NOT actif AND password_hash IS NULL;
    IF affected_ids IS NOT NULL THEN
        RAISE EXCEPTION 'Cannot migrate utilisateur: disabled passwordless account has ambiguous lifecycle for utilisateur ids [%]', affected_ids
            USING ERRCODE = '23514';
    END IF;

    IF EXISTS (
        SELECT 1 FROM utilisateur GROUP BY lower(btrim(email_normalise)) HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot migrate utilisateur: multiple accounts share the same normalized email'
            USING ERRCODE = '23514';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM utilisateur u
        LEFT JOIN enseignant e ON e.utilisateur_id = u.id
        LEFT JOIN responsable r ON r.utilisateur_id = u.id
        WHERE (u.role = 'ADMIN' AND (e.id IS NOT NULL OR r.id IS NOT NULL))
           OR (u.role = 'ENSEIGNANT' AND (e.id IS NULL OR r.id IS NOT NULL))
           OR (u.role = 'RESPONSABLE' AND (r.id IS NULL OR e.id IS NOT NULL))
           OR (e.id IS NOT NULL AND lower(btrim(e.email)) <> lower(btrim(u.email_normalise)))
           OR (r.id IS NOT NULL AND lower(btrim(r.email)) <> lower(btrim(u.email_normalise)))
    ) THEN
        RAISE EXCEPTION 'Cannot migrate utilisateur: linked profile role or email conflicts with its account'
            USING ERRCODE = '23514';
    END IF;
END $$;

ALTER TABLE utilisateur
    ADD COLUMN statut VARCHAR(30),
    ADD COLUMN email_verifie_at TIMESTAMP,
    ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- A legacy account without a password cannot be made active safely; it must activate first.
UPDATE utilisateur
SET email_normalise = lower(btrim(email_normalise)),
    statut = CASE
        WHEN password_hash IS NULL THEN 'EN_ATTENTE_ACTIVATION'
        WHEN actif THEN 'ACTIF'
        ELSE 'DESACTIVE'
    END,
    email_verifie_at = CASE
        WHEN password_hash IS NULL THEN NULL
        ELSE COALESCE(last_login_at, updated_at, created_at)
    END;

ALTER TABLE utilisateur
    ALTER COLUMN statut SET NOT NULL,
    DROP COLUMN actif,
    ADD CONSTRAINT ck_utilisateur_email_normalise
        CHECK (btrim(email_normalise) <> '' AND email_normalise = lower(btrim(email_normalise))),
    ADD CONSTRAINT ck_utilisateur_lifecycle_password
        CHECK ((statut = 'EN_ATTENTE_ACTIVATION' AND password_hash IS NULL AND email_verifie_at IS NULL)
            OR (statut IN ('ACTIF', 'DESACTIVE') AND password_hash IS NOT NULL AND email_verifie_at IS NOT NULL)),
    ADD CONSTRAINT ck_utilisateur_statut
        CHECK (statut IN ('EN_ATTENTE_ACTIVATION', 'ACTIF', 'DESACTIVE')),
    ADD CONSTRAINT ck_utilisateur_token_version
        CHECK (token_version >= 0);

CREATE UNIQUE INDEX uk_utilisateur_email_normalise_ci
    ON utilisateur (lower(email_normalise));

-- Profile emails remain temporarily for API compatibility and legacy-data provenance only.
ALTER TABLE enseignant ADD COLUMN legacy_email VARCHAR(180);
ALTER TABLE responsable ADD COLUMN legacy_email VARCHAR(180);
UPDATE enseignant SET legacy_email = email;
UPDATE responsable SET legacy_email = email;
COMMENT ON COLUMN enseignant.email IS 'Deprecated legacy profile contact/source email; authentication uses utilisateur.email_normalise.';
COMMENT ON COLUMN responsable.email IS 'Deprecated legacy profile contact/source email; authentication uses utilisateur.email_normalise.';
COMMENT ON COLUMN enseignant.legacy_email IS 'Original V1 profile email retained during the canonical-email transition.';
COMMENT ON COLUMN responsable.legacy_email IS 'Original V1 profile email retained during the canonical-email transition.';
