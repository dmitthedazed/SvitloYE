package com.occaecat.ztoeschedule.widget.glance

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.occaecat.ztoeschedule.MainActivity
import com.occaecat.ztoeschedule.R
import com.occaecat.ztoeschedule.data.local.EnergyPreferencesManager
import com.occaecat.ztoeschedule.data.repository.EnergyRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LightWidgetEntryPoint {
    fun energyRepository(): EnergyRepository
    fun energyPreferencesManager(): EnergyPreferencesManager
}

class LightWidget : GlanceAppWidget() {

    companion object {
        val KEY_STATUS = stringPreferencesKey("status")
        val KEY_NEXT_EVENT = stringPreferencesKey("next_event")
        val KEY_ADDRESS = stringPreferencesKey("address")
        val KEY_UPDATED = longPreferencesKey("updated")
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(100.dp, 100.dp),
            DpSize(200.dp, 100.dp),
            DpSize(200.dp, 200.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext ?: context
        val entryPoint = EntryPointAccessors.fromApplication(appContext, LightWidgetEntryPoint::class.java)
        val prefManager = entryPoint.energyPreferencesManager()

        // 1. Get the address name from the app's DataStore (fast)
        val selection = prefManager.savedSelectionFlow.first()
        
        // We do NOT fetch network here to avoid blocking the widget rendering
        
        provideContent {
            val prefs = currentState<Preferences>()
            
            // Use address from app config if available, otherwise from widget cache
            val address = selection?.addressName ?: prefs[KEY_ADDRESS]
            val status = prefs[KEY_STATUS] ?: "unknown"
            val nextEvent = prefs[KEY_NEXT_EVENT] ?: "--:--"
            val currentContext = LocalContext.current
            
            SvitloYeGlanceTheme {
                val size = LocalSize.current
                
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .clickable(actionStartActivity(Intent(currentContext, MainActivity::class.java)))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (address == null) {
                        Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                            Text("Налаштуйте адресу", style = TextStyle(color = GlanceTheme.colors.onSurface))
                            Spacer(GlanceModifier.height(8.dp))
                            androidx.glance.Button(
                                text = "Налаштувати",
                                onClick = actionStartActivity(
                                    Intent(currentContext, MainActivity::class.java).apply {
                                        putExtra("configure_widget", true)
                                    }
                                )
                            )
                        }
                    } else {
                        when {
                            size.height >= 180.dp -> FullLayout(status, nextEvent, address)
                            size.width >= 180.dp -> RowLayout(status, nextEvent)
                            else -> SmallLayout(status, nextEvent)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SmallLayout(status: String, nextEvent: String) {
        Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
            StatusIcon(status, 32.dp)
            Spacer(GlanceModifier.height(8.dp))
            Text(
                text = if(status == "red") "Немає" else if(status == "unknown") "Оновлення" else "Є світло",
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold)
            )
            if (status != "unknown") {
                Text(text = nextEvent, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
            }
        }
    }

    @Composable
    private fun RowLayout(status: String, nextEvent: String) {
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            StatusIcon(status, 40.dp)
            Spacer(GlanceModifier.width(12.dp))
            Column {
                Text(
                    text = if(status == "red") "Відключення" else if(status == "unknown") "Синхронізація..." else "Світло є",
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                )
                if (status != "unknown") {
                    Text(text = "До $nextEvent", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                }
            }
        }
    }

    @Composable
    private fun FullLayout(status: String, nextEvent: String, address: String) {
        Column(modifier = GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
            RowLayout(status, nextEvent)
            Spacer(GlanceModifier.height(12.dp))
            Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(GlanceTheme.colors.outline)) {}
            Spacer(GlanceModifier.height(8.dp))
            Text(text = address, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp), maxLines = 2)
        }
    }

    @Composable
    private fun StatusIcon(status: String, size: androidx.compose.ui.unit.Dp) {
        val iconRes = if (status == "red") R.drawable.ic_home_filled else R.drawable.ic_bolt
        val tint = when (status) {
            "red" -> ColorProvider(Color.Red)
            "green", "white" -> ColorProvider(Color.Green)
            "yellow" -> ColorProvider(Color.Yellow)
            else -> GlanceTheme.colors.onSurfaceVariant
        }
        Image(provider = ImageProvider(iconRes), contentDescription = null, modifier = GlanceModifier.size(size), colorFilter = ColorFilter.tint(tint))
    }
}