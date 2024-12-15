package net.alephdev.lab3.service;

import lombok.RequiredArgsConstructor;
import net.alephdev.lab3.models.Coordinates;
import net.alephdev.lab3.repository.CoordinatesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CoordinatesService {

    private final CoordinatesRepository coordinatesRepository;

    @Transactional
    public Coordinates createCoordinates(Coordinates coordinates) {
        return coordinatesRepository.save(coordinates);
    }

    public Coordinates getCoordinatesById(Long id) {
        return coordinatesRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Координаты не найдены"));
    }
    public List<Coordinates> getAllCoordinates() {
        return coordinatesRepository.findAll();
    }

    @Transactional
    public Coordinates updateCoordinates(Long id, Coordinates updatedCoordinates) {
        Coordinates existingCoordinates = coordinatesRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Координаты не найдены"));

        existingCoordinates.setX(updatedCoordinates.getX());
        existingCoordinates.setY(updatedCoordinates.getY());

        return coordinatesRepository.save(existingCoordinates);
    }

    @Transactional
    public void deleteCoordinates(Long id) {
        coordinatesRepository.deleteById(id);
    }
}