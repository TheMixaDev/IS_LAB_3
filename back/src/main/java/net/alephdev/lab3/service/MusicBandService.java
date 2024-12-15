package net.alephdev.lab3.service;

import com.opencsv.CSVReader;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import net.alephdev.lab3.WebSocketHandler;
import net.alephdev.lab3.dto.FullMusicBand;
import net.alephdev.lab3.dto.MusicBandRequestDto;
import net.alephdev.lab3.dto.MusicBandShortDto;
import net.alephdev.lab3.enums.EventType;
import net.alephdev.lab3.enums.ImportStatus;
import net.alephdev.lab3.enums.MusicGenre;
import net.alephdev.lab3.enums.Role;
import net.alephdev.lab3.exception.ImportErrorException;
import net.alephdev.lab3.models.*;
import net.alephdev.lab3.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class MusicBandService {

    private final MusicBandRepository musicBandRepository;
    private final CoordinatesRepository coordinatesRepository;
    private final AlbumRepository albumRepository;
    private final LabelRepository labelRepository;
    private final EventRepository eventRepository;
    private final WebSocketHandler webSocketHandler;

    private final CoordinatesService coordinatesService;
    private final AlbumService albumService;
    private final LabelService labelService;
    private final ImportHistoryService importHistoryService;
    private final MinioService minioService;

    private void createEvent(MusicBand musicBand, User user, EventType eventType, String description) {
        Event event = new Event();
        event.setMusicBand(musicBand);
        event.setUser(user);
        event.setEventType(eventType);
        event.setDescription(description);
        eventRepository.save(event);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MusicBand createMusicBand(MusicBandRequestDto musicBandRequestDto, User user, boolean inImport) {
        Coordinates coordinates = coordinatesRepository.findById(musicBandRequestDto.getCoordinatesId())
                .orElseThrow(() -> new IllegalArgumentException("Координаты не найдены"));

        Album bestAlbum = musicBandRequestDto.getBestAlbumId() != null
                ? albumRepository.findById(musicBandRequestDto.getBestAlbumId())
                .orElseThrow(() -> new IllegalArgumentException("Альбом не найден"))
                : null;

        Label label = musicBandRequestDto.getLabelId() != null
                ? labelRepository.findById(musicBandRequestDto.getLabelId())
                .orElseThrow(() -> new IllegalArgumentException("Лейбл не найден"))
                : null;

        if (musicBandRepository.findByName(musicBandRequestDto.getName()).isPresent()) {
            throw new IllegalArgumentException("Группа с названием " + musicBandRequestDto.getName() + " уже существует");
        }

        if(bestAlbum != null && musicBandRepository.findByBestAlbum(bestAlbum).isPresent()) {
            throw new IllegalArgumentException("Группа с лучшим альбомом " + bestAlbum.getName() + " уже существует");
        }

        if(label != null && musicBandRepository.findByLabel(label).isPresent()) {
            throw new IllegalArgumentException("Группа с таким лейблом уже существует");
        }

        MusicBand musicBand = new MusicBand();
        musicBand.setName(musicBandRequestDto.getName());
        musicBand.setCoordinates(coordinates);
        musicBand.setGenre(musicBandRequestDto.getGenre());
        musicBand.setNumberOfParticipants(musicBandRequestDto.getNumberOfParticipants());
        musicBand.setSinglesCount(musicBandRequestDto.getSinglesCount());
        musicBand.setDescription(musicBandRequestDto.getDescription());
        musicBand.setBestAlbum(bestAlbum);
        musicBand.setAlbumsCount(musicBandRequestDto.getAlbumsCount());
        musicBand.setEstablishmentDate(musicBandRequestDto.getEstablishmentDate());
        musicBand.setLabel(label);
        musicBand.setCreatedBy(user);
        MusicBand savedMusicBand = musicBandRepository.save(musicBand);
        webSocketHandler.notifyClients(EventType.CREATE, savedMusicBand.getId());

        createEvent(savedMusicBand, user, EventType.CREATE, "Группа создана");
        return savedMusicBand;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = ImportErrorException.class)
    public void importMusicBands(MultipartFile file, CSVReader csv, User user) {
        String filename = null;
        try {
            String[] values = null;
            int count = 0;
            while ((values = csv.readNext()) != null) {
                FullMusicBand fullMusicBand = new FullMusicBand(values);
                Coordinates coordinates = coordinatesService.createCoordinates(fullMusicBand.getCoordinates());
                fullMusicBand.getMusicBandRequestDto().setCoordinatesId(coordinates.getId());
                if (fullMusicBand.getAlbum() != null) {
                    Album album = albumService.createAlbum(fullMusicBand.getAlbum());
                    fullMusicBand.getMusicBandRequestDto().setBestAlbumId(album.getId());
                }
                if (fullMusicBand.getLabel() != null) {
                    Label label = labelService.createLabel(fullMusicBand.getLabel());
                    fullMusicBand.getMusicBandRequestDto().setLabelId(label.getId());
                }
                createMusicBand(fullMusicBand.getMusicBandRequestDto(), user, true);
                count++;
            }
            filename = minioService.uploadFile(file);
            importHistoryService.createImportRecord(
                    ImportHistory.builder()
                            .status(ImportStatus.SUCCESS)
                            .count(count)
                            .createdBy(user)
                            .filename(filename)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            try {
                if (filename != null)
                    minioService.deleteFile(filename);
            } catch (Exception ignored) {}
            throw new ImportErrorException(e.getMessage(), user);
        } catch (MinioException e) {
            throw new ImportErrorException("Не удалось загрузить файл в хранилище", user);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (filename != null)
                    minioService.deleteFile(filename);
            } catch (Exception ignored) {}
            throw new ImportErrorException("Предоставлен файл с некорректными данными", user);
        }
    }

    public Page<MusicBand> getAllMusicBands(int page, String search, String sortColumn, String sortDirection) {
        return musicBandRepository.findAllBy(search, PageRequest.of(page, 20, Sort.by(Sort.Direction.fromString(sortDirection), sortColumn)));
    }

    public List<MusicBandShortDto> getAllShort() {
        return musicBandRepository.findAllShort();
    }

    public MusicBand getMusicBandById(Long id) {
        return musicBandRepository.findById(id).orElseThrow();
    }
    @Transactional
    public MusicBand updateMusicBand(Long id, MusicBandRequestDto updatedMusicBand, User user) {
        MusicBand existingMusicBand = musicBandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Группа не найдена"));

        if (!existingMusicBand.getCreatedBy().equals(user) && !user.getRole().name().equals("ADMIN")) {
            throw new AccessDeniedException("Вы не можете редактировать эту группу");
        }

        if(!existingMusicBand.getName().equals(updatedMusicBand.getName())) {
            if(musicBandRepository.findByName(updatedMusicBand.getName()).isPresent()) {
                throw new IllegalArgumentException("Группа с названием " + updatedMusicBand.getName() + " уже существует");
            }
        }

        if(updatedMusicBand.getBestAlbumId() != null && existingMusicBand.getBestAlbum() != null && !updatedMusicBand.getBestAlbumId().equals(existingMusicBand.getBestAlbum().getId())) {
            if(musicBandRepository.findByBestAlbum(albumRepository.findById(updatedMusicBand.getBestAlbumId()).orElseThrow()).isPresent()) {
                throw new IllegalArgumentException("Группа с лучшим альбомом " + albumRepository.findById(updatedMusicBand.getBestAlbumId()).orElseThrow().getName() + " уже существует");
            }
        }

        if(updatedMusicBand.getLabelId() != null && existingMusicBand.getLabel() != null && !updatedMusicBand.getLabelId().equals(existingMusicBand.getLabel().getId())) {
            if(musicBandRepository.findByLabel(labelRepository.findById(updatedMusicBand.getLabelId()).orElseThrow()).isPresent()) {
                throw new IllegalArgumentException("Группа с таким лейблом уже существует");
            }
        }

        existingMusicBand.setName(updatedMusicBand.getName());
        existingMusicBand.setNumberOfParticipants(updatedMusicBand.getNumberOfParticipants());
        existingMusicBand.setSinglesCount(updatedMusicBand.getSinglesCount());
        existingMusicBand.setDescription(updatedMusicBand.getDescription());
        existingMusicBand.setAlbumsCount(updatedMusicBand.getAlbumsCount());
        existingMusicBand.setEstablishmentDate(updatedMusicBand.getEstablishmentDate());
        existingMusicBand.setGenre(updatedMusicBand.getGenre());

        if (updatedMusicBand.getCoordinatesId() != null) {
            Coordinates coordinates = coordinatesRepository.findById(updatedMusicBand.getCoordinatesId())
                    .orElseThrow(() -> new IllegalArgumentException("Координаты не найдены"));
            existingMusicBand.setCoordinates(coordinates);
        }

        if (updatedMusicBand.getBestAlbumId() != null) {
            Album album = albumRepository.findById(updatedMusicBand.getBestAlbumId())
                    .orElseThrow(() -> new IllegalArgumentException("Альбом не найден"));
            existingMusicBand.setBestAlbum(album);
        }

        if (updatedMusicBand.getLabelId() != null) {
            Label label = labelRepository.findById(updatedMusicBand.getLabelId())
                    .orElseThrow(() -> new IllegalArgumentException("Лейбл не найден"));
            existingMusicBand.setLabel(label);
        }

        MusicBand resultMusicBand = musicBandRepository.save(existingMusicBand);
        webSocketHandler.notifyClients(EventType.UPDATE, resultMusicBand.getId());
        createEvent(resultMusicBand, user, EventType.UPDATE, "Группа обновлена");
        return resultMusicBand;
    }
    @Transactional
    public void deleteMusicBand(Long id, User user) {
        MusicBand musicBand = musicBandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Группа не найдена"));

        if (!musicBand.getCreatedBy().equals(user) && !user.getRole().name().equals("ADMIN")) {
            throw new AccessDeniedException("Вы не можете удалить эту группу");
        }

        String deletedMusicBandName = musicBand.getName();

        eventRepository.findAllByMusicBand(musicBand).forEach(event -> {
            event.setMusicBand(null);
            event.setDescription(event.getDescription() + " (группа \"" + deletedMusicBandName + "\" была удалена)");
            eventRepository.save(event);
        });

        createEvent(null, user, EventType.DELETE, "Группа \"" + deletedMusicBandName + "\" удалена");

        musicBandRepository.delete(musicBand);
        webSocketHandler.notifyClients(EventType.DELETE, musicBand.getId());

        deleteUnrelatedCoordinates(musicBand.getCoordinates());
        deleteUnrelatedAlbum(musicBand.getBestAlbum());
        deleteUnrelatedLabel(musicBand.getLabel());
    }

    private void deleteUnrelatedCoordinates(Coordinates coordinates) {
        if (coordinates != null && musicBandRepository.countAllByCoordinates(coordinates) == 0) {
            coordinatesRepository.delete(coordinates);
        }
    }

    private void deleteUnrelatedAlbum(Album album) {
        if (album != null && musicBandRepository.countAllByBestAlbum(album) == 0) {
            albumRepository.delete(album);
        }
    }

    private void deleteUnrelatedLabel(Label label) {
        if (label != null && musicBandRepository.countAllByLabel(label) == 0) {
            labelRepository.delete(label);
        }
    }


    public long countByEstablishmentDate(LocalDate establishmentDate) {
        return musicBandRepository.countAllByEstablishmentDate(establishmentDate);
    }

    public long countByLabelSalesGreaterThan(float sales) {
        return musicBandRepository.countAllByLabelSalesGreaterThan(sales);
    }

    public List<MusicBand> findByNameStartingWith(String prefix) {
        return musicBandRepository.findAllByNameStartingWithIgnoreCase(prefix);
    }

    @Transactional
    public void addSingleToMusicBand(Long id, User user) {
        MusicBand musicBand = musicBandRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Группа не найдена"));
        if(user.getRole() == Role.USER && musicBand.getCreatedBy().getId() != user.getId()) {
            throw new AccessDeniedException("Группа вам не принадлежит");
        }
        if (musicBand.getSinglesCount() == null) {
            musicBand.setSinglesCount(1);
        } else {
            musicBand.setSinglesCount(musicBand.getSinglesCount() + 1);
        }
        webSocketHandler.notifyClients(EventType.UPDATE, musicBand.getId());
        createEvent(musicBand, user, EventType.UPDATE, "Добавлен сингл");
        musicBandRepository.save(musicBand);
    }

    @Transactional
    public void rewardBestMusicBand(MusicGenre genre, User user) {
        if(user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Нет прав для награждения");
        }
        List<MusicBand> bestMusicBands = musicBandRepository.findAllByGenreOrderByLabel_SalesDesc(genre);
        if (!bestMusicBands.isEmpty()) {
            MusicBand bestMusicBand = bestMusicBands.get(0);
            String description = bestMusicBand.getDescription();
            if (description == null) {
                description = "";
            }
            bestMusicBand.setDescription(description + "\n Награждена как лучшая в жанре " + genre.getName() + "!");
            musicBandRepository.save(bestMusicBand);
            webSocketHandler.notifyClients(EventType.REWARD, bestMusicBand.getId());
            createEvent(bestMusicBand, bestMusicBand.getCreatedBy(), EventType.REWARD,
                    "Группа награждена как лучшая в жанре " + genre.getName());
        } else {
            throw new NoSuchElementException();
        }
    }
}