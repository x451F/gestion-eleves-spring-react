CREATE OR REPLACE FUNCTION assert_utilisateur_profile_invariant_for(account_id BIGINT) RETURNS void AS $$
DECLARE
    account_role VARCHAR(30);
    teacher_count INTEGER;
    guardian_count INTEGER;
BEGIN
    IF account_id IS NULL THEN RETURN; END IF;
    SELECT role INTO account_role FROM utilisateur WHERE id = account_id;
    IF NOT FOUND THEN RETURN; END IF;

    SELECT count(*) INTO teacher_count FROM enseignant WHERE utilisateur_id = account_id;
    SELECT count(*) INTO guardian_count FROM responsable WHERE utilisateur_id = account_id;
    IF (account_role = 'ADMIN' AND (teacher_count <> 0 OR guardian_count <> 0))
       OR (account_role = 'ENSEIGNANT' AND (teacher_count <> 1 OR guardian_count <> 0))
       OR (account_role = 'RESPONSABLE' AND (guardian_count <> 1 OR teacher_count <> 0)) THEN
        RAISE EXCEPTION 'Account role/profile invariant violated for utilisateur %', account_id
            USING ERRCODE = '23514';
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION assert_utilisateur_profile_invariant() RETURNS trigger AS $$
BEGIN
    IF TG_TABLE_NAME = 'utilisateur' THEN
        PERFORM assert_utilisateur_profile_invariant_for(COALESCE(NEW.id, OLD.id));
    ELSIF TG_OP = 'INSERT' THEN
        PERFORM assert_utilisateur_profile_invariant_for(NEW.utilisateur_id);
    ELSIF TG_OP = 'DELETE' THEN
        PERFORM assert_utilisateur_profile_invariant_for(OLD.utilisateur_id);
    ELSE
        PERFORM assert_utilisateur_profile_invariant_for(OLD.utilisateur_id);
        IF NEW.utilisateur_id IS DISTINCT FROM OLD.utilisateur_id THEN
            PERFORM assert_utilisateur_profile_invariant_for(NEW.utilisateur_id);
        END IF;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION reject_utilisateur_role_change() RETURNS trigger AS $$
BEGIN
    IF NEW.role IS DISTINCT FROM OLD.role THEN
        RAISE EXCEPTION 'Utilisateur role is immutable for utilisateur %', OLD.id
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_utilisateur_role_immutable
BEFORE UPDATE OF role ON utilisateur
FOR EACH ROW EXECUTE FUNCTION reject_utilisateur_role_change();

CREATE CONSTRAINT TRIGGER ct_utilisateur_profile_invariant
AFTER INSERT OR UPDATE OR DELETE ON utilisateur
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION assert_utilisateur_profile_invariant();

CREATE CONSTRAINT TRIGGER ct_enseignant_profile_invariant
AFTER INSERT OR UPDATE OR DELETE ON enseignant
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION assert_utilisateur_profile_invariant();

CREATE CONSTRAINT TRIGGER ct_responsable_profile_invariant
AFTER INSERT OR UPDATE OR DELETE ON responsable
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION assert_utilisateur_profile_invariant();
