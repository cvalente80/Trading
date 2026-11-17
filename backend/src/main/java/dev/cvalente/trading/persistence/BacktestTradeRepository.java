package dev.cvalente.trading.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BacktestTradeRepository extends JpaRepository<BacktestTradeEntity,Long> {
    List<BacktestTradeEntity> findByRunIdOrderByEntryIndexAsc(Long runId);
}
