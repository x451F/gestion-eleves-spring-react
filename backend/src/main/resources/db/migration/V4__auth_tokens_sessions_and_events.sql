CREATE TABLE activation_token (
    id BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT NOT NULL REFERENCES utilisateur(id) ON DELETE RESTRICT,
    token_hash VARCHAR(64) NOT NULL UNIQUE CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    revoked_at TIMESTAMP,
    revocation_reason VARCHAR(100),
    CHECK (used_at IS NULL OR used_at >= created_at),
    CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);

CREATE TABLE password_reset_token (
    id BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT NOT NULL REFERENCES utilisateur(id) ON DELETE RESTRICT,
    token_hash VARCHAR(64) NOT NULL UNIQUE CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    revoked_at TIMESTAMP,
    revocation_reason VARCHAR(100),
    CHECK (used_at IS NULL OR used_at >= created_at),
    CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);

CREATE TABLE refresh_session_family (
    id BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT NOT NULL REFERENCES utilisateur(id) ON DELETE RESTRICT,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP,
    revoked_at TIMESTAMP,
    revocation_reason VARCHAR(100),
    device_label VARCHAR(255),
    CHECK (expires_at > created_at),
    CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);

CREATE TABLE refresh_session (
    id BIGSERIAL PRIMARY KEY,
    family_id BIGINT NOT NULL REFERENCES refresh_session_family(id) ON DELETE RESTRICT,
    generation BIGINT NOT NULL CHECK (generation >= 0),
    token_hash VARCHAR(64) NOT NULL UNIQUE CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    revoked_at TIMESTAMP,
    revocation_reason VARCHAR(100),
    replaced_by_id BIGINT UNIQUE,
    replaced_by_generation BIGINT,
    CHECK (expires_at > created_at),
    CHECK (used_at IS NULL OR used_at >= created_at),
    CHECK (revoked_at IS NULL OR revoked_at >= created_at),
    CHECK ((replaced_by_id IS NULL AND replaced_by_generation IS NULL)
        OR (replaced_by_id IS NOT NULL AND replaced_by_generation IS NOT NULL
            AND replaced_by_id <> id AND replaced_by_generation > generation)),
    CONSTRAINT uk_refresh_session_id_family_generation UNIQUE (id, family_id, generation),
    CONSTRAINT uk_refresh_session_family_generation UNIQUE (family_id, generation),
    CONSTRAINT fk_refresh_session_replacement_same_family
        FOREIGN KEY (replaced_by_id, family_id, replaced_by_generation)
        REFERENCES refresh_session(id, family_id, generation) ON DELETE RESTRICT
);
CREATE INDEX idx_refresh_session_family ON refresh_session(family_id);

CREATE OR REPLACE FUNCTION reject_refresh_session_identity_change() RETURNS trigger AS $$
BEGIN
    IF NEW.family_id IS DISTINCT FROM OLD.family_id THEN
        RAISE EXCEPTION 'Refresh session family membership is immutable' USING ERRCODE = '23514';
    END IF;
    IF NEW.generation IS DISTINCT FROM OLD.generation THEN
        RAISE EXCEPTION 'Refresh session generation is immutable' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_refresh_session_identity_immutable
BEFORE UPDATE OF family_id, generation ON refresh_session
FOR EACH ROW EXECUTE FUNCTION reject_refresh_session_identity_change();

CREATE OR REPLACE FUNCTION reject_refresh_session_replacement_change() RETURNS trigger AS $$
BEGIN
    IF OLD.replaced_by_id IS NOT NULL OR OLD.replaced_by_generation IS NOT NULL THEN
        IF NEW.replaced_by_id IS DISTINCT FROM OLD.replaced_by_id
           OR NEW.replaced_by_generation IS DISTINCT FROM OLD.replaced_by_generation THEN
            RAISE EXCEPTION 'Refresh session replacement can only be assigned once' USING ERRCODE = '23514';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_refresh_session_replacement_immutable
BEFORE UPDATE OF replaced_by_id, replaced_by_generation ON refresh_session
FOR EACH ROW EXECUTE FUNCTION reject_refresh_session_replacement_change();

CREATE OR REPLACE FUNCTION reject_refresh_family_owner_change() RETURNS trigger AS $$
BEGIN
    IF NEW.utilisateur_id IS DISTINCT FROM OLD.utilisateur_id
       AND EXISTS (SELECT 1 FROM refresh_session WHERE family_id = OLD.id) THEN
        RAISE EXCEPTION 'Refresh family owner cannot change after sessions exist' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_refresh_family_owner_immutable_with_sessions
BEFORE UPDATE OF utilisateur_id ON refresh_session_family
FOR EACH ROW EXECUTE FUNCTION reject_refresh_family_owner_change();

CREATE TABLE security_event (
    id BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT REFERENCES utilisateur(id) ON DELETE RESTRICT,
    refresh_family_id BIGINT REFERENCES refresh_session_family(id) ON DELETE RESTRICT,
    event_type VARCHAR(60) NOT NULL CHECK (event_type IN (
        'INVITATION_CREATED', 'INVITATION_RESENT', 'ACTIVATION_USED', 'ACTIVATION_REVOKED', 'ACTIVATION_EXPIRED',
        'PASSWORD_RESET_REQUESTED', 'PASSWORD_RESET_USED', 'PASSWORD_RESET_REVOKED',
        'REFRESH_FAMILY_REVOKED', 'REFRESH_TOKEN_REUSE_DETECTED',
        'ACCOUNT_DEACTIVATED', 'ACCOUNT_REACTIVATED', 'EMAIL_DELIVERY_ATTEMPT', 'EMAIL_DELIVERY_SUCCEEDED', 'EMAIL_DELIVERY_FAILED')),
    occurred_at TIMESTAMP NOT NULL,
    delivery_status VARCHAR(30),
    details VARCHAR(1000)
);
CREATE INDEX idx_security_event_utilisateur ON security_event(utilisateur_id, occurred_at DESC);
