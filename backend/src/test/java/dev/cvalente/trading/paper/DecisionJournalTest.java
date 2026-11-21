package dev.cvalente.trading.paper;

import dev.cvalente.trading.persistence.PaperDecisionEntity;
import dev.cvalente.trading.persistence.PaperDecisionRepository;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DecisionJournalTest {
    @Test void appendPersistsAnImmutableDecisionEntry() {
        var repository = mock(PaperDecisionRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> {
            PaperDecisionEntity entity = invocation.getArgument(0);
            entity.id = 7L;
            return entity;
        });
        var journal = new DecisionJournal(repository);
        var entry = journal.append(Instant.EPOCH, "BTCUSDT", "DAY", "BUY_CANDIDATE", 80, List.of("EMA", "RSI"));
        assertEquals(7L, entry.id());
        assertEquals("EMA | RSI", entry.reasons());
        verify(repository).save(any(PaperDecisionEntity.class));
        verifyNoMoreInteractions(repository);
    }

    @Test void invalidEntriesAreRejectedBeforePersistence() {
        var repository = mock(PaperDecisionRepository.class);
        var journal = new DecisionJournal(repository);
        assertThrows(IllegalArgumentException.class, () ->
            journal.append(Instant.EPOCH, "", "DAY", "BUY", 1, List.of()));
        verifyNoInteractions(repository);
    }
}
