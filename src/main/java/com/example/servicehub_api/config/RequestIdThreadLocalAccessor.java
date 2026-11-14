package com.example.servicehub_api.config;

import io.micrometer.context.ThreadLocalAccessor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;

/** Propaga somente o identificador seguro para a execução assíncrona do GraphQL. */
public class RequestIdThreadLocalAccessor implements ThreadLocalAccessor<String> {
    @Override
    public @NonNull Object key() { return RequestIdThreadLocalAccessor.class.getName(); }

    @Override
    public @Nullable String getValue() { return MDC.get(RequestIdFilter.MDC_KEY); }

    @Override
    public void setValue(@NonNull String value) { MDC.put(RequestIdFilter.MDC_KEY, value); }

    @Override
    public void setValue() { MDC.remove(RequestIdFilter.MDC_KEY); }
}
