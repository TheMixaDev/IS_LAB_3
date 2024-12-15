package net.alephdev.lab3.dto;

import lombok.Getter;
import lombok.Setter;
import net.alephdev.lab3.enums.MusicGenre;
import net.alephdev.lab3.models.Album;
import net.alephdev.lab3.models.Coordinates;
import net.alephdev.lab3.models.Label;

import java.time.LocalDate;


@Getter
@Setter
public class FullMusicBand {
    MusicBandRequestDto musicBandRequestDto;
    Album album = null;
    Label label = null;
    Coordinates coordinates;

    public FullMusicBand(String[] values) {
        for(int i = 0; i < values.length; i++) {
            if(values[i] != null) {
                values[i] = values[i].trim();
                if(values[i].isEmpty())
                    values[i] = null;
            }
        }
        if(values[7] != null && values[8] != null && values[9] != null) {
            this.album = Album.builder()
                    .name(values[7])
                    .length(Long.parseLong(values[8]))
                    .sales(Float.parseFloat(values[9]))
                    .build();
        }

        if(values[12] != null) {
            this.label = Label.builder()
                    .sales(Float.parseFloat(values[12]))
                    .build();
        }

        this.coordinates = Coordinates.builder()
                .x(Float.parseFloat(values[1]))
                .y(Float.parseFloat(values[2]))
                .build();

        this.musicBandRequestDto = MusicBandRequestDto.builder()
                .name(values[0])
                .genre(MusicGenre.valueOf(values[3]))
                .numberOfParticipants(Long.parseLong(values[4]))
                .description(values[6])
                .albumsCount(Integer.parseInt(values[10]))
                .establishmentDate(LocalDate.parse(values[11]))
                .build();
        if(values[5] != null) {
            this.musicBandRequestDto.setSinglesCount(Integer.parseInt(values[5]));
        }
    }
}
