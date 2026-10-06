package com.ceenveiw.editor

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.ceenveiw.editor.model.Clip
import com.ceenveiw.editor.model.EditorProject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CeenveiwTheme { EditorApp() } }
    }
}

@Composable
private fun CeenveiwTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF111111),
            onPrimary = Color.White,
            background = Color(0xFFF7F7F7),
            surface = Color.White
        ),
        content = content
    )
}

@Composable
fun EditorApp(vm: EditorViewModel = viewModel()) {
    val project by vm.project.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(project.clips.isNotEmpty()) }
    if (!editing && project.clips.isEmpty()) {
        HomeScreen { vm.importMedia(it); editing = true }
    } else {
        EditorScreen(project, vm) { editing = false }
    }
}

@Composable
private fun HomeScreen(onPicked: (Uri) -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(onPicked) }
    Column(
        Modifier.fillMaxSize().background(Color.White).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("ceenveiw", fontSize = 30.sp)
        Text("Create. Cut. Tell your story.", color = Color.Gray)
        Card(
            Modifier.fillMaxWidth().height(170.dp).clickable { picker.launch(arrayOf("video/*")) },
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F2))
        ) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.AddCircle, null, Modifier.size(48.dp))
                Spacer(Modifier.height(10.dp))
                Text("New Project", fontSize = 20.sp)
                Text("Import video and start editing", color = Color.Gray)
            }
        }
        Text("Quick tools", fontSize = 18.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile("Trim", Icons.Default.ContentCut)
            QuickTile("Text", Icons.Default.TextFields)
            QuickTile("Music", Icons.Default.MusicNote)
        }
    }
}

@Composable
private fun QuickTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null)
            Spacer(Modifier.height(5.dp))
            Text(label)
        }
    }
}

@Composable
private fun EditorScreen(project: EditorProject, vm: EditorViewModel, onBack: () -> Unit) {
    val selected = project.clips.firstOrNull { it.id == project.selectedClipId }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::importMedia) }
    var addText by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color(0xFFF6F6F6))) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("ceenveiw", fontSize = 20.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = vm::undo) { Icon(Icons.Default.Undo, "Undo") }
            IconButton(onClick = vm::redo) { Icon(Icons.Default.Redo, "Redo") }
            Button(onClick = {}, enabled = false, shape = RoundedCornerShape(12.dp)) { Text("Export") }
        }

        Preview(selected, project)
        Transport(project, vm)
        Timeline(project, vm)
        Toolbar(
            selected = selected,
            project = project,
            add = { picker.launch(arrayOf("video/*")) },
            split = vm::splitSelected,
            copy = vm::duplicateSelected,
            delete = vm::deleteSelected,
            text = { addText = true },
            speed = vm::setSpeed,
            rotate = { vm.setRotation((selected?.rotation ?: 0f) + 90f) },
            opacity = vm::setOpacity,
            volume = vm::setVolume,
            ratio = vm::setAspectRatio
        )
    }

    if (addText) {
        var value by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addText = false },
            title = { Text("Add text") },
            text = { OutlinedTextField(value, { value = it }, label = { Text("Text") }) },
            confirmButton = { TextButton(onClick = { vm.addText(value); addText = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { addText = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun Preview(selected: Clip?, project: EditorProject) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(selected?.uri) {
        if (selected != null) {
            player.setMediaItem(MediaItem.fromUri(selected.uri))
            player.prepare()
            player.seekTo(selected.trimStartMs)
        }
    }
    Box(
        Modifier.fillMaxWidth().height(320.dp).background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (selected == null) {
            Text("Add media", color = Color.White)
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { PlayerView(it).apply { this.player = player; useController = true } }
            )
        }
        project.textLayers.filter { project.playheadMs in it.startMs..it.endMs }.forEach {
            Text(it.text, color = Color.White, fontSize = it.fontSizeSp.sp)
        }
    }
}

@Composable
private fun Transport(project: EditorProject, vm: EditorViewModel) {
    val duration = project.clips.maxOfOrNull { it.timelineStartMs + it.editedDurationMs } ?: 1L
    Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp)) {
        Slider(
            value = project.playheadMs.coerceAtMost(duration).toFloat(),
            onValueChange = { vm.setPlayhead(it.toLong()) },
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f)
        )
        Text("${project.playheadMs / 1000f}s / ${duration / 1000f}s", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun Timeline(project: EditorProject, vm: EditorViewModel) {
    Row(
        Modifier.fillMaxWidth().height(92.dp).background(Color(0xFF222222)).horizontalScroll(rememberScrollState()).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        project.clips.forEach { clip ->
            val active = clip.id == project.selectedClipId
            Surface(
                modifier = Modifier.width((clip.editedDurationMs / 60).coerceIn(70, 220).dp).fillMaxHeight().clickable { vm.selectClip(clip.id) },
                color = if (active) Color(0xFFE5E5E5) else Color(0xFF444444),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Movie, null, tint = if (active) Color.Black else Color.White)
                    Text(clip.name, maxLines = 1, color = if (active) Color.Black else Color.White, fontSize = 12.sp)
                    Text("%.1fs".format(clip.editedDurationMs / 1000f), color = if (active) Color.DarkGray else Color.LightGray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun Toolbar(
    selected: Clip?,
    project: EditorProject,
    add: () -> Unit,
    split: () -> Unit,
    copy: () -> Unit,
    delete: () -> Unit,
    text: () -> Unit,
    speed: (Float) -> Unit,
    rotate: () -> Unit,
    opacity: (Float) -> Unit,
    volume: (Float) -> Unit,
    ratio: (String) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Tool("Add", Icons.Default.Add, add)
        Tool("Split", Icons.Default.ContentCut, split)
        Tool("Copy", Icons.Default.ContentCopy, copy)
        Tool("Text", Icons.Default.TextFields, text)
        Tool("Speed", Icons.Default.Speed) { speed(if ((selected?.speed ?: 1f) == 1f) 2f else 1f) }
        Tool("Rotate", Icons.Default.RotateRight, rotate)
        Tool("Opacity", Icons.Default.Opacity) { opacity(if ((selected?.opacity ?: 1f) > .6f) .5f else 1f) }
        Tool("Mute", Icons.Default.VolumeOff) { volume(if ((selected?.volume ?: 1f) > 0f) 0f else 1f) }
        Tool("Canvas", Icons.Default.AspectRatio) { ratio(if (project.aspectRatio == "9:16") "16:9" else "9:16") }
        Tool("Delete", Icons.Default.Delete, delete)
    }
}

@Composable
private fun Tool(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, click: () -> Unit) {
    Column(Modifier.width(70.dp).clickable(onClick = click).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label)
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 11.sp)
    }
}
