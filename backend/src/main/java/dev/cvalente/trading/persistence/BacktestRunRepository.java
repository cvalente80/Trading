package dev.cvalente.trading.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BacktestRunRepository extends JpaRepository<BacktestRunEntity,Long> {
    List<BacktestRunEntity> findTop50ByOrderByCreatedAtDescIdDesc();
}
