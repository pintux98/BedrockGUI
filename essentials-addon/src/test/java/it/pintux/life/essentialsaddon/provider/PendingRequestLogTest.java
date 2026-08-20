package it.pintux.life.essentialsaddon.provider;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingRequestLogTest {

    private final AtomicLong now = new AtomicLong(1_000L);
    private final PendingRequestLog log = new PendingRequestLog(60_000L, now::get);

    @Test
    void newestRequestIsAnsweredFirst() {
        log.record("Steve", "Alex");
        now.addAndGet(1_000L);
        log.record("Steve", "Herobrine");

        assertEquals(List.of("Herobrine", "Alex"), log.senders("Steve"));
        assertEquals("Herobrine", log.newestSender("Steve"));
    }

    @Test
    void recipientLookupIgnoresCase() {
        log.record("Steve", "Alex");
        assertEquals(List.of("Alex"), log.senders("sTeVe"));
    }

    @Test
    void repeatedRequestFromSameSenderRefreshesInsteadOfDuplicating() {
        log.record("Steve", "Alex");
        now.addAndGet(30_000L);
        log.record("Steve", "alex");

        assertEquals(List.of("alex"), log.senders("Steve"));

        now.addAndGet(40_000L);
        assertEquals(List.of("alex"), log.senders("Steve"));
    }

    @Test
    void requestsExpireAfterTheTtl() {
        log.record("Steve", "Alex");
        now.addAndGet(60_001L);

        assertEquals(List.of(), log.senders("Steve"));
        assertNull(log.newestSender("Steve"));
    }

    @Test
    void forgettingOneSenderLeavesTheOthers() {
        log.record("Steve", "Alex");
        log.record("Steve", "Herobrine");

        log.forget("Steve", "alex");

        assertEquals(List.of("Herobrine"), log.senders("Steve"));
    }

    @Test
    void forgettingWithoutASenderClearsTheRecipient() {
        log.record("Steve", "Alex");
        log.record("Steve", "Herobrine");

        log.forget("Steve", null);

        assertEquals(List.of(), log.senders("Steve"));
        assertEquals(0, log.trackedRecipients());
    }

    @Test
    void expiredRecipientsAreSweptSoOfflinePlayersDoNotAccumulate() {
        log.record("Steve", "Alex");
        log.record("Notch", "Alex");
        assertEquals(2, log.trackedRecipients());

        now.addAndGet(60_001L);
        log.record("Herobrine", "Alex");

        assertEquals(1, log.trackedRecipients());
        assertEquals(List.of("Alex"), log.senders("Herobrine"));
    }

    @Test
    void unknownRecipientAndNullArgumentsAreSafe() {
        assertEquals(List.of(), log.senders("Nobody"));
        assertEquals(List.of(), log.senders(null));
        assertNull(log.newestSender("Nobody"));
        log.record(null, "Alex");
        log.record("Steve", null);
        log.forget(null, null);
        log.forget("Nobody", "Alex");
        assertTrue(log.senders("Steve").isEmpty());
        assertEquals(0, log.trackedRecipients());
    }
}
