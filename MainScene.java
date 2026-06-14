import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.scene.transform.*;
import javafx.stage.*;
import javafx.util.Duration;

public class MainScene {

    private Stage stage;
    private Scene scene;
    private BorderPane root;
    private Image appIcon;
    private double dragOffsetX;
    private double dragOffsetY;

    // 3D
    private Group world;
    private PerspectiveCamera camera;
    private SubScene subScene;

    // Shape state
    private Node currentShape;
    private String currentShapeName = "Sphere";
    private double currentHue = 200;
    private Color currentColor = null; // null = use hue-based color
    private java.util.List<Button> shapeButtons;

    // Rotation — two persistent transforms reused across all shapes
    private final Rotate dragRotateX = new Rotate(0, new Point3D(-1, 0, 0));
    private final Rotate dragRotateY = new Rotate(0, new Point3D(0, 1, 0));
    private AnimationTimer autoRotateTimer;
    private boolean autoRotate = true;

    // Feature toggles
    private boolean wireframe = false;
    private boolean exploding  = false;

    // Info / status labels
    private Label shapeNameLabel;
    private Label shapeDescLabel;
    private Label polyCountLabel;
    private Label vertexCountLabel;
    private Label divisionsLabel;
    private Label sizeLabel;
    private Label statusLabel;

    public MainScene(Stage stage) { this.stage = stage; }

    public void show() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #0a0a0f;");
        appIcon = loadAppIcon();

        root.setTop(buildTopBar());
        root.setLeft(buildSidebar());
        root.setCenter(buildViewport());
        root.setRight(buildInfoPanel());
        root.setBottom(buildStatusBar());

        scene = new Scene(root, 1280, 800, true, SceneAntialiasing.BALANCED);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.getIcons().setAll(appIcon);
        scene.setFill(Color.web("#0a0a0f"));

        stage.setScene(scene);
        stage.setTitle("3D Object Showcase");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();

        loadShape("Sphere");
        startCameraDrift();
    }

    // ─── TOP BAR ──────────────────────────────────────────────────────────────

    private HBox buildTopBar() {
        HBox bar = new HBox();
        bar.setPadding(new Insets(0, 24, 0, 24));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPrefHeight(52);
        bar.setStyle(
            "-fx-background-color: #0d0d16;" +
            "-fx-border-color: #1e1e30;" +
            "-fx-border-width: 0 0 1 0;"
        );

        ImageView logoIcon = buildLogoIcon();

        Label title = new Label("SHOWCASE");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 15));
        title.setTextFill(Color.web("#e0e0f0"));
        title.setPadding(new Insets(0, 0, 0, 10));

        Label subtitle = new Label("· Interactive 3D Explorer");
        subtitle.setFont(Font.font("Monospace", 12));
        subtitle.setTextFill(Color.web("#4a4a6a"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = buildIconButton("✕", "#c0392b");
        Button minBtn   = buildIconButton("−", "#3a7bd5");
        closeBtn.setOnAction(e -> stage.close());
        minBtn.setOnAction(e -> stage.setIconified(true));

        bar.setOnMousePressed(e -> {
            dragOffsetX = e.getSceneX();
            dragOffsetY = e.getSceneY();
        });
        bar.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });

        bar.getChildren().addAll(logoIcon, title, subtitle, spacer, minBtn, closeBtn);
        HBox.setMargin(minBtn, new Insets(0, 4, 0, 16));
        return bar;
    }

    private ImageView buildLogoIcon() {
        ImageView icon = new ImageView(appIcon);
        icon.setFitWidth(30);
        icon.setFitHeight(30);
        icon.setPreserveRatio(true);
        icon.setSmooth(true);

        Circle clip = new Circle(15, 15, 15);
        icon.setClip(clip);

        return icon;
    }

    private Image loadAppIcon() {
        // Load from inside the jar (works both running from source and installed)
        java.io.InputStream is = getClass().getResourceAsStream("/logo.png");
        if (is != null) return new Image(is, 128, 128, true, true);

        // Fallback: draw it programmatically
        return createAppIcon();
    }

    private Image createAppIcon() {
        int size = 64;
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#3a7bd5")),
            new Stop(1, Color.web("#8e44ad"))
        ));
        gc.fillOval(0, 0, size, size);

        gc.setFill(Color.web("#ffffff", 0.12));
        gc.fillOval(8, 8, size - 16, size - 16);

        gc.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#f6f9ff", 0.95)),
            new Stop(1, Color.web("#dbe7ff", 0.85))
        ));
        gc.beginPath();
        gc.moveTo(size * 0.28, size * 0.44);
        gc.lineTo(size * 0.44, size * 0.24);
        gc.lineTo(size * 0.72, size * 0.38);
        gc.lineTo(size * 0.56, size * 0.58);
        gc.closePath();
        gc.fill();

        gc.setStroke(Color.web("#ffffff", 0.9));
        gc.setLineWidth(3);
        gc.strokeLine(size * 0.44, size * 0.24, size * 0.44, size * 0.58);
        gc.strokeLine(size * 0.44, size * 0.24, size * 0.72, size * 0.38);
        gc.strokeLine(size * 0.72, size * 0.38, size * 0.56, size * 0.58);

        WritableImage icon = new WritableImage(size, size);
        canvas.snapshot(null, icon);
        return icon;
    }

    private Button buildIconButton(String symbol, String color) {
        Button btn = new Button(symbol);
        btn.setFont(Font.font("Monospace", 12));
        btn.setTextFill(Color.web("#888"));
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
        btn.setOnMouseEntered(e -> btn.setTextFill(Color.web(color)));
        btn.setOnMouseExited(e  -> btn.setTextFill(Color.web("#888")));
        return btn;
    }

    // ─── SIDEBAR ──────────────────────────────────────────────────────────────

    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(200);
        sidebar.setStyle(
            "-fx-background-color: #0d0d16;" +
            "-fx-border-color: #1e1e30;" +
            "-fx-border-width: 0 1 0 0;"
        );

        sidebar.getChildren().add(sectionHeader("SHAPES"));
        String[] shapes = {"Sphere", "Box", "Cylinder", "Torus", "Icosahedron"};
        String[] icons  = {"◉", "⬛", "⬤", "⊙", "◈"};
        shapeButtons = new java.util.ArrayList<>();
        for (int i = 0; i < shapes.length; i++) {
            Button btn = buildNavButton(shapes[i], icons[i]);
            shapeButtons.add(btn);
            sidebar.getChildren().add(btn);
        }
        // Highlight Sphere by default since it loads first
        highlightButton(shapeButtons.get(0));

        sidebar.getChildren().add(sectionHeader("MATERIAL"));
        String[] colors     = {"Cosmic Blue", "Ember Red", "Neon Green", "Solar Gold", "Violet"};
        double[] hues       = {220, 0, 140, 45, 270};
        String[] colorCodes = {"#3a7bd5", "#e74c3c", "#2ecc71", "#f39c12", "#9b59b6"};
        for (int i = 0; i < colors.length; i++)
            sidebar.getChildren().add(buildColorButton(colors[i], colorCodes[i], hues[i]));

        // ── Custom color input ─────────────────────────────────────────────
        sidebar.getChildren().add(sectionHeader("CUSTOM COLOR"));
        sidebar.getChildren().add(buildColorInput());

        sidebar.getChildren().add(sectionHeader("ACTIONS"));
        sidebar.getChildren().add(buildToggleButton("⟳  Auto Rotate", this::toggleAutoRotate));
        sidebar.getChildren().add(buildToggleButton("⬚  Wireframe",    this::toggleWireframe));
        sidebar.getChildren().add(buildToggleButton("◎  Explode",      this::triggerExplode));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        Label footer = new Label("JavaFX 3D Engine");
        footer.setFont(Font.font("Monospace", 10));
        footer.setTextFill(Color.web("#2a2a4a"));
        footer.setPadding(new Insets(12, 16, 12, 16));
        sidebar.getChildren().add(footer);

        return sidebar;
    }

    /** Text field + button: type a color name or hex, press Enter or click Apply. */
    private HBox buildColorInput() {
        HBox row = new HBox(6);
        row.setPadding(new Insets(4, 12, 8, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        TextField field = new TextField();
        field.setPromptText("e.g. red, #ff00aa");
        field.setFont(Font.font("Monospace", 11));
        field.setPrefWidth(108);
        field.setStyle(
            "-fx-background-color: #12122a;" +
            "-fx-text-fill: #c0c0e8;" +
            "-fx-prompt-text-fill: #3a4a6a;" +
            "-fx-border-color: #2a2a4a;" +
            "-fx-border-width: 1;" +
            "-fx-padding: 5 7 5 7;"
        );

        Button apply = new Button("→");
        apply.setFont(Font.font("Monospace", FontWeight.BOLD, 12));
        apply.setTextFill(Color.web("#3a7bd5"));
        apply.setStyle(
            "-fx-background-color: #12122a;" +
            "-fx-border-color: #2a2a4a;" +
            "-fx-border-width: 1;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 5 9 5 9;"
        );
        apply.setOnMouseEntered(e -> apply.setStyle(
            "-fx-background-color: #1a1a3a;" +
            "-fx-border-color: #3a7bd5;" +
            "-fx-border-width: 1;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 5 9 5 9;"
        ));
        apply.setOnMouseExited(e -> apply.setStyle(
            "-fx-background-color: #12122a;" +
            "-fx-border-color: #2a2a4a;" +
            "-fx-border-width: 1;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 5 9 5 9;"
        ));

        Runnable applyColor = () -> {
            String raw = field.getText().trim();
            if (raw.isEmpty()) return;
            Color parsed = parseColorName(raw);
            if (parsed != null) {
                applyNamedColor(parsed, raw);
                field.setStyle(field.getStyle().replace("#c0392b", "#3a7bd5"));
            } else {
                // Flash red border on invalid input
                field.setStyle(
                    "-fx-background-color: #1a0a0a;" +
                    "-fx-text-fill: #e74c3c;" +
                    "-fx-prompt-text-fill: #3a4a6a;" +
                    "-fx-border-color: #c0392b;" +
                    "-fx-border-width: 1;" +
                    "-fx-padding: 5 7 5 7;"
                );
                statusLabel.setText("Unknown color: \"" + raw + "\" · Try: red, blue, #ff0000");
                // Reset after 1.5s
                PauseTransition reset = new PauseTransition(Duration.seconds(1.5));
                reset.setOnFinished(ev -> field.setStyle(
                    "-fx-background-color: #12122a;" +
                    "-fx-text-fill: #c0c0e8;" +
                    "-fx-prompt-text-fill: #3a4a6a;" +
                    "-fx-border-color: #2a2a4a;" +
                    "-fx-border-width: 1;" +
                    "-fx-padding: 5 7 5 7;"
                ));
                reset.play();
            }
        };

        apply.setOnAction(e -> applyColor.run());
        field.setOnAction(e -> applyColor.run());

        row.getChildren().addAll(field, apply);
        return row;
    }

    private Label sectionHeader(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Monospace", FontWeight.BOLD, 9));
        lbl.setTextFill(Color.web("#3a4a6a"));
        lbl.setPadding(new Insets(18, 16, 6, 16));
        return lbl;
    }

    private Button buildNavButton(String name, String icon) {
        Button btn = new Button(icon + "  " + name);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Monospace", 13));
        btn.setTextFill(Color.web("#8080aa"));
        btn.setPadding(new Insets(9, 16, 9, 20));
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> { if (!btn.getStyle().contains("#1a1a30")) btn.setStyle("-fx-background-color: #12122a; -fx-cursor: hand;"); });
        btn.setOnMouseExited(e  -> { if (!btn.getStyle().contains("#1a1a30")) btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;"); });
        btn.setOnAction(e -> {
            loadShape(name);
            for (Button b : shapeButtons) resetButtonStyle(b);
            highlightButton(btn);
        });
        return btn;
    }

    private void resetButtonStyle(Button btn) {
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        btn.setTextFill(Color.web("#8080aa"));
    }

    private void highlightButton(Button selected) {
        selected.setStyle("-fx-background-color: #1a1a30; -fx-border-color: #3a7bd5; -fx-border-width: 0 0 0 2; -fx-cursor: hand;");
        selected.setTextFill(Color.web("#3a7bd5"));
    }

    private Button buildColorButton(String name, String hexColor, double hue) {
        HBox content = new HBox(10);
        content.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(5);
        dot.setFill(Color.web(hexColor));
        dot.setEffect(new Glow(0.6));
        Label lbl = new Label(name);
        lbl.setFont(Font.font("Monospace", 12));
        lbl.setTextFill(Color.web("#8080aa"));
        content.getChildren().addAll(dot, lbl);
        content.setPadding(new Insets(8, 16, 8, 20));

        Button btn = new Button();
        btn.setGraphic(content);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
        btn.setOnMouseEntered(e -> { btn.setStyle("-fx-background-color: #12122a; -fx-cursor: hand; -fx-padding: 0;"); lbl.setTextFill(Color.web("#c0c0e8")); });
        btn.setOnMouseExited(e  -> { btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;"); lbl.setTextFill(Color.web("#8080aa")); });
        btn.setOnAction(e -> { currentColor = null; currentHue = hue; loadShape(currentShapeName); });
        return btn;
    }

    private Button buildToggleButton(String name, Runnable action) {
        Button btn = new Button(name);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Monospace", 12));
        btn.setTextFill(Color.web("#8080aa"));
        btn.setPadding(new Insets(9, 16, 9, 20));
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> { if (!btn.getStyle().contains("#1a1a30")) btn.setStyle("-fx-background-color: #12122a; -fx-cursor: hand;"); });
        btn.setOnMouseExited(e  -> { if (!btn.getStyle().contains("#1a1a30")) btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;"); });
        btn.setOnAction(e -> action.run());
        return btn;
    }

    // ─── VIEWPORT ─────────────────────────────────────────────────────────────

    private StackPane buildViewport() {
        world = new Group();

        camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-600);
        camera.setNearClip(0.1);
        camera.setFarClip(10000);
        camera.setFieldOfView(35);

        javafx.scene.AmbientLight ambient = new javafx.scene.AmbientLight(Color.web("#3a3a4a"));
        javafx.scene.PointLight light1 = new javafx.scene.PointLight(Color.web("#ffffff"));
        light1.setTranslateX(300); light1.setTranslateY(-300); light1.setTranslateZ(-300);
        light1.setLinearAttenuation(0.0005);
        javafx.scene.PointLight light2 = new javafx.scene.PointLight(Color.web("#9aa5c0"));
        light2.setTranslateX(-300); light2.setTranslateY(200); light2.setTranslateZ(-200);
        light2.setLinearAttenuation(0.001);

        world.getChildren().addAll(ambient, light1, light2);

        subScene = new SubScene(world, 700, 700, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.TRANSPARENT);
        subScene.setCamera(camera);

        StackPane viewport = new StackPane();
        viewport.setStyle("-fx-background-color: #0a0a0f;");
        viewport.getChildren().addAll(buildBackgroundRings(), subScene);
        StackPane.setAlignment(subScene, Pos.CENTER);

        final double[] lastMouse = {0, 0};

        subScene.setOnMousePressed(e -> {
            lastMouse[0] = e.getSceneX();
            lastMouse[1] = e.getSceneY();
            if (autoRotateTimer != null) autoRotateTimer.stop();
        });

        subScene.setOnMouseDragged(e -> {
            if (currentShape == null) return;
            double dx = e.getSceneX() - lastMouse[0];
            double dy = e.getSceneY() - lastMouse[1];
            lastMouse[0] = e.getSceneX();
            lastMouse[1] = e.getSceneY();
            dragRotateX.setAngle(dragRotateX.getAngle() - dy * 0.5);
            dragRotateY.setAngle(dragRotateY.getAngle() + dx * 0.5);
        });

        subScene.setOnMouseReleased(e -> {
            if (autoRotate && autoRotateTimer != null) autoRotateTimer.start();
        });

        subScene.setOnScroll(e -> {
            double z = camera.getTranslateZ();
            camera.setTranslateZ(z + e.getDeltaY() * 2);
        });

        return viewport;
    }

    private Pane buildBackgroundRings() {
        Pane pane = new Pane();
        pane.setMouseTransparent(true);
        for (int i = 1; i <= 4; i++) {
            Circle ring = new Circle(80 * i);
            ring.setFill(Color.TRANSPARENT);
            ring.setStroke(Color.web("#1a1a30", 0.5 - i * 0.1));
            ring.setStrokeWidth(1);
            ring.setTranslateX(340);
            ring.setTranslateY(340);
            pane.getChildren().add(ring);
        }
        Line hLine = new Line(240, 340, 440, 340);
        hLine.setStroke(Color.web("#1a1a30", 0.4));
        Line vLine = new Line(340, 240, 340, 440);
        vLine.setStroke(Color.web("#1a1a30", 0.4));
        pane.getChildren().addAll(hLine, vLine);
        return pane;
    }

    // ─── INFO PANEL ───────────────────────────────────────────────────────────

    private VBox buildInfoPanel() {
        VBox panel = new VBox(0);
        panel.setPrefWidth(240);
        panel.setStyle("-fx-background-color: #0d0d16; -fx-border-color: #1e1e30; -fx-border-width: 0 0 0 1;");

        Label header = new Label("OBJECT INFO");
        header.setFont(Font.font("Monospace", FontWeight.BOLD, 9));
        header.setTextFill(Color.web("#3a4a6a"));
        header.setPadding(new Insets(20, 16, 10, 16));
        panel.getChildren().add(header);

        VBox nameCard = new VBox(4);
        nameCard.setStyle("-fx-background-color: #12122a; -fx-padding: 14 16 14 16;");
        shapeNameLabel = new Label("Sphere");
        shapeNameLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 20));
        shapeNameLabel.setTextFill(Color.web("#3a7bd5"));
        shapeDescLabel = new Label("A perfect round surface\nwith uniform curvature\nin all directions.");
        shapeDescLabel.setFont(Font.font("Monospace", 11));
        shapeDescLabel.setTextFill(Color.web("#5a6a8a"));
        shapeDescLabel.setWrapText(true);
        nameCard.getChildren().addAll(shapeNameLabel, shapeDescLabel);
        panel.getChildren().add(nameCard);

        panel.getChildren().add(sectionHeader("GEOMETRY"));

        vertexCountLabel = new Label("—");
        panel.getChildren().add(buildStatRow("Vertices", vertexCountLabel));

        polyCountLabel = new Label("—");
        panel.getChildren().add(buildStatRow("Faces", polyCountLabel));

        divisionsLabel = new Label("64");
        panel.getChildren().add(buildStatRow("Divisions", divisionsLabel));

        sizeLabel = new Label("120 units");
        panel.getChildren().add(buildStatRow("Size", sizeLabel));

        panel.getChildren().add(sectionHeader("MATERIAL"));
        panel.getChildren().add(buildStatRow("Type", "Phong"));
        panel.getChildren().add(buildStatRow("Diffuse", "Cosmic Blue"));
        panel.getChildren().add(buildStatRow("Specular", "Enabled"));
        panel.getChildren().add(buildStatRow("Shininess", "96.0"));
        panel.getChildren().add(sectionHeader("TRANSFORM"));
        panel.getChildren().add(buildStatRow("Position", "0, 0, 0"));
        panel.getChildren().add(buildStatRow("Rotation", "Auto"));
        panel.getChildren().add(buildStatRow("Scale", "1.0×"));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        panel.getChildren().add(spacer);

        VBox hint = new VBox(4);
        hint.setStyle("-fx-background-color: #0f0f22; -fx-padding: 14 16 14 16;");
        hint.getChildren().add(hintRow("🖱 Drag",   "Orbit camera"));
        hint.getChildren().add(hintRow("⚲ Scroll", "Zoom in/out"));
        hint.getChildren().add(hintRow("Sidebar",   "Change shape"));
        panel.getChildren().add(hint);

        return panel;
    }

    private HBox buildStatRow(String key, String val) {
        HBox row = new HBox();
        row.setPadding(new Insets(6, 16, 6, 16));
        row.setAlignment(Pos.CENTER_LEFT);
        Label keyLbl = new Label(key);
        keyLbl.setFont(Font.font("Monospace", 11));
        keyLbl.setTextFill(Color.web("#4a5a7a"));
        keyLbl.setMinWidth(80);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label valLbl = new Label(val);
        valLbl.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        valLbl.setTextFill(Color.web("#8090b0"));
        row.getChildren().addAll(keyLbl, sp, valLbl);
        return row;
    }

    /** Overload that uses a pre-created Label so we can update its text later. */
    private HBox buildStatRow(String key, Label valLbl) {
        HBox row = new HBox();
        row.setPadding(new Insets(6, 16, 6, 16));
        row.setAlignment(Pos.CENTER_LEFT);
        Label keyLbl = new Label(key);
        keyLbl.setFont(Font.font("Monospace", 11));
        keyLbl.setTextFill(Color.web("#4a5a7a"));
        keyLbl.setMinWidth(80);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        valLbl.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        valLbl.setTextFill(Color.web("#8090b0"));
        row.getChildren().addAll(keyLbl, sp, valLbl);
        return row;
    }

    private HBox hintRow(String key, String val) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label k = new Label(key);
        k.setFont(Font.font("Monospace", 10));
        k.setTextFill(Color.web("#3a4a6a"));
        k.setMinWidth(70);
        Label v = new Label(val);
        v.setFont(Font.font("Monospace", 10));
        v.setTextFill(Color.web("#2a3a5a"));
        row.getChildren().addAll(k, v);
        return row;
    }

    // ─── STATUS BAR ───────────────────────────────────────────────────────────

    private HBox buildStatusBar() {
        HBox bar = new HBox(20);
        bar.setPadding(new Insets(6, 20, 6, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #08080e; -fx-border-color: #1a1a28; -fx-border-width: 1 0 0 0;");
        Circle statusDot = new Circle(4, Color.web("#2ecc71"));
        statusDot.setEffect(new Glow(0.8));
        statusLabel = new Label("Ready · JavaFX 3D Engine Active");
        statusLabel.setFont(Font.font("Monospace", 10));
        statusLabel.setTextFill(Color.web("#3a5a4a"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label fps = new Label("60 FPS  ·  OpenGL  ·  Anti-alias ON");
        fps.setFont(Font.font("Monospace", 10));
        fps.setTextFill(Color.web("#2a3a4a"));
        bar.getChildren().addAll(statusDot, statusLabel, spacer, fps);
        return bar;
    }

    // ─── SHAPE LOADING ────────────────────────────────────────────────────────

    private void loadShape(String name) {
        if (autoRotateTimer != null) autoRotateTimer.stop();
        if (currentShape != null) world.getChildren().remove(currentShape);

        currentShapeName = name;

        // Build material: named color overrides hue-based
        PhongMaterial mat;
        if (currentColor != null) {
            mat = new PhongMaterial();
            mat.setDiffuseColor(currentColor);
            mat.setSpecularColor(currentColor.brighter().desaturate());
            mat.setSpecularPower(80);
        } else {
            mat = null; // ShapeFactory will build from hue
        }

        currentShape = (mat != null)
            ? ShapeFactory.createWithMaterial(name, mat)
            : ShapeFactory.create(name, currentHue);

        dragRotateX.setAngle(dragRotateX.getAngle()); // keep current angle
        dragRotateY.setAngle(dragRotateY.getAngle());
        currentShape.getTransforms().setAll(dragRotateY, dragRotateX);

        setDrawMode(wireframe ? DrawMode.LINE : DrawMode.FILL);

        world.getChildren().add(currentShape);

        currentShape.setScaleX(0.01); currentShape.setScaleY(0.01); currentShape.setScaleZ(0.01);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(400), currentShape);
        scaleIn.setToX(1); scaleIn.setToY(1); scaleIn.setToZ(1);
        scaleIn.setInterpolator(Interpolator.EASE_OUT);
        scaleIn.play();

        if (autoRotate) startAutoRotate();

        shapeNameLabel.setText(name);
        shapeDescLabel.setText(ShapeFactory.getDescription(name));

        String[] stats = ShapeFactory.getStats(name);
        vertexCountLabel.setText(stats[0]);
        polyCountLabel.setText(stats[1]);
        divisionsLabel.setText(stats[2]);
        sizeLabel.setText(stats[3]);

        statusLabel.setText("Loaded: " + name + " · Drag to orbit · Scroll to zoom");
    }

    // ─── AUTO ROTATE ──────────────────────────────────────────────────────────

    private void startAutoRotate() {
        if (autoRotateTimer != null) autoRotateTimer.stop();
        autoRotateTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                dragRotateY.setAngle(dragRotateY.getAngle() + 0.30);
                dragRotateX.setAngle(dragRotateX.getAngle() + 0.12);
            }
        };
        autoRotateTimer.start();
    }

    private void toggleAutoRotate() {
        autoRotate = !autoRotate;
        if (autoRotate) { startAutoRotate(); statusLabel.setText("Auto-rotate: ON"); }
        else { if (autoRotateTimer != null) autoRotateTimer.stop(); statusLabel.setText("Auto-rotate: OFF"); }
    }

    // ─── WIREFRAME ────────────────────────────────────────────────────────────

    private void toggleWireframe() {
        wireframe = !wireframe;
        setDrawMode(wireframe ? DrawMode.LINE : DrawMode.FILL);
        statusLabel.setText("Wireframe: " + (wireframe ? "ON" : "OFF"));
    }

    private void setDrawMode(DrawMode mode) {
        if (currentShape instanceof Shape3D s) { s.setDrawMode(mode); }
        else if (currentShape instanceof Group g) {
            for (Node child : g.getChildren())
                if (child instanceof Shape3D s) s.setDrawMode(mode);
        }
    }

    // ─── EXPLODE ──────────────────────────────────────────────────────────────

    private void triggerExplode() {
        if (exploding || currentShape == null) return;
        exploding = true;
        if (autoRotateTimer != null) autoRotateTimer.stop();
        statusLabel.setText("💥 Exploding! Reassembling in 3s…");

        ExplodeEffect.run(world, currentShape, () -> {
            exploding = false;
            if (autoRotate) startAutoRotate();
            statusLabel.setText("Reassembled · " + currentShapeName);
        });
    }

    // ─── NAMED COLOR INPUT ────────────────────────────────────────────────────

    private void applyNamedColor(Color color, String name) {
        currentColor = color;
        // Derive hue for sidebar buttons to not get confused
        currentHue = color.getHue();
        loadShape(currentShapeName);
        statusLabel.setText("Color → " + name);
    }

    /**
     * Parses CSS color names (all named colors JavaFX supports) plus hex strings.
     * Returns null if unrecognised.
     */
    private Color parseColorName(String input) {
        String s = input.trim().toLowerCase();
        try {
            return Color.web(s);          // handles #rrggbb, #rgb, named colors
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ─── CAMERA DRIFT ─────────────────────────────────────────────────────────

    private void startCameraDrift() {
        Timeline drift = new Timeline(
            new KeyFrame(Duration.ZERO,        new KeyValue(camera.translateYProperty(), 0)),
            new KeyFrame(Duration.seconds(4),  new KeyValue(camera.translateYProperty(), -15, Interpolator.EASE_BOTH)),
            new KeyFrame(Duration.seconds(8),  new KeyValue(camera.translateYProperty(), 0,   Interpolator.EASE_BOTH))
        );
        drift.setCycleCount(Animation.INDEFINITE);
        drift.play();
    }
}