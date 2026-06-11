# JavaFX 3D Demo

A simple interactive 3D showcase app built with JavaFX 25, demonstrating what Java desktop apps are capable of.

Made with the help of [Claude](https://claude.ai) by Anthropic.

## Features

- 3D shapes rotating in real time
- Drag to rotate the view
- Change the color of any shape
- Toggle wireframe mode
- Explode shapes into particles

## Download

Grab the installer for your platform from the [Releases](https://github.com/mrtngrega/JavaFX_Demo/releases) page:

| Platform | File |
|----------|------|
| Windows  |`.exe`|
| Linux (Ubuntu/Debian) | `.deb` |
| Linux (Fedora/RHEL)   | `.rpm` |
| macOS | `.dmg` |

No Java installation required — the runtime is bundled.

## Building from source

Requires Java 25 and JavaFX 25 installed at `/opt/javafx/lib`.

```bash
git clone https://github.com/mrtngrega/JavaFX_Demo.git
cd JavaFX_Demo
chmod +x build.sh
./build.sh
```

## Tech

- Java 25
- JavaFX 25 (3D graphics, animations)
- Built and packaged with `jpackage`
- CI/CD via GitHub Actions
