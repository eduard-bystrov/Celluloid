package com.example.celluloid.data.sources

import android.app.Activity
import java.security.MessageDigest
import com.example.celluloid.data.models.User
import com.example.celluloid.ui.board.BoardActivity

class AuthSource(private val activity: Activity) {
    private val db = DbAdapter(activity, User::class)
    private val users = db.fetchAll().use { rows ->
        rows.map { it.apply { stars = StarsSet(activity, it.userName) } }.toMutableList()
    }

    enum class Status { Ok, NoUser, WrongPassword }

    fun login(username: String, password: String) =
        when (
            users.firstOrNull { it.userName == username }
                ?.let { it.passwordHash == passwordHash(password) }
            ) {
            true -> Status.Ok
            false -> Status.WrongPassword
            null -> Status.NoUser
        }

    fun register(username: String, password: String) {
        val user = User(username, passwordHash(password)).apply {
            stars = StarsSet(activity, username)
        }
        users.add(user)
        db.insert(user)
    }

    fun getCurrentUser() = getUser(
        activity.intent.extras!!.getString(BoardActivity.USERNAME)!!,
        activity.intent.extras!!.getString(BoardActivity.PWHASH)!!
    )

    fun getUser(username: String, passwordHash: String) =
        users.first { it.userName == username && it.passwordHash == passwordHash }

    fun passwordHash(password: String) = String(
        MessageDigest.getInstance("SHA-1").digest(password.toByteArray())
    )
}
