package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    private val email: String? = null,
    private val rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {
    val userInfo: String

    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize()

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString(" ")

    private var phone: String? = null
        set(value) {
            field = value?.replace("""[^+\d]""".toRegex(), "")
        }

    private var _login: String? = null
    var login: String
        set(value) {
            _login = value?.toLowerCase()
        }
        get() = _login!!

    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null



    private val salt by lazy {
        ByteArray(16).also { SecureRandom().nextBytes(it)}.toString()
    }

    //for email
    constructor(
        firstName: String,
        lastName: String?,
        email: String,
        password: String
    ): this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("Secondary mail constructor")
        passwordHash = encrypt(password)
    }

    //for phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String,
    ): this(firstName, lastName, rawPhone = rawPhone,  meta = mapOf("auth" to "sms")) {
        println("Secondary rawPhone constructor")
        regenerateAccessCode(rawPhone)
//        val code = generateAccessCod()
//        passwordHash = encrypt(code)
//        accessCode = code
//        sendAccessCodeToUser(rawPhone, code)
    }


    init {
        println("First init block, primary constructor was called")

        check(firstName.isNotBlank()) {"First name must be not blank"}
        check(!email.isNullOrBlank() || !rawPhone.isNullOrBlank()) {"email or  rawPhone be not blank"}

        phone = rawPhone
        login = email ?: phone!!

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()
    }

    //метод проверки ввода пароля
    fun checkPassword(pass: String) = encrypt(pass) == passwordHash

    //метод замены пароля
    fun changePassword(oldPass: String, newPass: String) {
        if(checkPassword(oldPass))  {
            passwordHash = encrypt(newPass)
            if(!accessCode.isNullOrEmpty()) accessCode = newPass
        } else throw java.lang.IllegalArgumentException("The entered password does not match current pasword")
    }


    private fun encrypt(password: String): String = salt.plus(password).md5() // good

    private fun String.md5() : String {
        val md = MessageDigest.getInstance("MD5")
        val digest: ByteArray = md.digest(toByteArray()) // 16 byte
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    fun regenerateAccessCode(rawPhone: String) {
        val code = generateAccessCod()
        passwordHash = encrypt(code)
        println("Phone passwordHash is $passwordHash")
        accessCode = code
        sendAccessCodeToUser(rawPhone, code)
    }

    private fun generateAccessCod(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabsdifghijklmnopqrstuvwxyz0123456789"

        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    //имитирует отправку кода по смс
    private fun sendAccessCodeToUser(phone: String, code: String) {
        println("..... sending access code $code on $phone")
    }

    companion object Factory {
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return when {
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(firstName, lastName, email, password)
                else -> throw java.lang.IllegalArgumentException("Email or phone must be not blank or blank")
            }
        }


        private fun String.fullNameToPair(): Pair<String, String?> {
            return this.split(" ")
                .filter { it.isNotBlank() }
                .run {
                    when(size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException(
                            "FullName must contain only first name and last name, current split " +
                                    "result: ${this@fullNameToPair}"
                        )
                    }
                }
        }
    }
}