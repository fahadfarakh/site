# ceenveiw Android Video Editor

`ceenveiw` is a clean Android Studio video-editor project inspired by the workflow of modern mobile editors while using original source code.

## Open in Android Studio

Open the `ceenveiw` directory as the project root. Use JDK 17 and let Android Studio sync Gradle dependencies.

Package/application id: `com.ceenveiw.editor`

Minimum Android: API 26

Target/compile SDK: 35

## Working in this build

- ceenveiw-branded home screen and editor workspace
- Android document picker video import
- Media3/ExoPlayer video preview
- duration extraction from imported media
- sequential timeline clip model
- horizontal VN-style clip timeline
- clip selection
- project playhead and timeline scrubbing
- split clip at the project playhead
- duplicate clip
- delete clip
- clip speed state (0.1x to 8x model; quick 1x/2x control in UI)
- clip volume model
- rotation control
- scale model
- opacity control
- text-layer model and add-text dialog
- text overlay display at the active playhead range
- aspect-ratio project state
- 50-step undo / redo state history
- export resolution/FPS fields in the project model

## Architecture already prepared for expansion

The source models include video/audio/text/overlay track types and keyframes for position, scale, rotation and opacity. This allows the project to grow into a multi-track editor without replacing its data model.

## Recommended next engine additions

These should be implemented with original code or appropriately licensed libraries/models rather than copying another editor's binaries:

1. Media3 Transformer export pipeline for trim/speed/effects.
2. Multi-track audio mixer and waveform rendering.
3. Keyframe interpolation and transform preview.
4. Filters, brightness, contrast, saturation, temperature and LUT support.
5. Video transitions.
6. Crop, mirror, canvas/background and masks.
7. Chroma key shader.
8. Picture-in-picture overlays.
9. Voice-over recording and audio extraction.
10. Speech-to-text captions through Android/on-device/cloud provider interfaces.
11. Background-removal provider using a separately licensed segmentation model.
12. Motion tracking and stabilization providers.
13. Draft persistence and thumbnail generation.
14. Production export settings for 720p/1080p/4K, frame rate and bitrate.

## Notes

The project is not a CapCut or VN codebase and contains no copied proprietary source, AI models, templates, effects or licensed media from those applications. The goal is an independently maintainable ceenveiw editor with comparable user-facing capabilities.
