package it.pintux.life.essentialsaddon.provider;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.LongSupplier;

final class PendingRequestLog {

    private record Entry(String senderName, long receivedAt) {
    }

    private final long ttlMillis;
    private final LongSupplier clock;
    private final Map<String, Deque<Entry>> byRecipient = new ConcurrentHashMap<>();

    PendingRequestLog(long ttlMillis, LongSupplier clock) {
        this.ttlMillis = ttlMillis;
        this.clock = clock;
    }

    void record(String recipientName, String senderName) {
        if (recipientName == null || senderName == null) return;
        Deque<Entry> queue = byRecipient.computeIfAbsent(key(recipientName), ignored -> new ConcurrentLinkedDeque<>());
        queue.removeIf(entry -> entry.senderName().equalsIgnoreCase(senderName));
        queue.addLast(new Entry(senderName, clock.getAsLong()));
        sweep();
    }

    List<String> senders(String recipientName) {
        if (recipientName == null) return List.of();
        Deque<Entry> queue = byRecipient.get(key(recipientName));
        if (queue == null) return List.of();
        prune(queue);
        List<String> names = new ArrayList<>();
        for (Entry entry : queue) {
            names.add(0, entry.senderName());
        }
        return names;
    }

    String newestSender(String recipientName) {
        List<String> senders = senders(recipientName);
        return senders.isEmpty() ? null : senders.get(0);
    }

    void forget(String recipientName, String senderName) {
        if (recipientName == null) return;
        String key = key(recipientName);
        Deque<Entry> queue = byRecipient.get(key);
        if (queue == null) return;
        if (senderName == null) {
            queue.clear();
        } else {
            queue.removeIf(entry -> entry.senderName().equalsIgnoreCase(senderName));
        }
        if (queue.isEmpty()) {
            byRecipient.remove(key);
        }
    }

    int trackedRecipients() {
        return byRecipient.size();
    }

    private void sweep() {
        Iterator<Map.Entry<String, Deque<Entry>>> iterator = byRecipient.entrySet().iterator();
        while (iterator.hasNext()) {
            Deque<Entry> queue = iterator.next().getValue();
            prune(queue);
            if (queue.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private void prune(Deque<Entry> queue) {
        long cutoff = clock.getAsLong() - ttlMillis;
        queue.removeIf(entry -> entry.receivedAt() < cutoff);
    }

    private String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
