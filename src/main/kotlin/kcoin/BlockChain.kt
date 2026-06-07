
package kcoin

class BlockChain(val difficulty: Int) {

    private val blocks: MutableList<Block> = mutableListOf()
    private val validPrefix = "0".repeat(difficulty)
    val UTXO: MutableMap<String, TransactionItem> = mutableMapOf()

    fun isValid() : Boolean {
        when {
            blocks.isEmpty() -> return true
            blocks.size == 1 -> return blocks[0].hash == blocks[0].calculateHash()
            else -> {
                for (i in 1 until blocks.size) {
                    val previousBlock = blocks[i - 1]
                    val currentBlock = blocks[i]
                    when {
                        currentBlock.hash != currentBlock.calculateHash() -> return false
                        currentBlock.previousHash != previousBlock.calculateHash() -> return false
                        !previousBlock.isMined(validPrefix) || !currentBlock.isMined(validPrefix) -> return false
                    }
                }
                return true
            }
        }
    }

    fun add(block: Block) : Block {
        block.mine(validPrefix);
        blocks.add(block)
        updateUTXO(block)
        return block;
    }

    private fun updateUTXO(block: Block) {
        block.transactions.flatMap { it.inputs }.map { it.hash }.forEach { UTXO.remove(it) }
        UTXO.putAll(block.transactions.flatMap { it.outputs }.associateBy { it.hash })
    }
}