package net.alephdev.lab3.repository;

import net.alephdev.lab3.models.Coordinates;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoordinatesRepository extends JpaRepository<Coordinates, Long> {
}