package com.example.servicehub_api.model;

/** Evento interno sem dados pessoais, publicado dentro da transação da ordem. */
public record RequestTransitioned(RequestStatus to) {}
