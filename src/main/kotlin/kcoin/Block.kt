package kcoin

import java.time.Instant

data class Block(val previousHash: String,
                 val transactions: MutableList<Transaction> = mutableListOf(),
                 val timestamp: Long = Instant.now().toEpochMilli(),
                 var nonce: Long = 0,
                 var hash: String = "") {

    fun calculateHash(): String {
        return "$previousHash$transactions$timestamp$nonce".hash()
    }

    fun addGenesisTransaction(transaction: Transaction) : Block {
        transactions.add(transaction)
        return this
    }

    fun addTransaction(transaction: Transaction) : Block {
        if (transaction.isSignatureValid())
            transactions.add(transaction)
        return this
    }

    fun mine(validPrefix: String) {
        val start = System.currentTimeMillis()
        while (!isMined(validPrefix)) {
            nonce += 1;
            hash = calculateHash();
        }
        println("Completed mining: " + (System.currentTimeMillis() - start) + " milliseconds. Nonce: " + nonce)
    }

    fun isMined(validPrefix: String) : Boolean {
        return hash.startsWith(validPrefix)
    }

}