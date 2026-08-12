package com.damianhoward.kcoin

import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey

// Deliberately not a data class: a generated toString/copy/equals over a PrivateKey would leak
// or duplicate key material (JCA RSA keys print their internals).
class Wallet(
    val publicKey: PublicKey,
    private val privateKey: PrivateKey,
    private val blockChain: BlockChain,
) {
    companion object {
        fun create(blockChain: BlockChain): Wallet {
            val generator = KeyPairGenerator.getInstance("RSA")
            generator.initialize(2048)
            val keyPair = generator.generateKeyPair()
            return Wallet(keyPair.public, keyPair.private, blockChain)
        }
    }

    val balance: Int get() {
        return getMyTransactions().sumOf { it.amount }
    }

    private fun getMyTransactions(): Collection<TransactionItem> = blockChain.UTXO.filterValues { it.isMine(publicKey) }.values

    fun sendFundsTo(
        recipient: PublicKey,
        amountToSend: Int,
    ): Transaction {
        // A negative amount inverts the transfer: the recipient receives a negative output, and
        // the change line computes collected - (-n), handing the sender more than it put in. So
        // "sending" -40 moves 40 from the recipient to the sender, with only the sender's
        // signature. The amount has to be positive before any of that arithmetic runs.
        if (amountToSend <= 0) {
            throw IllegalArgumentException("Amount to send must be positive, was $amountToSend")
        }
        if (amountToSend > balance) {
            throw IllegalArgumentException("Insufficient funds")
        }

        val tx = Transaction.create(signer = publicKey)
        tx.outputs.add(TransactionItem(recipient = recipient, amount = amountToSend, transactionHash = tx.hash))

        var collectedAmount = 0
        for (myTx in getMyTransactions()) {
            collectedAmount += myTx.amount
            tx.inputs.add(myTx)

            if (collectedAmount > amountToSend) {
                val change = collectedAmount - amountToSend
                tx.outputs.add(TransactionItem(recipient = publicKey, amount = change, transactionHash = tx.hash))
            }

            if (collectedAmount >= amountToSend) {
                break
            }
        }
        return tx.sign(privateKey)
    }
}
