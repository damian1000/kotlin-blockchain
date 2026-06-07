# TODO

## Roadmap (prioritized)

### P1 — positioning

An external review flagged this repo as "low value, don't pin in its present form" for quant/HFT-leaning portfolios. That's probably right: blockchain isn't what those firms hire for, and the current implementation is a learning toy rather than a serious distributed-systems demo.

Two honest options:

- **Keep as-is, don't pin.** Leave it as a working public repo that demonstrates Kotlin + HMAC + recursive Merkle-style hashing, but stop including it in the GitHub profile pins.
- **Re-purpose into "Kotlin cryptography demo".** Drop the blockchain framing, lean into the SHA-256 / RSA-signing / signature-verification code that's already here. Smaller scope, no overclaim. Add `kotlin-stdlib` `Result` types instead of exceptions, document the signing payload format. Achievable in a focused afternoon.

### P2 — only if keeping pinned

The items under "Possible enhancements" below are nice-to-haves; none of them change the repo's positioning. Don't invest here unless P1 lands on "keep and rehabilitate".

## Possible enhancements

- The vestigial `Transaction(sender, recipient, amount)` header could be dropped in favor of deriving sender/recipient from inputs/outputs (already noted as a teaching limitation in the README).
- `Transaction.Companion.salt` is a non-thread-safe counter; fine for the single-threaded demo but a real engine would use a per-transaction nonce derived from cryptographic randomness.
- `Block.mine()` can spin forever if difficulty is set too high. A `maxIterations` parameter or a timeout would let callers fail predictably instead of hang.
