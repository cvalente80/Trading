package dev.cvalente.trading.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PaperPositionRepository extends JpaRepository<PaperPositionEntity, String> {}
