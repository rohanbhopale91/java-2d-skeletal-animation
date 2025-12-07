# AnimStudio - 2D Skeletal Animation Editor

A professional 2D skeletal animation software built with Java and JavaFX.

> **Note**: This project is being developed from the original Java 2D Skeletal Animation library demo.

## Original Demo

The original demo explores 2D skeletal animation similar to [Spine2D](http://esotericsoftware.com/spine-demos). Art assets from Ragnarok Battle Offline (owned by フランスパン) are used for demonstration:

![preview](https://user-images.githubusercontent.com/110074141/214950221-67784245-e299-4cae-94b9-68f3682c2964.png)

![preview](https://user-images.githubusercontent.com/110074141/214951140-1c9a56f0-088c-4423-a1b8-babe3afac0f8.gif)

---

## Requirements

- Java 17 or later
- Maven 3.8+

## Building the Editor

```bash
# Compile and package
mvn clean package

# Run with JavaFX plugin
mvn javafx:run
```

## Running the Original Demo

```bash
javac -d bin src/*.java
java -cp bin Game
```

## Project Structure

```
src/com/animstudio/
├── core/                    # Engine core (no UI dependencies)
│   ├── math/               # Vector2, Transform2D, MathUtil
│   ├── model/              # Bone, Skeleton, Slot, Attachment
│   ├── animation/          # Keyframe, KeyframeTrack, AnimationClip, AnimationState
│   ├── interpolation/      # Linear, Stepped, Bezier interpolators
│   └── event/              # Event bus system
├── editor/                  # JavaFX editor application
│   ├── commands/           # Undo/redo command system
│   ├── project/            # Project model
│   ├── tools/              # Editor tools (Select, Translate, Rotate, etc.)
│   └── ui/                 # UI panels
│       ├── canvas/         # Skeleton canvas view
│       ├── hierarchy/      # Bone tree view
│       ├── inspector/      # Property inspector
│       └── timeline/       # Animation timeline
└── test/                    # Test classes
```

## Features

### Core Engine
- Hierarchical bone system with parent-child transforms
- Multiple interpolation types: Linear, Stepped, Bezier
- Keyframe-based animation with typed tracks
- Event-driven architecture for decoupled components

### Editor (In Development)
- Visual bone editing with drag manipulation
- Undo/redo command system
- Animation timeline with keyframe editing
- Property inspector panel
- Dark theme UI

## Development Phases

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Core Engine (math, model, animation, events) | ✅ Complete |
| 2 | GUI Foundation (JavaFX, canvas, commands) | 🔄 In Progress |
| 3 | Editor Tools (selection, IK, mesh deformation) | ⏳ Planned |
| 4 | Automation (procedural animation, motion blend) | ⏳ Planned |
| 5 | Import/Export (JSON, Spine, sprite sheet) | ⏳ Planned |

## License

MIT License

## Disclaimer

Ragnarok Battle Offline is owned by フランスパン and thus the art assets are owned by them.
