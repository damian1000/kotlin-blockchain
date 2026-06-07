# TODO

## Possible enhancements

- The vestigial `Transaction(sender, recipient, amount)` header could be dropped in favor of deriving sender/recipient from inputs/outputs (already noted as a teaching limitation in the README).
- `Transaction.Companion.salt` is a non-thread-safe counter; fine for the single-threaded demo but a real engine would use a per-transaction nonce derived from cryptographic randomness.
- `Block.mine()` can spin forever if difficulty is set too high. A `maxIterations` parameter or a timeout would let callers fail predictably instead of hang.
