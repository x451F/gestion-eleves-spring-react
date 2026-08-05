package fr.afpa.gestioneleves.enumtype;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum StatutInscription {
    EN_COURS,
    TERMINEE,
    ANNULEE;

    @JsonCreator
    public static StatutInscription fromJson(String value) {
        return "ACTIVE".equals(value) ? EN_COURS : valueOf(value);
    }
}
