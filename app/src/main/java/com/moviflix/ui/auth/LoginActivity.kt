package com.moviflix.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.moviflix.MoviFlixApp
import com.moviflix.R
import com.moviflix.ui.home.MainActivity
import com.moviflix.utils.SessionManager
import kotlinx.coroutines.*

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGuest: TextView
    private lateinit var pbLogin: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session = SessionManager(this)

        // Auto-skip if already logged in
        if (session.isLoggedIn) {
            startMain()
            return
        }

        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etLoginEmail)
        etPassword = findViewById(R.id.etLoginPassword)
        btnLogin = findViewById(R.id.btnLoginSubmit)
        tvGuest = findViewById(R.id.tvLoginGuest)
        pbLogin = findViewById(R.id.pbLogin)
        tvError = findViewById(R.id.tvLoginError)

        val app = application as MoviFlixApp

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString()
            if (email.isEmpty() || pass.isEmpty()) {
                tvError.text = "أدخل البريد الإلكتروني وكلمة المرور"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            tvError.visibility = View.GONE
            pbLogin.visibility = View.VISIBLE
            btnLogin.isEnabled = false

            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    try { app.repository.login(email, pass) } catch (e: Exception) { null }
                }
                pbLogin.visibility = View.GONE
                btnLogin.isEnabled = true

                val body = result?.body()
                if (body?.isSuccess == true && body.data != null) {
                    val data = body.data
                    session.token = data.token
                    session.userId = data.userInfo.userId
                    session.nickName = data.userInfo.nickName
                    session.email = data.userInfo.email
                    session.headImg = data.userInfo.headImgUrl
                    session.isVip = data.userInfo.userVipInfo?.payed ?: false
                    startMain()
                } else {
                    tvError.text = body?.msg ?: "فشل تسجيل الدخول"
                    tvError.visibility = View.VISIBLE
                }
            }
        }

        tvGuest.setOnClickListener { startMain() }
    }

    private fun startMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
