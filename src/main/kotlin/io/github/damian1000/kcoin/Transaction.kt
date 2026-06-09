package io.github.damian1000.kcoin

import java.security.PrivateKey
import java.security.PublicKey

data class TransactionItem(
    val recipient: PublicKey,
    val amount: Int,
    val transactionHash: String,
    var hash: String = "",
) {
    init {
        hash = "${recipient.encodeToString()}$amount$transactionHash".hash()
    }

    fun isMine(me: PublicKey): Boolean = recipient == me
}

data class Transaction(
    val sender: PublicKey,
    val recipient: PublicKey,
    val amount: Int,
    var hash: String = "",
    val inputs: MutableList<TransactionItem> = mutableListOf(),
    val outputs: MutableList<TransactionItem> = mutableListOf(),
) {
    private var signature: ByteArray = ByteArray(0)

    init {
        hash = "${sender.encodeToString()}${recipient.encodeToString()}$amount$salt".hash()
    }

    companion object {
        fun create(
            sender: PublicKey,
            recipient: PublicKey,
            amount: Int,
        ): Transaction = Transaction(sender, recipient, amount)

        var salt: Long = 0
            get() {
                field += 1
                return field
            }
    }

    fun sign(privateKey: PrivateKey): Transaction {
        signature = signaturePayload().sign(privateKey)
        return this
    }

    fun isSignatureValid(): Boolean = signaturePayload().verifySignature(sender, signature)

    private fun signaturePayload(): String {
        val inputsPart = inputs.joinToString(",") { it.hash }
        val outputsPart = outputs.joinToString(",") { "${it.recipient.encodeToString()}:${it.amount}" }
        return "${sender.encodeToString()}|${recipient.encodeToString()}|$amount|$inputsPart|$outputsPart"
    }
}
