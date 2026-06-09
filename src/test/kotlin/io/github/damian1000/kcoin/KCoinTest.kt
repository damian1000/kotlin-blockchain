package io.github.damian1000.kcoin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
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
        genesisTransaction.outputs.add(
            TransactionItem(recipient = wallet1.publicKey, amount = 100, transactionHash = genesisTransaction.hash),
        )
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

    @Test
    fun sendingMoreThanBalanceThrows() {
        createGenesisTransaction()
        assertThrows(IllegalArgumentException::class.java) {
            wallet1.sendFundsTo(recipient = wallet2.publicKey, amountToSend = wallet1.balance + 1)
        }
    }

    @Test
    fun transactionItemIsMineDistinguishesOwners() {
        val tx = Transaction.create(sender = wallet1.publicKey, recipient = wallet2.publicKey, amount = 5)
        val item = TransactionItem(recipient = wallet2.publicKey, amount = 5, transactionHash = tx.hash)
        assertTrue(item.isMine(wallet2.publicKey))
        assertFalse(item.isMine(wallet1.publicKey))
        assertEquals(tx.hash, item.transactionHash)
        item.hash = "rewritten"
        assertEquals("rewritten", item.hash)
    }

    @Test
    fun emptyBlockChainAndSingleBlockChainAreValid() {
        // empty chain valid by definition
        assertTrue(BlockChain(1).isValid())

        // single-block chain valid when its hash matches calculateHash()
        val single = BlockChain(1)
        single.add(
            Block(previousHash = "0").addGenesisTransaction(
                Transaction.create(sender = wallet1.publicKey, recipient = wallet1.publicKey, amount = 1),
            ),
        )
        assertTrue(single.isValid())
    }

    @Test
    fun tamperedOutputInvalidatesSignature() {
        createGenesisTransaction()
        val tx = wallet1.sendFundsTo(recipient = wallet2.publicKey, amountToSend = 15)
        assertTrue(tx.isSignatureValid())

        // Sneak an extra output to wallet3 after the wallet has signed.
        tx.outputs.add(TransactionItem(recipient = wallet3.publicKey, amount = 50, transactionHash = tx.hash))
        assertFalse(tx.isSignatureValid(), "Adding outputs after signing must invalidate the signature")

        // And a block must refuse to accept it.
        assertThrows(IllegalArgumentException::class.java) {
            Block(previousHash = "0").addTransaction(tx)
        }
    }
}
