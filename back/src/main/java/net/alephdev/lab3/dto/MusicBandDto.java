package net.alephdev.lab3.dto;

import lombok.Getter;
import lombok.Setter;
import net.alephdev.lab3.enums.MusicGenre;
import net.alephdev.lab3.models.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class MusicBandDto {
    private Long id;
    private String name;
    private Coordinates coordinates;
    private LocalDateTime creationDate;
    private MusicGenre genre;
    private long numberOfParticipants;
    private Integer singlesCount;
    private String description;
    private Album bestAlbum;
    private int albumsCount;
    private LocalDate establishmentDate;
    private Label label;
    private UserAuthorizedDto createdBy;
}
