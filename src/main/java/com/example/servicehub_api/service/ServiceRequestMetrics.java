package com.example.servicehub_api.service;

import com.example.servicehub_api.model.RequestStatus;
import com.example.servicehub_api.model.RequestTransitioned;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.EnumMap;
import java.util.Map;

@Component
public class ServiceRequestMetrics {
    public static final String TRANSITIONS_METRIC = "servicehub.service_requests.transitions";
    private static final Logger log = LoggerFactory.getLogger(ServiceRequestMetrics.class);
    private final Map<RequestStatus, Counter> counters = new EnumMap<>(RequestStatus.class);

    public ServiceRequestMetrics(MeterRegistry registry) {
        for (RequestStatus status : RequestStatus.values()) {
            counters.put(status, Counter.builder(TRANSITIONS_METRIC)
                    .description("Criações e transições de ordens confirmadas no banco")
                    .tag("to", status.name()).register(registry));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void countCommittedTransition(RequestTransitioned event) {
        counters.get(event.to()).increment();
        log.info("Transição de ordem confirmada: to={}", event.to());
    }
}
