package com.occaecat.ztoeschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.model.ColorTheme
import com.occaecat.ztoeschedule.presentation.ui.info.FeedbackScreen
import com.occaecat.ztoeschedule.presentation.ui.info.GeminiChatScreen
import android.content.Intent
import com.occaecat.ztoeschedule.presentation.ui.info.AboutScreen
import com.occaecat.ztoeschedule.presentation.ui.info.FaqScreen
import com.occaecat.ztoeschedule.ui.theme.SvitloYeZhytomyrTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InfoActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: EnergyPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val type = intent.getStringExtra("type") ?: "about"

        setContent {
            val colorThemeState = preferencesManager.colorThemeFlow.collectAsState(initial = ColorTheme.System)
            val colorTheme = colorThemeState.value
            val cornerRadiusState = preferencesManager.cornerRadiusFlow.collectAsState(initial = 24)
            val cornerRadius = cornerRadiusState.value

            SvitloYeZhytomyrTheme(
                themePreference = colorTheme,
                cornerRadius = cornerRadius
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (type) {
                        "about", "donate" -> AboutScreen(onBack = { finish() })
                        "faq" -> FaqScreen(onBack = { finish() })
                        "feedback" -> FeedbackScreen(
                            onBack = { finish() },
                            onNavigateToGemini = {
                                val intent = Intent(this@InfoActivity, InfoActivity::class.java).apply {
                                    putExtra("type", "gemini")
                                }
                                startActivity(intent)
                            }
                        )
                        "gemini" -> GeminiChatScreen(onBack = { finish() })
                        else -> finish()
                    }
                }
            }
        }
    }
}
