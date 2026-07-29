package fr.afpa.gestioneleves.service;

import fr.afpa.gestioneleves.dto.response.MoyennesResponse;
import fr.afpa.gestioneleves.entity.Note;
import fr.afpa.gestioneleves.enumtype.PeriodeBulletin;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class CalculMoyenneService {

    public MoyennesResponse calculer(Long inscriptionId, PeriodeBulletin periode, List<Note> notes) {
        var groupes = new LinkedHashMap<Long, List<Note>>();
        notes.stream().collect(java.util.stream.Collectors.groupingBy(
                n -> n.getEnseignement().getMatiere().getId(), LinkedHashMap::new, java.util.stream.Collectors.toList()))
                .forEach(groupes::put);

        var matieres = groupes.values().stream().map(groupe -> {
            Note premiere = groupe.getFirst();
            BigDecimal somme = groupe.stream()
                    .map(n -> n.getValeur().multiply(n.getCoefficient()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal coefficients = groupe.stream().map(Note::getCoefficient).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal moyenne = somme.divide(coefficients, 8, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
            return new MoyennesResponse.Matiere(premiere.getEnseignement().getMatiere().getId(),
                    premiere.getEnseignement().getMatiere().getNom(), moyenne,
                    premiere.getEnseignement().getCoefficientMatiere(), groupe.size());
        }).toList();

        if (matieres.isEmpty()) {
            return new MoyennesResponse(inscriptionId, periode, null, List.of());
        }
        BigDecimal sommePonderee = matieres.stream()
                .map(m -> m.moyenne().multiply(m.coefficient()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sommeCoefficients = matieres.stream()
                .map(MoyennesResponse.Matiere::coefficient).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal generale = sommePonderee.divide(sommeCoefficients, 8, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
        return new MoyennesResponse(inscriptionId, periode, generale, matieres);
    }
}
