package net.alephdev.lab3.controller;

import com.opencsv.CSVReader;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.alephdev.lab3.annotation.AuthorizedRequired;
import net.alephdev.lab3.annotation.CurrentUser;
import net.alephdev.lab3.dto.*;
import net.alephdev.lab3.enums.MusicGenre;
import net.alephdev.lab3.exception.ImportErrorException;
import net.alephdev.lab3.models.*;
import net.alephdev.lab3.service.*;
import org.springframework.core.io.InputStreamSource;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/music-bands")
@RequiredArgsConstructor
@AuthorizedRequired
public class MusicBandController {

    private final MusicBandService musicBandService;
    private final ImportHistoryService importHistoryService;
    private final ReentrantLock musicBandLock = new ReentrantLock();

    @PostMapping
    public ResponseEntity<MusicBand> createMusicBand(@Valid @RequestBody MusicBandRequestDto musicBandRequestDto, @CurrentUser User user) {
        musicBandLock.lock();
        try {
            MusicBand result = musicBandService.createMusicBand(musicBandRequestDto, user, false);
            musicBandLock.unlock();
            return new ResponseEntity<>(result, HttpStatus.CREATED);
        } catch (Exception ex) {
            musicBandLock.unlock();
            throw ex;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<MusicBand> updateMusicBand(@PathVariable Long id, @Valid @RequestBody MusicBandRequestDto musicBandRequestDto, @CurrentUser User user) {
        try {
            musicBandLock.lock();
            try {
                MusicBand result = musicBandService.updateMusicBand(id, musicBandRequestDto, user);
                musicBandLock.unlock();
                return new ResponseEntity<>(result, HttpStatus.OK);
            } catch (Exception ex) {
                musicBandLock.unlock();
                throw ex;
            }
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (AccessDeniedException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping
    public ResponseEntity<Page<MusicBand>> getAllMusicBands(
            @RequestParam int page,
            @RequestParam String search,
            @RequestParam String sortColumn,
            @RequestParam String sortDirection
    ) {
        Page<MusicBand> musicBands = musicBandService.getAllMusicBands(page, search, sortColumn, sortDirection);
        /*List<MusicBandDto> musicBandDtos = musicBands.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());*/
        return new ResponseEntity<>(musicBands, HttpStatus.OK);
    }

    private MusicBandDto convertToDto(MusicBand musicBand) {
        MusicBandDto dto = new MusicBandDto();
        dto.setId(musicBand.getId());
        dto.setName(musicBand.getName());
        dto.setCoordinates(musicBand.getCoordinates());
        dto.setCreationDate(musicBand.getCreationDate());
        dto.setGenre(musicBand.getGenre());
        dto.setNumberOfParticipants(musicBand.getNumberOfParticipants());
        dto.setSinglesCount(musicBand.getSinglesCount());
        dto.setDescription(musicBand.getDescription());
        dto.setBestAlbum(musicBand.getBestAlbum());
        dto.setAlbumsCount(musicBand.getAlbumsCount());
        if (musicBand.getEstablishmentDate() != null) {
            dto.setEstablishmentDate(musicBand.getEstablishmentDate());
        }
        dto.setEstablishmentDate(musicBand.getEstablishmentDate());
        dto.setLabel(musicBand.getLabel());
        if (musicBand.getCreatedBy() != null) {
            dto.setCreatedBy(convertToUserDto(musicBand.getCreatedBy()));
        }
        return dto;
    }

    private UserAuthorizedDto convertToUserDto(User user) {
        UserAuthorizedDto dto = new UserAuthorizedDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        return dto;
    }

    @GetMapping("/{id}")
    public ResponseEntity<MusicBandDto> getMusicBandById(@PathVariable Long id) {
        try {
            return new ResponseEntity<>(convertToDto(musicBandService.getMusicBandById(id)), HttpStatus.OK);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMusicBand(@PathVariable Long id, @CurrentUser User user) {
        try {
            musicBandLock.lock();
            try {
                musicBandService.deleteMusicBand(id, user);
                musicBandLock.unlock();
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } catch (Exception ex) {
                musicBandLock.unlock();
                throw ex;
            }
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (AccessDeniedException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/count/establishment-date")
    public ResponseEntity<Long> countByEstablishmentDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate establishmentDate) {
        long count = musicBandService.countByEstablishmentDate(establishmentDate);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/label-sales")
    public ResponseEntity<Long> countByLabelSalesGreaterThan(@RequestParam("sales") float sales) {
        long count = musicBandService.countByLabelSalesGreaterThan(sales);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/name/{prefix}")
    public ResponseEntity<List<MusicBandDto>> findByNameStartingWith(@PathVariable("prefix") String prefix) {
        List<MusicBand> musicBands = musicBandService.findByNameStartingWith(prefix);
        List<MusicBandDto> musicBandDtos = musicBands.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(musicBandDtos);
    }

    @PostMapping("/{id}/add-single")
    public ResponseEntity<Void> addSingleToMusicBand(@PathVariable("id") Long id, @CurrentUser User user) {
        try {
            musicBandService.addSingleToMusicBand(id, user);
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/reward-best/{genre}")
    public ResponseEntity<Void> rewardBestMusicBand(@PathVariable("genre") MusicGenre genre, @CurrentUser User user) {
        try {
            musicBandService.rewardBestMusicBand(genre, user);
            return ResponseEntity.ok().build();
        } catch(NoSuchElementException ex) {
            return ResponseEntity.notFound().build();
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/import")
    public ResponseEntity<Void> importMusicBands(
            @RequestParam("file") MultipartFile file,
            @CurrentUser User user
    ) {
        if(file == null || file.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        musicBandLock.lock();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
            CSVReader csv = new CSVReader(reader);
            musicBandService.importMusicBands(file, csv, user);
            musicBandLock.unlock();
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            musicBandLock.unlock();
            throw new ImportErrorException(e.getMessage(), user);
        } catch (ImportErrorException e) {
            musicBandLock.unlock();
            throw e;
        } catch (Exception e) {
            musicBandLock.unlock();
            throw new ImportErrorException("Предоставлен файл с некорректными данными", user);
        }
    }

    @GetMapping("/import")
    public ResponseEntity<List<ImportHistory>> getImportHistory(@CurrentUser User user) {
        return ResponseEntity.ok(importHistoryService.getImportHistory(user));
    }

    @GetMapping("/import/download/{id}")
    public ResponseEntity<InputStreamSource> downloadFile(@PathVariable("id") Long id, @CurrentUser User user) {
        InputStreamWithSize resource = importHistoryService.downloadFile(id, user);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", id.toString()+".csv");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(resource.getSize())
                .body(resource.getInputStreamSource());
    }


    @GetMapping("/short")
    public ResponseEntity<List<MusicBandShortDto>> getShortMusicBands() {
        return ResponseEntity.ok(musicBandService.getAllShort());
    }
}