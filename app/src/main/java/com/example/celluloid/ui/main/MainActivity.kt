package com.example.celluloid.ui.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.example.celluloid.R
import com.example.celluloid.data.sources.AuthSource
import com.example.celluloid.ui.board.BoardActivity
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private val authSource by lazy { AuthSource(this) }

    private val view by lazy { findViewById<LinearLayout>(R.id.login_view) }
    private val username by lazy { findViewById<EditText>(R.id.username) }
    private val password by lazy { findViewById<EditText>(R.id.password) }
    private val confirmPassword by lazy { findViewById<EditText>(R.id.confirm_password) }

    private val doLogin by lazy { findViewById<Button>(R.id.login) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        username.afterTextChanged {
            confirmPassword.visibility = View.INVISIBLE
            updateLoginForm()
        }

        password.afterTextChanged { updateLoginForm() }

        doLogin.setOnClickListener {
            if (confirmPassword.isVisible) {
                if (password.text.toString() != confirmPassword.text.toString()) {
                    Snackbar.make(view, R.string.password_mismatch, Snackbar.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                authSource.register(username.text.toString(), password.text.toString())
            } else when (authSource.login(username.text.toString(), password.text.toString())) {
                AuthSource.Status.NoUser -> {
                    confirmPassword.visibility = View.VISIBLE
                    confirmPassword.text.clear()
                    updateLoginForm()
                    return@setOnClickListener
                }
                AuthSource.Status.WrongPassword -> {
                    Snackbar.make(view, R.string.wrong_password, Snackbar.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                else -> Unit
            }

            startActivity(
                Intent(this, BoardActivity::class.java).apply {
                    putExtra(BoardActivity.USERNAME, username.text.toString())
                    putExtra(
                        BoardActivity.PWHASH,
                        authSource.passwordHash(password.text.toString())
                    )
                }
            )
        }
    }

    private fun updateLoginForm() {
        doLogin.isEnabled = username.text.isNotEmpty() && password.text.isNotEmpty()
        doLogin.text = getString(
            if (confirmPassword.isVisible) R.string.prompt_sign_up else R.string.prompt_sign_in
        )
    }

    private fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) =
                afterTextChanged.invoke(editable.toString())

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) =
                Unit

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) =
                Unit
        })
    }
}