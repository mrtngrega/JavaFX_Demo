#!/bin/bash
set -e

# ── Config ────────────────────────────────────────────────────────────────────
APP_NAME="JavaFXDemo"
APP_VERSION="1.0"
MAIN_CLASS="ShowcaseApp"
JAVAFX_PATH="/opt/javafx/lib"
JAVAFX_MODULES="javafx.controls,javafx.graphics"
JAVA_SECURITY="/usr/lib/jvm/java-25-openjdk/conf/security/java.security"

BUILD_DIR="build"
JAR_DIR="$BUILD_DIR/jar"
CLASSES_DIR="$BUILD_DIR/classes"
DIST_DIR="dist"

# ── Clean ─────────────────────────────────────────────────────────────────────
echo "→ Cleaning previous build..."
rm -rf "$BUILD_DIR" "$DIST_DIR"
mkdir -p "$CLASSES_DIR" "$JAR_DIR/javafx-libs" "$DIST_DIR"

# ── Compile ───────────────────────────────────────────────────────────────────
echo "→ Compiling..."
javac \
  --module-path "$JAVAFX_PATH" \
  --add-modules "$JAVAFX_MODULES" \
  -d "$CLASSES_DIR" \
  ShowcaseApp.java MainScene.java ShapeFactory.java ExplodeEffect.java

# ── Copy resources ────────────────────────────────────────────────────────────
echo "→ Copying resources..."
cp logo.png "$CLASSES_DIR/" 2>/dev/null || true

# ── Copy JavaFX native libs ───────────────────────────────────────────────────
echo "→ Bundling JavaFX native libs..."
cp "$JAVAFX_PATH"/*.so "$JAR_DIR/javafx-libs/"
cp "$JAVAFX_PATH"/*.jar "$JAR_DIR/javafx-libs/"

# ── Build JAR ─────────────────────────────────────────────────────────────────
echo "→ Building JAR..."
cat > "$JAR_DIR/MANIFEST.MF" << MANIFEST
Manifest-Version: 1.0
Main-Class: $MAIN_CLASS
MANIFEST

jar cfm "$JAR_DIR/app.jar" "$JAR_DIR/MANIFEST.MF" -C "$CLASSES_DIR" .

# ── Swap java.security for jpackage ──────────────────────────────────────────
echo "→ Temporarily restoring upstream java.security..."
sudo cp "${JAVA_SECURITY}.upstream" "$JAVA_SECURITY"

# ── jpackage ──────────────────────────────────────────────────────────────────
echo "→ Running jpackage..."
jpackage \
  --type rpm \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --input "$JAR_DIR" \
  --main-jar app.jar \
  --main-class "$MAIN_CLASS" \
  --module-path "$JAVAFX_PATH" \
  --add-modules "$JAVAFX_MODULES" \
  --java-options "--add-modules $JAVAFX_MODULES" \
  --java-options "-Djava.library.path=\$APPDIR/javafx-libs" \
  --icon logo.png \
  --dest "$DIST_DIR" \
  --linux-shortcut \
  --linux-menu-group "Education" \
  --linux-app-category "Education" \
  --vendor "Martin"

# ── Restore Fedora java.security ──────────────────────────────────────────────
echo "→ Restoring Fedora java.security..."
sudo cp "${JAVA_SECURITY}.fedora" "$JAVA_SECURITY"

echo ""
echo "✓ Done! Installer created in ./$DIST_DIR/"
echo ""
echo "To install, run:"
echo "  sudo rpm -i $DIST_DIR/javafxdemo-${APP_VERSION}-1.x86_64.rpm"
echo ""
echo "Then launch from your app menu or run: $APP_NAME"