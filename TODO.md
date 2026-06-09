# TODO

- Decide: keep as-is (unpinned), or re-purpose as a "Kotlin cryptography demo" (drop blockchain framing, lean into SHA-256 / RSA-signing code, use `Result` types in place of exceptions, document signing payload format).
- Drop the vestigial `Transaction(sender, recipient, amount)` header; derive sender/recipient from inputs/outputs.
- Replace `Transaction.Companion.salt` counter with a per-transaction nonce from cryptographic randomness.
