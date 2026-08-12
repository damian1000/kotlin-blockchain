package com.damianhoward.kcoin

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
        val genesisTransaction = Transaction.create(signer = wallet1.publicKey)
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
        val tx = Transaction.create(signer = wallet1.publicKey)
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
                Transaction.create(signer = wallet1.publicKey),
            ),
        )
        assertTrue(single.isValid())
    }

    @Test
    fun chainWithTransactionTamperedAfterMiningIsInvalid() {
        val genesisBlock = createGenesisTransaction()
        val tx = wallet1.sendFundsTo(recipient = wallet2.publicKey, amountToSend = 15)
        val block = blockChain.add(Block(genesisBlock.hash).addTransaction(tx))
        assertTrue(blockChain.isValid())

        // Forge an extra output after signing, then re-mine so the hash/link checks still pass —
        // only the signature verification can catch this rewrite.
        block.transactions[0].outputs.add(
            TransactionItem(recipient = wallet3.publicKey, amount = 50, transactionHash = tx.hash),
        )
        block.hash = ""
        block.mine("0".repeat(2))

        assertFalse(blockChain.isValid(), "a rewritten spend must invalidate the chain")
    }

    @Test
    fun chainWithUnsignedSpendIsInvalid() {
        val genesisBlock = createGenesisTransaction()

        // A spend (has inputs) that was never signed, smuggled in via the genesis path that
        // bypasses the add-time signature check.
        val forged = Transaction.create(signer = wallet1.publicKey)
        forged.inputs.add(blockChain.UTXO.values.first())
        forged.outputs.add(TransactionItem(recipient = wallet2.publicKey, amount = 100, transactionHash = forged.hash))
        blockChain.add(Block(genesisBlock.hash).addGenesisTransaction(forged))

        assertFalse(blockChain.isValid(), "an unsigned spend must invalidate the chain")
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

    @Test
    fun negativeAmountCannotDrainTheRecipient() {
        createGenesisTransaction()

        // Without the guard this succeeded and left wallet1 = 140, wallet2 = -40: the recipient's
        // balance moved to the sender on the sender's signature alone, which is theft, not a
        // transfer. Only wallet1 signs here — wallet2 never consents to anything.
        assertThrows(IllegalArgumentException::class.java) {
            wallet1.sendFundsTo(recipient = wallet2.publicKey, amountToSend = -40)
        }

        assertEquals(100, wallet1.balance, "the sender gained nothing")
        assertEquals(0, wallet2.balance, "the recipient lost nothing")
        assertTrue(blockChain.isValid())
    }

    @Test
    fun zeroAmountIsRejected() {
        createGenesisTransaction()

        // A zero transfer consumes an input to move nothing; there is no honest reading of it.
        assertThrows(IllegalArgumentException::class.java) {
            wallet1.sendFundsTo(recipient = wallet2.publicKey, amountToSend = 0)
        }

        assertEquals(100, wallet1.balance)
        assertEquals(0, wallet2.balance)
    }

    @Test
    fun aWalletWithNothingCannotSendANegativeAmountEither() {
        createGenesisTransaction()

        // wallet3 holds nothing, so the balance check alone would have waved -50 through and
        // minted it a positive balance out of wallet1's coins.
        assertThrows(IllegalArgumentException::class.java) {
            wallet3.sendFundsTo(recipient = wallet1.publicKey, amountToSend = -50)
        }

        assertEquals(0, wallet3.balance)
        assertEquals(100, wallet1.balance)
    }
}
