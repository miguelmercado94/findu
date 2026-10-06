package com.findu.notification.processor.domain.service;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FinancialEventDeduplicationService {

    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    public boolean isDuplicate(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        return !processedEventIds.add(eventId);
    }

    public void clear() {
        processedEventIds.clear();
    }
}
