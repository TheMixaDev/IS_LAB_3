package net.alephdev.lab3.repository;

import net.alephdev.lab3.dto.MusicBandShortDto;
import net.alephdev.lab3.enums.MusicGenre;
import net.alephdev.lab3.models.Album;
import net.alephdev.lab3.models.Coordinates;
import net.alephdev.lab3.models.Label;
import net.alephdev.lab3.models.MusicBand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MusicBandRepository extends JpaRepository<MusicBand, Long> {

    List<MusicBand> findAllByDescriptionContainingIgnoreCase(String description);

    long countAllByEstablishmentDate(LocalDate establishmentDate);

    long countAllByLabelSalesGreaterThan(float sales);

    List<MusicBand> findAllByNameStartingWithIgnoreCase(String prefix);

    List<MusicBand> findAllByGenreOrderByLabel_SalesDesc(MusicGenre genre);

    long countAllByCoordinates(Coordinates coordinates);

    long countAllByBestAlbum(Album album);

    long countAllByLabel(Label label);

    //@Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MusicBand> findByName(String name);

    Optional<MusicBand> findByBestAlbum(Album bestAlbum);

    Optional<MusicBand> findByLabel(Label label);

    @Query("SELECT m FROM MusicBand m WHERE m.name LIKE %:search% OR m.description LIKE %:search%")
    Page<MusicBand> findAllBy(String search, Pageable pageable);

    @Query("SELECT new net.alephdev.lab3.dto.MusicBandShortDto(m.id, m.name, m.genre, m.coordinates, m.createdBy) FROM MusicBand m")
    List<MusicBandShortDto> findAllShort();
}