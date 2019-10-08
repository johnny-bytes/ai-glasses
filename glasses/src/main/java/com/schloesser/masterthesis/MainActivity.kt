package com.schloesser.masterthesis

import android.os.Bundle
import com.vuzix.hud.actionmenu.ActionMenuActivity

class MainActivity : ActionMenuActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
