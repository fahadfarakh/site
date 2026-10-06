package com.ceenveiw.editor

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.ceenveiw.editor.model.Clip
import com.ceenveiw.editor.model.EditorProject
import com.ceenveiw.editor.model.TextLayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditorViewModel(app: Application) : AndroidViewModel(app) {
    private val _project = MutableStateFlow(EditorProject())
    val project: StateFlow<EditorProject> = _project.asStateFlow()

    private val undo = ArrayDeque<EditorProject>()
    private val redo = ArrayDeque<EditorProject>()

    private fun commit(next: EditorProject) {
        undo.addLast(_project.value)
        if (undo.size > 50) undo.removeFirst()
        redo.clear()
        _project.value = next
    }

    fun importMedia(uri: Uri, name: String = "Video") {
        val retriever = MediaMetadataRetriever()
        val duration = try {
            retriever.setDataSource(getApplication(), uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) { 0L } finally { retriever.release() }

        val current = _project.value
        val start = current.clips.maxOfOrNull { it.timelineStartMs + it.editedDurationMs } ?: 0L
        val clip = Clip(uri = uri, name = name, sourceDurationMs = duration, trimEndMs = duration, timelineStartMs = start)
        commit(current.copy(clips = current.clips + clip, selectedClipId = clip.id))
    }

    fun selectClip(id: String) { _project.value = _project.value.copy(selectedClipId = id) }

    fun splitSelected() {
        val p = _project.value
        val clip = p.clips.firstOrNull { it.id == p.selectedClipId } ?: return
        val local = (p.playheadMs - clip.timelineStartMs).coerceIn(0, clip.editedDurationMs)
        val sourceSplit = clip.trimStartMs + (local * clip.speed).toLong()
        if (sourceSplit <= clip.trimStartMs || sourceSplit >= clip.trimEndMs) return
        val left = clip.copy(trimEndMs = sourceSplit)
        val right = clip.copy(id = java.util.UUID.randomUUID().toString(), trimStartMs = sourceSplit, timelineStartMs = clip.timelineStartMs + left.editedDurationMs)
        val out = p.clips.flatMap { if (it.id == clip.id) listOf(left, right) else listOf(it) }
        commit(p.copy(clips = out, selectedClipId = right.id))
    }

    fun duplicateSelected() {
        val p = _project.value
        val c = p.clips.firstOrNull { it.id == p.selectedClipId } ?: return
        val copy = c.copy(id = java.util.UUID.randomUUID().toString(), timelineStartMs = c.timelineStartMs + c.editedDurationMs)
        commit(p.copy(clips = p.clips + copy, selectedClipId = copy.id))
    }

    fun deleteSelected() {
        val p = _project.value
        val id = p.selectedClipId ?: return
        commit(p.copy(clips = p.clips.filterNot { it.id == id }, selectedClipId = null))
    }

    fun setSpeed(speed: Float) = updateSelected { it.copy(speed = speed.coerceIn(.1f, 8f)) }
    fun setVolume(volume: Float) = updateSelected { it.copy(volume = volume.coerceIn(0f, 2f)) }
    fun setRotation(rotation: Float) = updateSelected { it.copy(rotation = rotation) }
    fun setScale(scale: Float) = updateSelected { it.copy(scale = scale.coerceIn(.1f, 5f)) }
    fun setOpacity(opacity: Float) = updateSelected { it.copy(opacity = opacity.coerceIn(0f, 1f)) }

    private fun updateSelected(change: (Clip) -> Clip) {
        val p = _project.value
        val id = p.selectedClipId ?: return
        commit(p.copy(clips = p.clips.map { if (it.id == id) change(it) else it }))
    }

    fun setPlayhead(ms: Long) { _project.value = _project.value.copy(playheadMs = ms.coerceAtLeast(0)) }

    fun addText(text: String) {
        if (text.isBlank()) return
        val p = _project.value
        val layer = TextLayer(text = text, startMs = p.playheadMs, endMs = p.playheadMs + 3000)
        commit(p.copy(textLayers = p.textLayers + layer))
    }

    fun setAspectRatio(value: String) { commit(_project.value.copy(aspectRatio = value)) }

    fun undo() {
        if (undo.isEmpty()) return
        redo.addLast(_project.value)
        _project.value = undo.removeLast()
    }

    fun redo() {
        if (redo.isEmpty()) return
        undo.addLast(_project.value)
        _project.value = redo.removeLast()
    }
}
