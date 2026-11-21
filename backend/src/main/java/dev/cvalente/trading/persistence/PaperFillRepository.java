package dev.cvalente.trading.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PaperFillRepository extends JpaRepository<PaperFillEntity, Long> {}
