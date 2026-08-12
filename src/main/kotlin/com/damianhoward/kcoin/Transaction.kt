package com.damianhoward.kcoin

import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom

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

/**
 * A UTXO-model transaction. The wallet that authorizes the spend is the
 * `signer`; the actual transfer is expressed entirely through `inputs`
 * (UTXOs being consumed) and `outputs` (UTXOs being created). The signer's
 * private key signs the full content.
 *
 * `nonce` is per-transaction cryptographically-random bytes (hex-encoded)
 * so two transactions with identical signer + inputs + outputs still get
 * distinct hashes. Replaces an earlier thread-unsafe counter.
 */
data class Transaction(
    val signer: PublicKey,
    val nonce: String = generateNonce(),
    var hash: String = "",
    val inputs: MutableList<TransactionItem> = mutableListOf(),
    val outputs: MutableList<TransactionItem> = mutableListOf(),
) {
    private var signature: ByteArray = ByteArray(0)

    init {
        hash = "${signer.encodeToString()}$nonce".hash()
    }

    companion object {
        fun create(signer: PublicKey): Transaction = Transaction(signer)

        private val random = SecureRandom()

        private fun generateNonce(): String {
            val bytes = ByteArray(16)
            random.nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    fun sign(privateKey: PrivateKey): Transaction {
        signature = signaturePayload().sign(privateKey)
        return this
    }

    fun isSignatureValid(): Boolean = signaturePayload().verifySignature(signer, signature)

    private fun signaturePayload(): String {
        val inputsPart = inputs.joinToString(",") { it.hash }
        val outputsPart = outputs.joinToString(",") { "${it.recipient.encodeToString()}:${it.amount}" }
        return "${signer.encodeToString()}|$nonce|$inputsPart|$outputsPart"
    }
}
