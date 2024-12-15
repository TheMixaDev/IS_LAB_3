package net.alephdev.lab3.service;

import lombok.RequiredArgsConstructor;
import net.alephdev.lab3.models.Label;
import net.alephdev.lab3.repository.LabelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LabelService {

    private final LabelRepository labelRepository;

    @Transactional
    public Label createLabel(Label label) {
        return labelRepository.save(label);
    }

    public Label getLabelById(Long id) {
        return labelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Лейбл не найден"));
    }

    @Transactional
    public Label updateLabel(Long id, Label updatedLabel) {
        Label existingLabel = labelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Лейбл не найден"));

        existingLabel.setSales(updatedLabel.getSales());

        return labelRepository.save(existingLabel);
    }

    @Transactional
    public void deleteLabel(Long id) {
        labelRepository.deleteById(id);
    }

    public List<Label> getAllLabels() {
        return labelRepository.findAll();
    }
}