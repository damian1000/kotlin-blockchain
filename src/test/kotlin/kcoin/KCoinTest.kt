package kcoin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KCoinTest {

    val blockChain = BlockChain(2)
    val wallet1 = Wallet.create(blockChain)
    val wallet2 = Wallet.create(blockChain)
    val wallet3 = Wallet.create(blockChain)

    @BeforeEach
    fun setup() {
        assertTrue(blockChain.isValid())
        assertEquals(0, wallet1.balance)
        assertEquals(0, wallet2.balance)
        assertEquals(0, wallet3.balance)
    }

    private fun createGenesisTransaction(): Block {
        val genesisTransaction = Transaction.create(sender = wallet1.publicKey, recipient = wallet1.publicKey, amount = 100)
        genesisTransaction.outputs.add(TransactionItem(recipient = wallet1.publicKey, amount = 100, transactionHash = genesisTransaction.hash))
        val genesisBlock = Block(previousHash = "0")
        genesisBlock.addGenesisTransaction(genesisTransaction)
        blockChain.add(genesisBlock)
        assertTrue(blockChain.isValid())
        assertEquals(100, wallet1.balance)
        assertEquals(0, wallet2.balance)
        assertEquals(0, wallet3.balance)
        return genesisBlock
    }

    @Test
    fun testTwoTransactions() {
        val genesisBlock: Block = createGenesisTransaction()

        val transactionOne = wallet1.sendFundsTo(recipient = wallet2.publicKey, amountToSend = 15)
        val blockOne = blockChain.add(Block(genesisBlock.hash).addTransaction(transactionOne))
        assertTrue(blockChain.isValid())
        assertEquals(85, wallet1.balance)
        assertEquals(15, wallet2.balance)
        assertEquals(0, wallet3.balance)

        val transactionTwo = wallet2.sendFundsTo(recipient = wallet3.publicKey, amountToSend = 10)
        blockChain.add(Block(blockOne.hash).addTransaction(transactionTwo))
        assertTrue(blockChain.isValid())
        assertEquals(85, wallet1.balance)
        assertEquals(5, wallet2.balance)
        assertEquals(10, wallet3.balance)
    }

}