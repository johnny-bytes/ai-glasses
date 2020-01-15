package com.schloesser.masterthesis.presentation.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.schloesser.masterthesis.BuildConfig
import com.schloesser.masterthesis.R
import com.schloesser.masterthesis.data.base.ApiFactory
import com.schloesser.masterthesis.data.repository.SessionRepository
import com.schloesser.masterthesis.data.request.LoginRequest
import com.schloesser.masterthesis.data.response.AuthResponse
import com.schloesser.masterthesis.presentation.extension.gone
import com.schloesser.masterthesis.presentation.extension.hideKeyboard
import com.schloesser.masterthesis.presentation.extension.visible
import com.schloesser.masterthesis.presentation.internal.InternalActivity
import kotlinx.android.synthetic.main.activity_login.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if(SessionRepository.getInstance(this).hasSession()) {
            startActivity(Intent(this@LoginActivity, InternalActivity::class.java))
            finish()
        }

        if (BuildConfig.DEBUG) {
            edtUsername.setText("timo")
            edtPassword.setText("testpw")
        }

        btnPerformLogin.setOnClickListener {
            it.hideKeyboard()
            loadingIndicator.visible()

            ApiFactory.getInstance(this).api.login(
                LoginRequest(
                    edtUsername.text.toString(),
                    edtPassword.text.toString()
                )
            ).enqueue(object : Callback<AuthResponse> {
                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    loadingIndicator.gone()
                    Toast.makeText(this@LoginActivity, t.localizedMessage, Toast.LENGTH_LONG).show()
                }

                override fun onResponse(
                    call: Call<AuthResponse>,
                    response: Response<AuthResponse>
                ) {
                    loadingIndicator.gone()

                    if (response.code() == 200) {
                        SessionRepository.getInstance(this@LoginActivity).accessToken = response.body()?.token?.access!!
                        SessionRepository.getInstance(this@LoginActivity).refreshToken = response.body()?.token?.refresh!!
                        startActivity(Intent(this@LoginActivity, InternalActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Falsche Login Daten.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })
        }


    }
}
