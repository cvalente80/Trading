package dev.cvalente.trading.persistence;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="paper_accounts") public class PaperAccountEntity {
    @Id public Long id;
    @Column(nullable=false, precision=30, scale=12) public BigDecimal cash;
    public PaperAccountEntity() {}
}
