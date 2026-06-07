# TODO

## Cleanup

- Replace `println("Completed mining: ... milliseconds. Nonce: ...")` in `Block.mine()` with SLF4J. It's library code inside a recursive operation; structured logging fits better than a bare `println`. Use `org.jetbrains.kotlin:kotlin-stdlib-jdk8` already on the classpath plus `org.slf4j:slf4j-api` + `org.slf4j:slf4j-simple`.

## Possible enhancements

- The vestigial `Transaction(sender, recipient, amount)` header could be dropped in favor of deriving sender/recipient from inputs/outputs (already noted as a teaching limitation in the README).
- `Transaction.Companion.salt` is a non-thread-safe counter; fine for the single-threaded demo but a real engine would use a per-transaction nonce derived from cryptographic randomness.
- `Block.mine()` can spin forever if difficulty is set too high. A `maxIterations` parameter or a timeout would let callers fail predictably instead of hang.
