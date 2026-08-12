package com.damianhoward.kcoin

import java.math.BigInteger
import java.security.Key
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.util.Base64

fun String.hash(algorithm: String = "SHA-256"): String {
    val messageDigest = MessageDigest.getInstance(algorithm)
    messageDigest.update(this.toByteArray())
    return String.format("%064x", BigInteger(1, messageDigest.digest()))
}

fun String.sign(
    privateKey: PrivateKey,
    algorithm: String = "SHA256withRSA",
): ByteArray {
    val rsa = Signature.getInstance(algorithm)
    rsa.initSign(privateKey)
    rsa.update(this.toByteArray())
    return rsa.sign()
}

fun String.verifySignature(
    publicKey: PublicKey,
    signature: ByteArray,
    algorithm: String = "SHA256withRSA",
): Boolean {
    val rsa = Signature.getInstance(algorithm)
    rsa.initVerify(publicKey)
    rsa.update(this.toByteArray())
    // verify() throws (rather than returning false) for a structurally invalid signature, e.g.
    // the empty byte array of a never-signed transaction — that's "invalid", not an error.
    return try {
        rsa.verify(signature)
    } catch (e: java.security.SignatureException) {
        false
    }
}

fun Key.encodeToString(): String = Base64.getEncoder().encodeToString(this.encoded)
