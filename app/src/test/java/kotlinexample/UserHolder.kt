package ru.skillbranch.kotlinexample

object UserHolder {
    private val map = mutableMapOf<String, User>()

//    fun registerUser(
//        fullName: String,
//        email: String,
//        password: String
//    ): User {
//        return User.makeUser(fullName, email = email, password = password)
//            .also { user ->
//                map[user.login] = user
//            }
//    }

//    fun loginUser(login: String, password: String): String? {
//        return map[login.trim()]?.run {
//            if (checkPassword(password)) this.userInfo
//            else null
//        }
//    }

    fun registerUser(fullName:String, email:String, password:String): User {
        val user = User.makeUser(fullName, email, password)
        val login = user.login
        if (isUserExist(login))
            throw IllegalArgumentException("A user with this email already exists")
        map[login] = user
        return user
    }

    private fun isUserExist(login: String) = map[login] != null

    fun registerUserByPhone(fullName: String, rawPhone: String): User {
        if (!isValidPhone(rawPhone))
            throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
        val user = User.makeUser(fullName, phone = rawPhone)
        val login = user.login

        if (isUserExist(login))
            throw IllegalArgumentException("A user with this phone already exists")
        map[login] = user
        return user
    }

    fun isValidPhone(phone: String):Boolean{
        var number_phone = ""
        for (ch in phone.indices){
            if (phone[ch].isDigit())
                number_phone += phone[ch]
        }
        if(number_phone.length != 11)
            return false
        return true
    }

    fun loginUser(login: String, password: String):String? {
        val phoneOrEmail = if (isValidPhone(login)) getValidPhone(login) else login
        map[phoneOrEmail]?.let { user ->
            return if(user.checkPassword(password))
                user.userInfo
            else
                null
        }
        return null
    }

    fun getValidPhone(phone: String):String?{
        var numberPhone = "+"
        for (ch in phone.indices){
            if (phone[ch].isDigit())
                numberPhone += phone[ch]
        }
        if(numberPhone.length != 12)
            return null
        return numberPhone
    }

    fun requestAccessCode(login: String) {
        val phone = (if (isValidPhone(login)) getValidPhone(login) else return) ?: return
        map[phone]?.regenerateAccessCode(login)
    }

    fun clearHolder(){
        map.clear()
    }

}