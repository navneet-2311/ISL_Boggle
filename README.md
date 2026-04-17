# ISL Boggle — Android (Java)

Real-time Indian Sign Language recognition + Boggle-style word game.

## Pipeline
Camera (CameraX) → MediaPipe Hand Landmarker → flatten 21 (x,y) → float[1,42] → TFLite → argmax + threshold → 10-frame majority vote → append letter → dictionary check → score.

## Required asset files (you must add these)

Drop into `app/src/main/assets/`:

1. **`model.tflite`** — your trained classifier.
   - Input shape: `[1, 42]` (21 landmarks × x,y, normalized 0–1, MediaPipe order)
   - Output shape: `[1, 26]` (A–Z). If your model outputs a different `N`, update `Labels.LABELS` accordingly.
2. **`hand_landmarker.task`** — MediaPipe Hand Landmarker bundle.
   Download from Google's MediaPipe model zoo:
   https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task
3. **`dictionary.txt`** — already included with sample words; replace with your own (one word per line).

## Build

Open in Android Studio (Hedgehog or newer), let Gradle sync, then Run on a device with a camera.

- `compileSdk` 34, `minSdk` 24, Java 17
- Front camera is used by default (edit `CameraManager` to switch).

## Module map
- `MainActivity` — wires pipeline + UI
- `CameraManager` — CameraX preview + ImageAnalysis on background executor
- `MediaPipeHandler` — Hand Landmarker (IMAGE mode)
- `LandmarkProcessor` — 21 landmarks → `float[42]` (x,y only, z ignored)
- `ModelRunner` — TFLite Interpreter, `[1,42] → [1,N]`
- `PredictionManager` — argmax + 0.8 threshold + 10-frame majority vote
- `GameEngine` — current word, score, dictionary validation
- `Labels` — index → letter mapping

## Tuning
- Threshold / window / majority: `PredictionManager` constants.
- Append cooldown (anti-double-fire): `MainActivity.APPEND_COOLDOWN_MS`.
- Auto-commit word on each append is enabled; remove the `tryCommitWord()` call inside `onFrame` to require manual commit only.
