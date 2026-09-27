# Mesh-Routed Deferred Payment Settlement Simulator

A Spring Boot simulation of a payment instruction travelling across offline relay
devices and being settled once any bridge device regains internet connectivity.

> This is an educational backend prototype, not real offline UPI, a bank core,
> or a Bluetooth implementation. It demonstrates secure packet handling,
> deferred delivery, transactional settlement, and duplicate prevention.

## Problem

Conventional digital payments need an immediate internet connection. In places
such as basements, remote areas, crowded venues, or during a temporary outage,
the payment request cannot reach the settlement server directly.

This simulator explores a store-and-forward model: an encrypted payment packet
can be carried by nearby devices and submitted later by a device that has
connectivity. The final settlement still happens centrally.

## Architecture

```text
Sender device
  creates an encrypted payment packet
        |
        | simulated mesh gossip
        v
Relay devices -----------------> bridge device with internet
                                      |
                                      | HTTPS POST
                                      v
Spring Boot settlement service
  hash -> idempotency -> decrypt -> freshness -> transactional ledger update
                                      |
                                      v
                              H2 demo ledger
```

### Main flow

1. The demo sender creates a `PaymentInstruction` containing sender, receiver,
   amount, nonce, and timestamp.
2. `HybridCryptoService` encrypts it with AES-256-GCM and encrypts the one-time
   AES key with the server RSA public key.
3. `MeshSimulatorService` copies the opaque packet among virtual devices.
4. An internet-enabled bridge calls `/api/bridge/ingest`.
5. `BridgeIngestionService` hashes the ciphertext and atomically claims that
   hash before doing further processing.
6. The service decrypts the packet, rejects stale/future-dated packets, and
   calls `SettlementService`.
7. `SettlementService` debits, credits, and writes a ledger record inside one
   database transaction.

## Key engineering concepts

| Concept | Implementation |
|---|---|
| Confidentiality and tamper detection | RSA-OAEP + AES-256-GCM hybrid encryption |
| Duplicate delivery | SHA-256 ciphertext hash and atomic `putIfAbsent` claim |
| Replay protection | Encrypted `signedAt` timestamp with 24-hour freshness window |
| Atomic ledger update | Spring `@Transactional` settlement service |
| Concurrent balance updates | JPA optimistic locking using `@Version` |
| Input safety | Bean validation and structured JSON validation errors |
| Test coverage | Crypto round trip, tampering, concurrency, stale/future packets, rejection, API validation |

## Run locally

Prerequisites: JDK 17 or newer.

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Open `http://localhost:8080` for the demo dashboard.

Useful endpoints:

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/health` | Application health check |
| GET | `/api/accounts` | Demo account balances |
| POST | `/api/demo/send` | Create and inject an encrypted packet |
| POST | `/api/mesh/gossip` | Simulate one mesh gossip round |
| POST | `/api/mesh/flush` | Submit bridge-held packets concurrently |
| GET | `/api/transactions` | Latest ledger entries |

Example payment request:

```json
{
  "senderVpa": "user1@demo",
  "receiverVpa": "user2@demo",
  "amount": 100.00,
  "pin": "1234",
  "ttl": 5,
  "startDevice": "phone-user1"
}
```

## Test scenarios

The test suite verifies:

- AES/RSA hybrid encryption can decrypt a valid instruction;
- altered ciphertext is rejected;
- three bridges submitting the same packet settle it exactly once;
- insufficient funds return `REJECTED` without changing balances;
- stale and future-dated packets are rejected;
- invalid HTTP requests return a structured `400 Bad Request` response.

## Intentional limitations

- The mesh and phones are in-memory simulations; no real BLE or Wi-Fi Direct is used.
- The H2 database and generated RSA key pair reset on restart.
- Encryption protects packet content but does not authenticate a sender. A real
  system needs device-bound signing keys, authenticated bridge nodes, PIN
  verification, risk controls, and bank/wallet integration.
- Deferred delivery is not guaranteed settlement: a sender can still lack funds
  when the packet reaches the server.
- The idempotency cache is JVM-local. A multi-instance deployment requires a
  shared, durable approach such as Redis plus database-backed transaction state.

## Technology

Java 17, Spring Boot 3, Spring MVC, Spring Data JPA, Hibernate, H2, Thymeleaf,
Bean Validation, JUnit 5, RSA-OAEP, AES-GCM.
