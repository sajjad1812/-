package com.iosexperience.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.iosexperience.app.island.IslandService
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { IosTheme { DashboardScreen(onSetDefault = ::openHomeSettings) } }
    }

    private fun openHomeSettings() {
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}

@Composable
fun DashboardScreen(onSetDefault: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { SettingsRepository(ctx.applicationContext) }
    val s by repo.settings.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val onIslandToggle: (Boolean) -> Unit = { on ->
        if (!on) {
            scope.launch { repo.setIslandEnabled(false) }
        } else if (!Settings.canDrawOverlays(ctx)) {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + ctx.packageName)),
            )
        } else {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            scope.launch {
                repo.setIslandEnabled(true)
                ContextCompat.startForegroundService(ctx, Intent(ctx, IslandService::class.java))
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.systemBarsPadding(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            item {
                Button(onClick = onSetDefault, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.set_default))
                }
            }
            item { SectionTitle(stringResource(R.string.section_modules)) }
            item {
                GroupCard {
                    ToggleRow(stringResource(R.string.mod_icons), s.iosIcons) { scope.launch { repo.setIosIcons(it) } }
                    ToggleRow(stringResource(R.string.mod_glass), s.glass) { scope.launch { repo.setGlass(it) } }
                    ToggleRow(stringResource(R.string.mod_anim), s.animations) { scope.launch { repo.setAnimations(it) } }
                    ToggleRow(stringResource(R.string.mod_haptics), s.haptics) { scope.launch { repo.setHaptics(it) } }
                    ToggleRow(stringResource(R.string.mod_island), s.islandEnabled, onIslandToggle)
                    Text(
                        stringResource(R.string.island_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    SliderRow(stringResource(R.string.island_offset), s.islandTopOffset, 0f..60f, 0) {
                        scope.launch { repo.setIslandTopOffset(it) }
                    }
                }
            }
            item {
                GroupCard {
                    PlannedRow(stringResource(R.string.mod_control))
                    PlannedRow(stringResource(R.string.mod_lock))
                    PlannedRow(stringResource(R.string.mod_keyboard))
                }
            }
            item {
                Text(
                    stringResource(R.string.planned_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { SectionTitle(stringResource(R.string.section_appearance)) }
            item {
                GroupCard {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val shape = if (s.iosIcons) RoundedCornerShape(s.cornerRadius.dp) else RoundedCornerShape(0.dp)
                        Box(
                            Modifier
                                .size(72.dp)
                                .clip(shape)
                                .background(Brush.verticalGradient(listOf(IosBlue, androidx.compose.ui.graphics.Color(0xFF5AC8FA)))),
                        )
                    }
                    SliderRow(stringResource(R.string.corner_radius), s.cornerRadius, 0f..30f, 0) {
                        scope.launch { repo.setCornerRadius(it) }
                    }
                    SliderRow(stringResource(R.string.grid_columns), s.columns.toFloat(), 3f..6f, 2) {
                        scope.launch { repo.setColumns(it.roundToInt()) }
                    }
                    SliderRow(stringResource(R.string.glass_opacity), s.glassOpacity, 0.05f..0.6f, 0) {
                        scope.launch { repo.setGlassOpacity(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun GroupCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { content() }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PlannedRow(label: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Text(
            stringResource(R.string.status_planned),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}
