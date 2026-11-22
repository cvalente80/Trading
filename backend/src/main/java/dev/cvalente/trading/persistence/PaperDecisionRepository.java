package dev.cvalente.trading.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaperDecisionRepository extends JpaRepository<PaperDecisionEntity, Long> {
    List<PaperDecisionEntity> findTop100ByOrderByDecisionTimeDescIdDesc();
}
