package net.alephdev.lab3.repository;

import net.alephdev.lab3.models.ImportHistory;
import net.alephdev.lab3.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportHistoryRepository extends JpaRepository<ImportHistory, Long> {
    List<ImportHistory> findByCreatedBy(User user);
}
