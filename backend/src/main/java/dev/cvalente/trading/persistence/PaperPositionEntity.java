package dev.cvalente.trading.persistence;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="paper_positions") public class PaperPositionEntity {
    @Id public String symbol; @Column(nullable=false, precision=30, scale=12) public BigDecimal quantity;
    @Column(nullable=false, precision=30, scale=12) public BigDecimal averageEntry;
    @Column(nullable=false, precision=30, scale=12) public BigDecimal markPrice;
    @Column(nullable=false, precision=30, scale=12) public BigDecimal realizedPnl;
    public PaperPositionEntity() {}
}
