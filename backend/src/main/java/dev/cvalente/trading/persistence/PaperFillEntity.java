package dev.cvalente.trading.persistence;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="paper_fills") public class PaperFillEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @Column(nullable=false) public Long orderId;
    @Column(nullable=false) public String symbol; @Column(nullable=false) public String side;
    @Column(nullable=false, precision=30, scale=12) public BigDecimal quantity;
    @Column(nullable=false, precision=30, scale=12) public BigDecimal price;
    @Column(nullable=false, precision=30, scale=12) public BigDecimal fee; @Column(nullable=false) public Instant fillTime;
    public PaperFillEntity() {}
}
