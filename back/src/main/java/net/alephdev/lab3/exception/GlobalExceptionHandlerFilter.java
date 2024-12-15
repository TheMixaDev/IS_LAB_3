package net.alephdev.lab3.exception;

import net.alephdev.lab3.dto.MessageDto;
import net.alephdev.lab3.enums.ImportStatus;
import net.alephdev.lab3.models.ImportHistory;
import net.alephdev.lab3.repository.ImportHistoryRepository;
import net.alephdev.lab3.service.MinioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandlerFilter {
    private final ImportHistoryRepository importHistoryRepository;
    private final MinioService minioService;

    public GlobalExceptionHandlerFilter(ImportHistoryRepository importHistoryRepository, MinioService minioService) {
        this.importHistoryRepository = importHistoryRepository;
        this.minioService = minioService;
    }

    @ExceptionHandler(ImportErrorException.class)
    public ResponseEntity<MessageDto> handleImportErrorException(ImportErrorException ex) {
        try {
            importHistoryRepository.save(
                ImportHistory.builder()
                    .status(ImportStatus.ERROR)
                    .createdBy(ex.getUser())
                    .count(0)
                    .build()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.badRequest().body(new MessageDto(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageDto> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new MessageDto(ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNoResourceFoundException(NoResourceFoundException ex) {
        return "forward:/";
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageDto> handleException(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.badRequest().body(new MessageDto("Предоставлены неверные данные"));
    }
}
