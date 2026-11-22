package dev.cvalente.trading.persistence;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="paper_orders") public class PaperOrderEntity {
    @Id public Long id; @Column(nullable=false) public String symbol; @Column(nullable=false) public String side;
    @Column(nullable=false) public String type; @Column(nullable=false, precision=30, scale=12) public BigDecimal quantity;
    public BigDecimal limitPrice; @Column(nullable=false) public Instant createdAt; @Column(nullable=false) public String status;
    public PaperOrderEntity() {}
}
