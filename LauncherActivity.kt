package com.iosexperience.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.iosexperience.app.island.IslandService
import kotlinx.coroutines.flow.first
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { IosTheme { LauncherScreen() } }
        // Restore the island after a reboot or process death if the user left it switched on.
        lifecycleScope.launch {
            val enabled = SettingsRepository(applicationContext).settings.first().islandEnabled
            if (enabled && Settings.canDrawOverlays(this@LauncherActivity)) {
                ContextCompat.startForegroundService(
                    this@LauncherActivity,
                    Intent(this@LauncherActivity, IslandService::class.java),
                )
            }
        }
    }
}

data class AppInfo(val label: String, val pkg: String, val icon: ImageBitmap)

private fun loadApps(ctx: Context): List<AppInfo> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .map { ri ->
            AppInfo(
                label = ri.loadLabel(pm).toString(),
                pkg = ri.activityInfo.packageName,
                icon = ri.loadIcon(pm).toBitmap(128, 128).asImageBitmap(),
            )
        }
        .distinctBy { it.pkg }
        .sortedBy { it.label.lowercase() }
}

private fun launchApp(ctx: Context, pkg: String) {
    ctx.packageManager.getLaunchIntentForPackage(pkg)?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(it)
    }
}

@Composable
private fun Modifier.glass(s: AppSettings, radius: Int = 22): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    return if (s.glass) {
        this.clip(shape)
            .background(Color.White.copy(alpha = s.glassOpacity))
            .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
    } else {
        this.clip(shape).background(Color(0xCC1C1C1E))
    }
}

@Composable
fun LauncherScreen() {
    val ctx = LocalContext.current
    val repo = remember { SettingsRepository(ctx.applicationContext) }
    val s by repo.settings.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var tick by remember { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    LaunchedEffect(tick) { apps = withContext(Dispatchers.IO) { loadApps(ctx) } }

    val filtered = filterByLabel(apps, query) { it.label }
    val dockApps = apps.filter { it.pkg in s.dock }

    val onLong: (AppInfo) -> Unit = { app ->
        if (s.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch { repo.toggleDock(app.pkg) }
    }

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.search_apps), color = Color.White.copy(alpha = 0.7f)) },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).glass(s, 14),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(s.columns),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filtered, key = { it.pkg }) { app ->
                AppCell(app, s, showLabel = true, onClick = { launchApp(ctx, app.pkg) }, onLong = { onLong(app) })
            }
        }

        if (dockApps.isEmpty()) {
            Text(
                stringResource(R.string.dock_hint),
                style = TextStyle(color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center, shadow = labelShadow),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        } else {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).glass(s).padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dockApps.forEach { app ->
                    AppCell(app, s, showLabel = false, onClick = { launchApp(ctx, app.pkg) }, onLong = { onLong(app) })
                }
            }
        }
    }
}

private val labelShadow = Shadow(Color.Black.copy(alpha = 0.6f), blurRadius = 4f)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCell(app: AppInfo, s: AppSettings, showLabel: Boolean, onClick: () -> Unit, onLong: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && s.animations) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    val shape = if (s.iosIcons) RoundedCornerShape(s.cornerRadius.dp) else RoundedCornerShape(0.dp)

    Column(
        Modifier
            .scale(scale)
            .combinedClickable(interactionSource = source, indication = null, onClick = onClick, onLongClick = onLong)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier.size(60.dp).clip(shape),
        )
        if (showLabel) {
            Text(
                app.label,
                style = TextStyle(color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center, shadow = labelShadow),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
