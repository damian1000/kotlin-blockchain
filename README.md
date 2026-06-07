# Kotlin Blockchain

[![CI](https://github.com/damian1000/kotlin-blockchain/actions/workflows/ci.yml/badge.svg)](https://github.com/damian1000/kotlin-blockchain/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/damian1000/kotlin-blockchain/graph/badge.svg)](https://codecov.io/gh/damian1000/kotlin-blockchain)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3.21-blueviolet)](https://kotlinlang.org/)
[![JDK](https://img.shields.io/badge/jdk-25-orange)](https://openjdk.org/projects/jdk/25/)

A small blockchain in Kotlin that demonstrates the core primitives end-to-end: SHA-256 block hashing, proof-of-work mining, RSA-signed transactions, and a UTXO balance model with multi-wallet transfers.

## What it implements

- **Proof-of-work mining** — incrementing a `nonce` until the block hash starts with `N` zero characters (`difficulty`).
- **UTXO accounting** — wallet balances are derived from unspent transaction outputs, not a mutable per-address ledger. Spending a UTXO removes it; change goes back to the sender as a new output.
- **RSA-signed transactions** — each spend is signed with the sender's private key (`SHA256withRSA`); blocks only accept transactions whose signature verifies against the sender's public key.
- **Chain validation** — every block's hash matches its recomputed hash, every `previousHash` matches its predecessor, and every block satisfies the PoW prefix.

## Design

| Type | Responsibility |
|---|---|
| `BlockChain` | Holds the chain, the `UTXO` map, and the difficulty. Adds blocks (mining inline) and validates the full chain. |
| `Block` | `previousHash`, `transactions`, `timestamp`, `nonce`, `hash`. Mines itself against a target prefix. |
| `Transaction` | Sender + recipient public keys, amount, inputs (UTXOs consumed), outputs (UTXOs created), RSA signature. |
| `TransactionItem` | A single UTXO: recipient, amount, and a self-hash that ties it back to the transaction that created it. |
| `Wallet` | RSA keypair + a view onto the chain's UTXO map. Builds, signs, and broadcasts transactions; balance is `Σ amount` over its own UTXOs. |
| `Utils` | `String.hash()`, `String.sign()`, `String.verifySignature()`, `Key.encodeToString()` extension functions. |

Mining happens in `BlockChain.add(block)` — the block is mined to the chain's difficulty before being appended, then the UTXO map is updated by removing spent inputs and inserting new outputs.

## Run

```bash
./gradlew test     # JUnit 5; runs the multi-wallet scenario
```

The test sets up three wallets, creates a genesis block awarding 100 to wallet 1, then transfers 15 from wallet 1 to wallet 2 and 10 from wallet 2 to wallet 3, asserting balances and chain validity at each step.

## Use it

```kotlin
val chain = BlockChain(difficulty = 2)
val alice = Wallet.create(chain)
val bob = Wallet.create(chain)

// Genesis: mint 100 to Alice
val mint = Transaction.create(sender = alice.publicKey, recipient = alice.publicKey, amount = 100)
mint.outputs.add(TransactionItem(alice.publicKey, 100, mint.hash))
val genesis = chain.add(Block(previousHash = "0").addGenesisTransaction(mint))

// Alice sends 30 to Bob
val tx = alice.sendFundsTo(recipient = bob.publicKey, amountToSend = 30)
chain.add(Block(genesis.hash).addTransaction(tx))

alice.balance   // 70
bob.balance     // 30
chain.isValid() // true
```

## What's intentionally not here

This is a teaching artifact, not a production node. The following are deliberately out of scope:

- **No peer-to-peer / no consensus.** Single-node only — no fork choice, no longest-chain rule, no gossip protocol.
- **No persistence.** The chain lives in memory; restart loses state.
- **No fees, no block reward halving, no difficulty adjustment.**
- **Mining is synchronous** inside `BlockChain.add(...)`. Real chains decouple mining from chain extension.
- **RSA, not ECDSA.** Real cryptocurrencies use secp256k1 / Ed25519; RSA was chosen here for clarity using the JDK's built-in providers.
- **Block hash includes `transactions.toString()`.** Works for a demo; a real implementation would hash a Merkle root for tamper-evident transaction trees.
- **The `Transaction(sender, recipient, amount)` header is vestigial.** Real UTXO transactions don't have a single sender/recipient — those are derived from inputs and outputs. When a wallet calls `sendFundsTo(other, 15)`, the resulting `Transaction` has `sender = recipient = self`, and the actual transfer lives entirely in the `outputs` list. The header is kept for didactic clarity but is misleading; a real engine would drop it.

## Stack

- Kotlin 2.3.21 (JVM target 25)
- Java 25 toolchain
- JUnit Jupiter 6.1
- Gradle 9.5.1

No web framework, no DB, no third-party crypto library. Pure JDK + Kotlin.

## License

Apache 2.0 — see [LICENSE](LICENSE).
