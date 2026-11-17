package dev.cvalente.trading.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface CandleRepository extends JpaRepository<CandleEntity,Long> {
    List<CandleEntity> findBySymbolAndIntervalAndOpenTimeGreaterThanEqualAndOpenTimeLessThanOrderByOpenTimeAsc(String symbol,String interval,Instant start,Instant end);
    long countBySymbolAndInterval(String symbol,String interval);
}
