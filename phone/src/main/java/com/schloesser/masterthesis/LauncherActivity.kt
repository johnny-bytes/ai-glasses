package com.schloesser.masterthesis

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.schloesser.masterthesis.data.repository.SessionRepository
import com.schloesser.masterthesis.presentation.internal.InternalActivity
import com.schloesser.masterthesis.presentation.login.LoginActivity

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SessionRepository.getInstance(this).hasSession()) {
            startActivity(Intent(this, InternalActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}
