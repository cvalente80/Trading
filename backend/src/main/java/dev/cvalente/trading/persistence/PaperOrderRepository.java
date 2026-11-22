package dev.cvalente.trading.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PaperOrderRepository extends JpaRepository<PaperOrderEntity, Long> {}
