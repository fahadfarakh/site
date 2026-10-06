package com.ceenveiw.editor.model

import android.net.Uri
import java.util.UUID

enum class TrackType { VIDEO, AUDIO, TEXT, OVERLAY }

data class Keyframe(
    val timeMs: Long,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val opacity: Float = 1f
)

data class Clip(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val trackType: TrackType = TrackType.VIDEO,
    val sourceDurationMs: Long = 0,
    val trimStartMs: Long = 0,
    val trimEndMs: Long = sourceDurationMs,
    val timelineStartMs: Long = 0,
    val speed: Float = 1f,
    val volume: Float = 1f,
    val opacity: Float = 1f,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val keyframes: List<Keyframe> = emptyList()
) {
    val editedDurationMs: Long
        get() = (((trimEndMs - trimStartMs).coerceAtLeast(0)) / speed.coerceAtLeast(.1f)).toLong()
}

data class TextLayer(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val fontSizeSp: Float = 36f,
    val x: Float = .5f,
    val y: Float = .5f,
    val rotation: Float = 0f,
    val opacity: Float = 1f
)

data class EditorProject(
    val title: String = "Untitled project",
    val clips: List<Clip> = emptyList(),
    val textLayers: List<TextLayer> = emptyList(),
    val selectedClipId: String? = null,
    val playheadMs: Long = 0,
    val aspectRatio: String = "9:16",
    val backgroundColor: Long = 0xFF000000,
    val exportWidth: Int = 1080,
    val exportHeight: Int = 1920,
    val exportFps: Int = 30
)
