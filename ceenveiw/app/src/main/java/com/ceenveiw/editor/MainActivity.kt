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
            surface = Color.White,
            background = Color(0xFFF7F7F7)
        ),
        content = content
    )
}

@Composable
fun EditorApp(vm: EditorViewModel = viewModel()) {
    val project by vm.project.collectAsStateWithLifecycle()
    var showEditor by remember { mutableStateOf(project.clips.isNotEmpty()) }

    if (!showEditor && project.clips.isEmpty()) {
        HomeScreen(onMediaPicked = { uri -> vm.importMedia(uri); showEditor = true })
    } else {
        EditorScreen(project, vm, onBack = { showEditor = false })
    }
}

@Composable
private fun HomeScreen(onMediaPicked: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onMediaPicked)
    }
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("ceenveiw", fontSize = 30.sp, style = MaterialTheme.typography.headlineMedium)
        Text("Create. Cut. Tell your story.", color = Color.Gray)
        Card(
            modifier = Modifier.fillMaxWidth().height(170.dp).clickable { launcher.launch(arrayOf("video/*")) },
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F2))
        ) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.AddCircle, null, modifier = Modifier.size(46.dp))
                Spacer(Modifier.height(12.dp))
                Text("New Project", fontSize = 20.sp)
                Text("Import video and start editing", color = Color.Gray)
            }
        }
        Text("Quick tools", fontSize = 18.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickTile("Trim", Icons.Default.ContentCut)
            QuickTile("Text", Icons.Default.TextFields)
            QuickTile("Music", Icons.Default.MusicNote)
        }
    }
}

@Composable
private fun QuickTile(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null)
            Spacer(Modifier.height(6.dp))
            Text(title)
        }
    }
}

@Composable
private fun EditorScreen(project: EditorProject, vm: EditorViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val selected = project.clips.firstOrNull { it.id == project.selectedClipId }
    val addMedia = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.importMedia(it) } }
    var textDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color(0xFFF6F6F6))) {
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("ceenveiw", fontSize = 20.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = vm::undo) { Icon(Icons.Default.Undo, "Undo") }
            IconButton(onClick = vm::redo) { Icon(Icons.Default.Redo, "Redo") }
            Button(onClick = { /* Export screen hook */ }, shape = RoundedCornerShape(12.dp)) { Text("Export") }
        }

        Preview(selected, project)
        Transport(project, vm)
        Timeline(project, vm)
        EditorToolbar(
            selected = selected,
            onAdd = { addMedia.launch(arrayOf("video/*")) },
            onSplit = vm::splitSelected,
            onDuplicate = vm::duplicateSelected,
            onDelete = vm::deleteSelected,
            onText = { textDialog = true },
            onSpeed = vm::setSpeed,
            onRotate = { vm.setRotation((selected?.rotation ?: 0f) + 90f) },
            onOpacity = vm::setOpacity
        )
    }

    if (textDialog) {
        var value by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { textDialog = false },
            title = { Text("Add text") },
            text = { OutlinedTextField(value, { value = it }, label = { Text("Text") }) },
            confirmButton = { TextButton(onClick = { vm.addText(value); textDialog = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { textDialog = false }) { Text("Cancel") } }
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
        Modifier.fillMaxWidth().weight(1f).background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (selected == null) Text("Add media", color = Color.White)
        else AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { PlayerView(it).apply { this.player = player; useController = false } }
        )
        project.textLayers.filter { project.playheadMs in it.startMs..it.endMs }.forEach {
            Text(it.text, color = Color.White, fontSize = it.fontSizeSp.sp)
        }
    }
}

@Composable
private fun Transport(project: EditorProject, vm: EditorViewModel) {
    val duration = project.clips.maxOfOrNull { it.timelineStartMs + it.editedDurationMs } ?: 1L
    Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Slider(
            value = project.playheadMs.coerceAtMost(duration).toFloat(),
            onValueChange = { vm.setPlayhead(it.toLong()) },
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.setPlayhead((project.playheadMs - 1000).coerceAtLeast(0)) }) { Icon(Icons.Default.Replay10, null) }
            Icon(Icons.Default.PlayCircle, null, modifier = Modifier.size(38.dp))
            IconButton(onClick = { vm.setPlayhead((project.playheadMs + 1000).coerceAtMost(duration)) }) { Icon(Icons.Default.Forward10, null) }
        }
    }
}

@Composable
private fun Timeline(project: EditorProject, vm: EditorViewModel) {
    Row(
        Modifier.fillMaxWidth().height(92.dp).background(Color(0xFF222222)).horizontalScroll(rememberScrollState()).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        project.clips.forEach { clip ->
            val selected = clip.id == project.selectedClipId
            Surface(
                modifier = Modifier.width((clip.editedDurationMs / 60).coerceIn(70, 220).dp).fillMaxHeight().clickable { vm.selectClip(clip.id) },
                color = if (selected) Color(0xFFE5E5E5) else Color(0xFF444444),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Movie, null, tint = if (selected) Color.Black else Color.White)
                    Text(clip.name, maxLines = 1, color = if (selected) Color.Black else Color.White, fontSize = 12.sp)
                    Text("%.1fs".format(clip.editedDurationMs / 1000f), color = if (selected) Color.DarkGray else Color.LightGray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun EditorToolbar(
    selected: Clip?,
    onAdd: () -> Unit,
    onSplit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onText: () -> Unit,
    onSpeed: (Float) -> Unit,
    onRotate: () -> Unit,
    onOpacity: (Float) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Tool("Add", Icons.Default.Add, onAdd)
        Tool("Split", Icons.Default.ContentCut, onSplit)
        Tool("Copy", Icons.Default.ContentCopy, onDuplicate)
        Tool("Text", Icons.Default.TextFields, onText)
        Tool("Speed", Icons.Default.Speed) { onSpeed(if ((selected?.speed ?: 1f) == 1f) 2f else 1f) }
        Tool("Rotate", Icons.Default.RotateRight, onRotate)
        Tool("Opacity", Icons.Default.Opacity) { onOpacity(if ((selected?.opacity ?: 1f) > .6f) .5f else 1f) }
        Tool("Delete", Icons.Default.Delete, onDelete)
    }
}

@Composable
private fun Tool(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, click: () -> Unit) {
    Column(
        Modifier.width(70.dp).clickable(onClick = click).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label)
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 11.sp)
    }
}
