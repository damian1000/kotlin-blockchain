package com.damianhoward.kcoin

import org.slf4j.LoggerFactory
import java.time.Instant

data class Block(
    val previousHash: String,
    val transactions: MutableList<Transaction> = mutableListOf(),
    val timestamp: Long = Instant.now().toEpochMilli(),
    var nonce: Long = 0,
    var hash: String = "",
) {
    fun calculateHash(): String = "$previousHash$transactions$timestamp$nonce".hash()

    fun addGenesisTransaction(transaction: Transaction): Block {
        transactions.add(transaction)
        return this
    }

    fun addTransaction(transaction: Transaction): Block {
        require(transaction.isSignatureValid()) { "Transaction signature is invalid; refusing to add to block" }
        transactions.add(transaction)
        return this
    }

    fun mine(
        validPrefix: String,
        maxIterations: Long = DEFAULT_MAX_MINE_ITERATIONS,
    ) {
        require(maxIterations > 0) { "maxIterations must be positive" }
        val start = System.currentTimeMillis()
        var iterations = 0L
        while (!isMined(validPrefix)) {
            if (iterations >= maxIterations) {
                throw MineException(
                    "Failed to mine block with prefix '$validPrefix' after $maxIterations iterations",
                )
            }
            nonce += 1
            hash = calculateHash()
            iterations += 1
        }
        log.info("Completed mining in {} ms. Nonce: {}", System.currentTimeMillis() - start, nonce)
    }

    fun isMined(validPrefix: String): Boolean = hash.startsWith(validPrefix)

    companion object {
        const val DEFAULT_MAX_MINE_ITERATIONS = 10_000_000L
        private val log = LoggerFactory.getLogger(Block::class.java)
    }
}

class MineException(
    message: String,
) : RuntimeException(message)
