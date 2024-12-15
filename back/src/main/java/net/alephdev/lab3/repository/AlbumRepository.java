package net.alephdev.lab3.repository;

import net.alephdev.lab3.models.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {
    Optional<List<Album>> findByName(String name);
}