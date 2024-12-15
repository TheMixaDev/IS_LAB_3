package net.alephdev.lab3.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.alephdev.lab3.enums.MusicGenre;
import net.alephdev.lab3.models.Coordinates;
import net.alephdev.lab3.models.User;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MusicBandShortDto {
    private Long id;
    private String name;
    private MusicGenre genre;
    private Coordinates coordinates;
    private User createdBy;
}
