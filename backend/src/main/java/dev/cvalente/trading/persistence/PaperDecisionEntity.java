package dev.cvalente.trading.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "paper_decisions", indexes = @Index(name = "idx_paper_decisions_time", columnList = "decisionTime"))
public class PaperDecisionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public Instant decisionTime;
    @Column(nullable = false, length = 20) public String symbol;
    @Column(nullable = false, length = 20) public String strategy;
    @Column(nullable = false, length = 30) public String action;
    public int score;
    @Column(nullable = false, length = 1000) public String reasons;
    public PaperDecisionEntity() {}
}
