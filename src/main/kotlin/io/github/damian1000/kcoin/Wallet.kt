package io.github.damian1000.kcoin

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
