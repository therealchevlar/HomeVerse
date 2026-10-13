# HOMEVERSE — Interactive 3D Digital Home & Spatial Walkthrough

A native Android spatial computing application that captures real interior spaces and reconstructs them into an interactive 3D digital twin, a first-person walkable game-like environment, and a panoramic virtual tour.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The architectural directions below were confirmed in Phase 1 clarification:
> - **3D Engine & Rendering**: Native Android 3D projection & rendering engine integrated into Jetpack Compose with 60 FPS touch orbit, first-person joystick navigation, dollhouse ceiling cutaways, and interactive room selection.
> - **Reconstruction Pipeline**: Complete on-device capture and processing pipeline generating valid capture packages, camera pose estimations, 3D point clouds, polygon meshes, and asset manifests.
> - **Default Showcase Project**: "Villa Lumina" — a modern architectural multi-room residence bundled with full 3D geometry, dollhouse viewing, first-person collision walkthrough, and 360° virtual tour viewpoints.

---

## 1. Overview & Core Concept

**HOMEVERSE** bridges the physical and digital world for homes:
- **What It Does**: Allows homeowners, architects, renters, and real estate professionals to scan physical rooms using the device camera and motion sensors, reconstruct the geometry and visual texture, and interact with the digital replica in three unified viewing modes.
- **The Three Viewing Modes**:
  1. **Digital Twin (Dollhouse)**: Orbiting 3D architectural perspective with floor selection, room clipping, dimension readouts, and spatial highlights.
  2. **Explore Mode**: First-person game-like exploration with dual on-screen joysticks (move & look), boundary collision, and spatial room transitions.
  3. **Virtual Tour**: Curated guided walkthrough across panoramic viewpoints with hotspots, room audio/text annotations, and directional teleports.
- **Target Audience**: Homeowners wanting digital records for renovations, interior design enthusiasts, property managers, and spatial computing early adopters.

---

## 2. User Experience & Visual Design

### Core User Flows

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│ 1. Onboarding   │──────>│ 2. Dashboard    │──────>│ 3. Create Home  │
│ Privacy & Perms │       │ Projects & Demo │       │ Rooms & Floors  │
└─────────────────┘       └─────────────────┘       └────────┬────────┘
                                                             │
┌─────────────────┐       ┌─────────────────┐       ┌────────▼────────┐
│ 6. Explore Mode │<──────│ 5. Digital Twin │<──────│ 4. Scan Studio  │
│ 1st-Person Walk │       │ 3D Dollhouse    │       │ Real Camera HUD │
└────────┬────────┘       └────────┬────────┘       └────────┬────────┘
         │                         │                         │
         └────────►┌───────────────┴───────────────┐◄────────┘
                   │ 7. Virtual Tour & Annotations │
                   │ 360 Waypoints & Asset Export  │
                   └───────────────────────────────┘
```

1. **Onboarding & Privacy Hub**:
   - Transparent disclosure of camera and sensor data processing before permission requests.
   - Immediate option to launch the pre-reconstructed **Villa Lumina** showcase home or scan a new house.
2. **Dashboard**:
   - Project cards displaying thumbnail, floor/room counts, square footage estimates, and processing statuses.
   - Quick-access stats (total rooms captured, stored versions, mesh complexity).
3. **Create Home & Room Setup**:
   - Multi-floor and room configuration (Living Room, Kitchen, Master Bedroom, Balcony, etc.) with customizable dimensions and tags.
4. **Scan Studio (Live Capture)**:
   - Real-time CameraX preview with spatial HUD (horizon leveling artificial horizon, tracking confidence grid, coverage progress meter).
   - Sensor-assisted pose tracking (gyroscope + accelerometer orientation vector integration).
   - Capture burst / timed keyframe sampler capturing images and sensor pose metadata into a structured package.
5. **Reconstruction Pipeline Monitor**:
   - 6-stage asynchronous processing pipeline with real progress logs:
     `Validate Capture` ➔ `Pose Estimation` ➔ `Feature Matching` ➔ `Point Cloud Densification` ➔ `Mesh Surface Reconstruction` ➔ `Manifest Generation`.
6. **Digital Twin Viewer (3D Dollhouse)**:
   - Orbiting camera around 3D house structure, pinch-to-zoom, two-finger pan.
   - Floor cutaway toggle (roof removed for dollhouse view), room isolation tap, room info cards.
7. **Explore Mode**:
   - First-person camera with left joystick for movement (forward/back/strafe) and right touch pad for look/aim.
   - Wall boundary constraints and room detection when walking across doorways.
8. **Virtual Tour Mode**:
   - Interactive waypoint node graph linking room viewpoints with smooth camera transitions and contextual room annotations.

### Visual Identity & Theme Tokens

- **Aesthetic**: Spatial computing dark mode — sleek, technical, precision-crafted.
- **Color Palette**:
  - `Background`: Deep Midnight Obsidian (`#080C14`)
  - `Surface`: Slate Glass (`#111827` with subtle cyan border `#1E293B`)
  - `Primary / Accent`: Electric Cyber Cyan (`#00F0FF`)
  - `Secondary Accent`: Luminescent Violet / Blue (`#3B82F6`)
  - `Status Success`: Neon Emerald (`#10B981`)
  - `Status Warning`: Electric Amber (`#F59E0B`)
  - `Text Primary`: Crisp Arctic White (`#F8FAFC`)
  - `Text Secondary`: Muted Slate (`#94A3B8`)
- **Typography**: Clean, geometric sans-serif hierarchy with monospaced precision telemetry readouts for coordinates, angles, and camera poses.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Pure Native Jetpack Compose 3D Engine
- **Chosen Approach**: Built-in 3D engine implemented with pure Kotlin vector math (Matrix4x4, Vector3, Quaternions, perspective projection, depth-sorted polygon painter, wireframe/shaded rendering, and camera frustum).
- **Why**: Zero external binary bloat, no WebGL/WebView bridge overhead, 100% testable in JVM/Robolectric, smooth 60 FPS Compose Canvas integration, guaranteed compatibility with the AI Studio streaming preview.
- **Alternative Considered**: Embedding Unity or web-based Three.js — discarded due to lack of Unity Editor in headless containers and webview sandbox latency.

### Decision 2: Realistic On-Device Reconstruction Pipeline
- **Chosen Approach**: A multi-stage processing pipeline that takes real captured frames, generates camera pose trajectories from sensor orientation and optical flow approximations, reconstructs a 3D point cloud, extracts room boundary planes, and builds a textured/shaded 3D mesh saved to the device storage.
- **Why**: Provides an end-to-end working pipeline that does not fabricate fake scans, complies with zero mock data rules, and runs completely locally without external cloud dependencies.

### Decision 3: Local Persistence via Room Database & File Storage
- **Chosen Approach**: Room Database for metadata, floor plans, room entities, capture sessions, and annotations, paired with local app storage for capture packages and asset manifests (`manifest.json`, `mesh.obj`, `textures`).
- **Why**: High-performance reactive flows (`Flow<List<HomeEntity>>`), strict schema validation, offline-first reliability.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      Jetpack Compose UI                         │
│  ┌───────────────┐ ┌────────────────┐ ┌──────────────────────┐  │
│  │ DashboardView │ │ ScanStudioView │ │ ThreeMode3DViewer    │  │
│  │ & HomeManager │ │ Real CameraHUD │ │ Twin / Explore / Tour│  │
│  └───────┬───────┘ └────────┬───────┘ └──────────┬───────────┘  │
└──────────┼──────────────────┼────────────────────┼──────────────┘
           ▼                  ▼                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Homeverse ViewModel                          │
│  - Active Home & Mode State (Twin | Explore | Tour)             │
│  - Camera Controller (Orbit / First-Person / Waypoints)         │
│  - Capture Session Manager & Reconstruction Job StateFlow       │
└──────────────────┬───────────────────────┬──────────────────────┘
                   ▼                       ▼
┌───────────────────────────────┐ ┌───────────────────────────────┐
│    3D Engine & Math Core      │ │    Reconstruction Service     │
│  - Matrix4x4, Vector3, Plane  │ │  - Frame Sampler & Pose Est.  │
│  - Camera3D (FOV, Yaw, Pitch) │ │  - Point Cloud Generator      │
│  - Polygon Rasterizer & Sort  │ │  - Room Boundary Extractor    │
│  - Collision Detection System │ │  - OBJ & Manifest Serializer  │
└───────────────────────────────┘ └───────────────┬───────────────┘
                                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                 Data Persistence & File Storage                 │
│  - Room Database (Home, Floor, Room, CaptureSession, Job)       │
│  - FileStorage: Capture Packages (images + metadata JSON)       │
│  - FileStorage: Asset Manifests (3D Mesh, Points, Panoramas)    │
└─────────────────────────────────────────────────────────────────┘
```

### Data Models & Entities

- **`HomeEntity`**: `id`, `name`, `description`, `createdAt`, `coverImagePath`, `totalFloors`, `totalRooms`, `isSample`
- **`FloorEntity`**: `id`, `homeId`, `floorNumber`, `name`, `levelElevationMeters`
- **`RoomEntity`**: `id`, `floorId`, `homeId`, `name`, `roomType`, `widthMeters`, `lengthMeters`, `heightMeters`, `colorHex`, `reconstructionStatus`
- **`CaptureSessionEntity`**: `id`, `roomId`, `homeId`, `frameCount`, `startedAt`, `completedAt`, `packagePath`, `status`
- **`ReconstructionJobEntity`**: `id`, `captureSessionId`, `roomId`, `currentStage`, `progressPercent`, `status`, `logOutput`, `assetManifestPath`
- **`AssetManifest`**: Structured JSON defining 3D vertices, polygon faces, wall planes, furniture bounding boxes, and 360 waypoint tour nodes.

### Interactive State & Component Mapping

| UI Action | Triggered Operation | State Mutation / Reaction |
|---|---|---|
| **Launch App** | Database check & seed `Villa Lumina` showcase if empty | Emits reactive list with pre-reconstructed home |
| **New Home / Room** | Room creation dialog with dimensions & type | Inserts `HomeEntity` & `RoomEntity`, navigates to Scan Studio |
| **Start Scan** | CameraX preview bind + SensorManager orientation listener | Live video feed, dynamic artificial horizon & coverage circle |
| **Snap / Burst Capture** | Captures frame bitmap + sensor Euler angles (yaw, pitch, roll) | Increments frame counter, persists to session directory |
| **Process Scan** | Launches coroutine `ReconstructionPipeline.process()` | Emits progress through 6 stages: Validation -> Pose -> Mesh |
| **Switch to Digital Twin** | Activates Orbit Camera, Dollhouse roof toggle | Renders multi-room 3D isometric layout with room selection |
| **Switch to Explore Mode** | Activates First-Person Camera + Dual On-Screen Joysticks | Touch drag updates player (X, Z) and camera (Yaw, Pitch) |
| **Switch to Virtual Tour** | Activates Waypoint Tour Controller | Camera smoothly interpolates between room nodes |

---

## 5. Verification & Testing Plan

1. **Gradle Compilation**: Ensure all dependencies (`camera-camera2`, `camera-lifecycle`, `room`, `compose-material3`, `lifecycle-runtime-compose`) compile cleanly via `compile_applet`.
2. **Robolectric JVM Unit Tests**:
   - `Matrix4x4Test` & `Camera3DTest`: Verify 3D projection math, matrix multiplication, and view-frustum culling.
   - `ReconstructionPipelineTest`: Verify ingestion of capture packages, generation of valid 3D vertices, faces, and asset manifest JSON.
   - `HomeRepositoryTest`: Verify Room database CRUD operations for homes, floors, and rooms.
3. **End-to-End User Experience Verification**:
   - Verification of `Villa Lumina` showcase home rendering in 3D Dollhouse, Explore mode with joysticks, and Waypoint Tour.
   - Verification of new home scan flow, photo capture, reconstruction job execution, and viewing newly reconstructed rooms.
