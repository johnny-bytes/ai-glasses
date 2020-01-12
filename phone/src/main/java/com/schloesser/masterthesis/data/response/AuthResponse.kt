package com.schloesser.masterthesis.data.response

data class AuthResponse(
    var token: Token? = null
)

data class Token(
    var access: String,
    var refresh: String
)