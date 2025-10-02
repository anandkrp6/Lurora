package com.bytecoder.lurora.backend.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Security manager for file encryption, secure storage, and cryptographic operations
 */
@Singleton
class SecurityManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val KEYSTORE_ALIAS = "LuroraSecureKey"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ENCRYPTED_PREFS_NAME = "lurora_secure_prefs"
        private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }

    private val encryptedSharedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Initialize the security manager and generate keys if needed
     */
    fun initialize() {
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            generateSecretKey()
        }
    }

    /**
     * Generate a new secret key in the Android Keystore
     */
    private fun generateSecretKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(false)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    /**
     * Get the secret key from the keystore
     */
    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
    }

    /**
     * Encrypt a file
     * @param inputFile The file to encrypt
     * @param outputFile The encrypted output file
     * @return True if encryption was successful
     */
    fun encryptFile(inputFile: File, outputFile: File): Boolean {
        return try {
            val secretKey = getSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv

            FileOutputStream(outputFile).use { outputStream ->
                // Write IV to the beginning of the file
                outputStream.write(iv.size)
                outputStream.write(iv)

                FileInputStream(inputFile).use { inputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        val encryptedData = cipher.update(buffer, 0, bytesRead)
                        if (encryptedData != null) {
                            outputStream.write(encryptedData)
                        }
                    }
                    val finalData = cipher.doFinal()
                    if (finalData != null) {
                        outputStream.write(finalData)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Decrypt a file
     * @param inputFile The encrypted file to decrypt
     * @param outputFile The decrypted output file
     * @return True if decryption was successful
     */
    fun decryptFile(inputFile: File, outputFile: File): Boolean {
        return try {
            val secretKey = getSecretKey()

            FileInputStream(inputFile).use { inputStream ->
                // Read IV from the beginning of the file
                val ivSize = inputStream.read()
                val iv = ByteArray(ivSize)
                inputStream.read(iv)

                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

                FileOutputStream(outputFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        val decryptedData = cipher.update(buffer, 0, bytesRead)
                        if (decryptedData != null) {
                            outputStream.write(decryptedData)
                        }
                    }
                    val finalData = cipher.doFinal()
                    if (finalData != null) {
                        outputStream.write(finalData)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Encrypt sensitive data for storage
     * @param data The data to encrypt
     * @return Encrypted data as Base64 string
     */
    fun encryptData(data: String): String? {
        return try {
            val secretKey = getSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encryptedData = cipher.doFinal(data.toByteArray())

            val combined = iv + encryptedData
            android.util.Base64.encodeToString(combined, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decrypt sensitive data
     * @param encryptedData The encrypted data as Base64 string
     * @return Decrypted data
     */
    fun decryptData(encryptedData: String): String? {
        return try {
            val secretKey = getSecretKey()
            val combined = android.util.Base64.decode(encryptedData, android.util.Base64.DEFAULT)

            val ivSize = 16 // AES CBC IV size
            val iv = combined.sliceArray(0 until ivSize)
            val encrypted = combined.sliceArray(ivSize until combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

            val decrypted = cipher.doFinal(encrypted)
            String(decrypted)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Store sensitive data securely
     * @param key The key to store the data under
     * @param value The value to store
     */
    fun storeSecureData(key: String, value: String) {
        encryptedSharedPreferences.edit()
            .putString(key, value)
            .apply()
    }

    /**
     * Retrieve sensitive data securely
     * @param key The key to retrieve data for
     * @return The stored value or null if not found
     */
    fun getSecureData(key: String): String? {
        return encryptedSharedPreferences.getString(key, null)
    }

    /**
     * Clear all secure data
     */
    fun clearSecureData() {
        encryptedSharedPreferences.edit().clear().apply()
    }

    /**
     * Check if a file is encrypted by this system
     * @param file The file to check
     * @return True if the file appears to be encrypted by this system
     */
    fun isFileEncrypted(file: File): Boolean {
        return try {
            if (!file.exists() || file.length() < 17) return false // Too small to contain IV + data

            FileInputStream(file).use { inputStream ->
                val ivSize = inputStream.read()
                ivSize == 16 // AES CBC IV size
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generate a secure random string for tokens, etc.
     * @param length The length of the string to generate
     * @return A secure random string
     */
    fun generateSecureToken(length: Int = 32): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = java.security.SecureRandom()
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }

    /**
     * Validate file integrity using checksum
     * @param file The file to validate
     * @param expectedChecksum The expected MD5 checksum
     * @return True if the file matches the expected checksum
     */
    fun validateFileIntegrity(file: File, expectedChecksum: String): Boolean {
        return try {
            val digest = java.security.MessageDigest.getInstance("MD5")
            FileInputStream(file).use { inputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val checksum = digest.digest().joinToString("") { "%02x".format(it) }
            checksum.equals(expectedChecksum, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
}