#!/usr/bin/env python3
"""Check a running API. --write exercises the full flow with fictitious accounts."""
import argparse
import json
import secrets
import sys
import urllib.error
import urllib.request
import uuid


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--expected-version")
    parser.add_argument("--write", action="store_true", help="Create fictitious accounts and a completed order")
    args = parser.parse_args()
    base = args.base_url.rstrip("/")

    def call(method, path, body=None, token=None, expected=200):
        request_id = "smoke-" + uuid.uuid4().hex
        headers = {"Accept": "application/json", "X-Request-Id": request_id}
        if token:
            headers["Authorization"] = "Bearer " + token
        data = None if body is None else json.dumps(body).encode("utf-8")
        if data is not None:
            headers["Content-Type"] = "application/json"
        request = urllib.request.Request(base + path, data=data, headers=headers, method=method)
        try:
            response = urllib.request.urlopen(request, timeout=15)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            code = response.code
            returned_id = response.headers.get("X-Request-Id")
            content = response.read()
        if returned_id != request_id:
            raise AssertionError(f"{method} {path}: request id was not preserved")
        if code != expected:
            raise AssertionError(f"{method} {path}: expected {expected}, received {code}")
        return json.loads(content) if content else None

    def check(condition, message):
        if not condition:
            raise AssertionError(message)

    health = call("GET", "/actuator/health")
    check(health["status"] == "UP", "Application health is not UP")
    info = call("GET", "/api/v1/info")
    check(info["status"] == "OK", "Versioned info endpoint failed")
    if args.expected_version:
        check(info["versao"] == args.expected_version, "Deployed version differs from the verified artifact")
    spec = call("GET", "/api-docs/v1")
    check(spec["paths"] and all(path.startswith("/api/v1/") for path in spec["paths"]), "OpenAPI v1 has unversioned paths")
    print("Health, version, OpenAPI and request-id correlation: OK")
    if not args.write:
        return

    def account(role):
        email = "smoke-" + uuid.uuid4().hex + "@example.com"
        password = secrets.token_urlsafe(24)
        user = call("POST", "/api/v1/auth/register", {"nome": "Teste da Aula 14", "email": email, "password": password, "role": role}, expected=201)
        login = call("POST", "/api/v1/auth/login", {"email": email, "password": password})
        return user["id"], login["accessToken"]

    client_id, client_token = account("CLIENT")
    provider_id, provider_token = account("PROVIDER")
    service = call("POST", "/api/v1/servicos", {"titulo": "Teste de suporte", "preco": 150.00, "categoria": "SUPORTE_TECNICO", "prestadorId": provider_id}, provider_token, 201)
    booking = call("POST", "/api/v1/contratos/bookings", {"clientId": client_id, "serviceId": service["id"]}, client_token, 201)
    check(booking["status"] == "PENDING", "New order has an unexpected status")
    for status in ("ACCEPTED", "IN_PROGRESS", "COMPLETED"):
        booking = call("PATCH", f"/api/v1/contratos/{booking['id']}", {"status": status}, provider_token)
        check(booking["status"] == status, "Order transition was not applied")
    review = {"nota": 5, "comentario": "Teste automatizado", "contratoId": booking["id"]}
    call("POST", "/api/v1/avaliacoes", review, client_token, 201)
    call("POST", "/api/v1/avaliacoes", review, client_token, 409)
    call("DELETE", f"/api/v1/servicos/{service['id']}", token=provider_token, expected=204)
    call("DELETE", f"/api/v1/prestadores/{provider_id}", token=provider_token, expected=204)
    call("GET", "/api/v1/prestadores", token=provider_token, expected=401)
    print("Registration, JWT, service, booking, transitions, unique review and soft deletion: OK")


if __name__ == "__main__":
    try:
        main()
    except (AssertionError, KeyError, ValueError, OSError) as error:
        print(f"Smoke check failed: {error}", file=sys.stderr)
        sys.exit(1)
