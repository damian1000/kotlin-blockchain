package com.damianhoward.kcoin

class BlockChain(
    val difficulty: Int,
) {
    private val blocks: MutableList<Block> = mutableListOf()
    private val validPrefix = "0".repeat(difficulty)

    @Suppress("PropertyName") // UTXO (Unspent Transaction Output) is established domain terminology
    val UTXO: MutableMap<String, TransactionItem> = mutableMapOf()

    /**
     * A chain is valid when every block's stored hash matches its recomputed one, every block is
     * mined to the difficulty prefix, every block links to its predecessor's hash, and every
     * *spending* transaction (one with inputs) carries a valid signature. Transactions without
     * inputs — genesis/coinbase issuance — have no prior owner to authorise them, so they are
     * exempt from the signature rule.
     */
    fun isValid(): Boolean {
        val blocksAreSound =
            blocks.all { block ->
                block.hash == block.calculateHash() &&
                    block.isMined(validPrefix) &&
                    block.transactions.all { it.inputs.isEmpty() || it.isSignatureValid() }
            }
        if (!blocksAreSound) return false
        return (1 until blocks.size).all { blocks[it].previousHash == blocks[it - 1].hash }
    }

    fun add(block: Block): Block {
        block.mine(validPrefix)
        blocks.add(block)
        updateUTXO(block)
        return block
    }

    private fun updateUTXO(block: Block) {
        block.transactions
            .flatMap { it.inputs }
            .map { it.hash }
            .forEach { UTXO.remove(it) }
        UTXO.putAll(block.transactions.flatMap { it.outputs }.associateBy { it.hash })
    }
}
