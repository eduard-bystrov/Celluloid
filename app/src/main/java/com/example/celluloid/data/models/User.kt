package com.example.celluloid.data.models

import com.example.celluloid.data.sources.StarsSet

data class User(val userName: String, val passwordHash: String) {
    lateinit var stars: StarsSet
}

