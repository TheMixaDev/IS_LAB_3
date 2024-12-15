package net.alephdev.lab3.service;

import io.minio.GetObjectResponse;
import io.minio.StatObjectResponse;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import net.alephdev.lab3.dto.InputStreamWithSize;
import net.alephdev.lab3.enums.Role;
import net.alephdev.lab3.models.ImportHistory;
import net.alephdev.lab3.models.User;
import net.alephdev.lab3.repository.ImportHistoryRepository;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportHistoryService {
    private final ImportHistoryRepository importHistoryRepository;
    private final UserService userService;
    private final MinioService minioService;

    public ImportHistory createImportRecord(ImportHistory importHistory) {
        return importHistoryRepository.save(importHistory);
    }

    public List<ImportHistory> getImportHistory(User user) {
        if(user.getRole() == Role.ADMIN) {
            return importHistoryRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        }
        return importHistoryRepository.findByCreatedBy(user);
    }

    public InputStreamWithSize downloadFile(Long id, User user) {
        ImportHistory importHistory = importHistoryRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Импорт не найден")
        );
        try {
            if (user.getRole() == Role.ADMIN || (importHistory != null && importHistory.getCreatedBy().equals(user))) {
                String filename = importHistory.getFilename();
                if (filename == null) {
                    throw new IllegalArgumentException("Файл не найден");
                }
                StatObjectResponse stat = minioService.getStat(filename);
                GetObjectResponse object = minioService.getFile(filename);
                return new InputStreamWithSize(new InputStreamResource(object), stat.size());
            }
        } catch (MinioException e) {
            throw new IllegalArgumentException("Файл не найден");
        }
        throw new IllegalArgumentException("Импорт не найден");
    }
}
