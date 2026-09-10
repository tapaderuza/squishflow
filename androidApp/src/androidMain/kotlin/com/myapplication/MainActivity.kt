package com.example.squishfocus

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FocusPreferences.initialize(this)
        intent.getStringExtra("focus_intercepted_package")?.let(InterceptEvents::intercept)
        setContent { MainView() }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("focus_intercepted_package")?.let(InterceptEvents::intercept)
    }
}
