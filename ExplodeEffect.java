import javafx.animation.*;
import javafx.scene.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.util.Duration;
import java.util.*;

/**
 * Breaks a shape into randomised smaller pieces, blasts them outward,
 * then reassembles them after a delay.
 *
 * The "pieces" are small spheres / boxes placed at random positions on
 * the surface of the original bounding sphere (radius ~120).  They fly
 * outward, spin, fade, then animate back.  The original shape is hidden
 * during the explosion.
 */
public class ExplodeEffect {

    private static final int PIECE_COUNT = 28;
    private static final double SURFACE_R = 120;

    /** Runs the full explode → wait → reassemble cycle. */
    public static void run(Group world, Node originalShape, Runnable onDone) {
        // Hide original
        originalShape.setVisible(false);

        List<MeshView> pieces = buildPieces(originalShape);
        Group debrisGroup = new Group(pieces.toArray(new Node[0]));
        // Copy the shape's own transforms so debris appears in same orientation
        debrisGroup.getTransforms().setAll(originalShape.getTransforms());
        world.getChildren().add(debrisGroup);

        // ── Phase 1 : explode outward (600 ms) ──────────────────────────────
        List<Timeline> outTimelines = new ArrayList<>();
        Random rng = new Random(42);

        for (MeshView piece : pieces) {
            // Random direction vector
            double azimuth = rng.nextDouble() * 2 * Math.PI;
            double polar   = rng.nextDouble() * Math.PI;
            double dist    = 180 + rng.nextDouble() * 220;
            double tx = dist * Math.sin(polar) * Math.cos(azimuth);
            double ty = dist * Math.sin(polar) * Math.sin(azimuth);
            double tz = dist * Math.cos(polar);
            double rotAngle = 120 + rng.nextDouble() * 360;

            Timeline out = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(piece.translateXProperty(), piece.getTranslateX()),
                    new KeyValue(piece.translateYProperty(), piece.getTranslateY()),
                    new KeyValue(piece.translateZProperty(), piece.getTranslateZ()),
                    new KeyValue(piece.opacityProperty(), 1.0),
                    new KeyValue(piece.rotateProperty(), 0)
                ),
                new KeyFrame(Duration.millis(650),
                    new KeyValue(piece.translateXProperty(), piece.getTranslateX() + tx, Interpolator.EASE_IN),
                    new KeyValue(piece.translateYProperty(), piece.getTranslateY() + ty, Interpolator.EASE_IN),
                    new KeyValue(piece.translateZProperty(), piece.getTranslateZ() + tz, Interpolator.EASE_IN),
                    new KeyValue(piece.opacityProperty(), 0.85, Interpolator.LINEAR),
                    new KeyValue(piece.rotateProperty(), rotAngle, Interpolator.LINEAR)
                )
            );
            outTimelines.add(out);
        }

        ParallelTransition explodePhase = new ParallelTransition();
        explodePhase.getChildren().addAll(outTimelines);

        // ── Phase 2 : hold (800 ms pause) ───────────────────────────────────
        PauseTransition hold = new PauseTransition(Duration.millis(800));

        // ── Phase 3 : reassemble (600 ms) ────────────────────────────────────
        List<Timeline> inTimelines = new ArrayList<>();
        for (MeshView piece : pieces) {
            double ox = piece.getTranslateX();
            double oy = piece.getTranslateY();
            double oz = piece.getTranslateZ();

            Timeline in = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(piece.opacityProperty(), 0.85)
                ),
                new KeyFrame(Duration.millis(600),
                    new KeyValue(piece.translateXProperty(), ox, Interpolator.EASE_OUT),
                    new KeyValue(piece.translateYProperty(), oy, Interpolator.EASE_OUT),
                    new KeyValue(piece.translateZProperty(), oz, Interpolator.EASE_OUT),
                    new KeyValue(piece.opacityProperty(), 1.0, Interpolator.LINEAR),
                    new KeyValue(piece.rotateProperty(), piece.getRotate() + 180, Interpolator.LINEAR)
                )
            );
            inTimelines.add(in);
        }
        ParallelTransition reassemblePhase = new ParallelTransition();
        reassemblePhase.getChildren().addAll(inTimelines);

        // ── Chain ────────────────────────────────────────────────────────────
        SequentialTransition sequence = new SequentialTransition(explodePhase, hold, reassemblePhase);
        sequence.setOnFinished(e -> {
            world.getChildren().remove(debrisGroup);
            originalShape.setVisible(true);
            onDone.run();
        });
        sequence.play();
    }

    /** Creates PIECE_COUNT small box/sphere shards scattered on the surface. */
    private static List<MeshView> buildPieces(Node originalShape) {
        // Grab the material from the original shape for matching color
        PhongMaterial sourceMat = getMaterial(originalShape);

        List<MeshView> pieces = new ArrayList<>();
        Random rng = new Random(42);

        for (int i = 0; i < PIECE_COUNT; i++) {
            // Random point on sphere surface
            double azimuth = rng.nextDouble() * 2 * Math.PI;
            double polar   = Math.acos(1 - 2 * rng.nextDouble());
            double x = SURFACE_R * Math.sin(polar) * Math.cos(azimuth);
            double y = SURFACE_R * Math.sin(polar) * Math.sin(azimuth);
            double z = SURFACE_R * Math.cos(polar);

            // Alternate between tiny box shards and sphere shards
            float size = 10 + rng.nextFloat() * 22;
            TriangleMesh mesh = (i % 3 == 0) ? makeShardBox(size) : makeShardTetra(size);
            MeshView mv = new MeshView(mesh);

            // Slight color variation per piece
            double hShift  = rng.nextDouble() * 30 - 15;
            Color base = sourceMat != null ? (Color) sourceMat.getDiffuseColor() : Color.CORNFLOWERBLUE;
            double hue = base.getHue();
            double sat = base.getSaturation();
            double bri = base.getBrightness();
            PhongMaterial mat = new PhongMaterial();
            mat.setDiffuseColor(Color.hsb((hue + hShift + 360) % 360, Math.min(1, sat + 0.1), Math.min(1, bri + 0.05)));
            mat.setSpecularColor(Color.WHITE);
            mat.setSpecularPower(64);
            mv.setMaterial(mat);
            mv.setDrawMode(DrawMode.FILL);
            mv.setCullFace(CullFace.BACK);

            mv.setTranslateX(x);
            mv.setTranslateY(y);
            mv.setTranslateZ(z);
            mv.setRotationAxis(javafx.geometry.Point3D.ZERO.add(
                rng.nextDouble(), rng.nextDouble(), rng.nextDouble()).normalize());
            mv.setRotate(rng.nextDouble() * 360);

            pieces.add(mv);
        }
        return pieces;
    }

    private static PhongMaterial getMaterial(Node node) {
        if (node instanceof Shape3D s && s.getMaterial() instanceof PhongMaterial m) return m;
        if (node instanceof javafx.scene.Group g) {
            for (Node child : g.getChildren()) {
                PhongMaterial m = getMaterial(child);
                if (m != null) return m;
            }
        }
        return null;
    }

    /** Tiny box shard as a TriangleMesh. */
    private static TriangleMesh makeShardBox(float s) {
        float h = s / 2;
        float[] pts = {
            -h,-h,-h,  h,-h,-h,  h, h,-h, -h, h,-h,
            -h,-h, h,  h,-h, h,  h, h, h, -h, h, h
        };
        float[] tex = {0,0, 1,0, 1,1, 0,1};
        int[] faces = {
            0,0,2,2,1,1, 0,0,3,3,2,2,
            4,0,5,1,6,2, 4,0,6,2,7,3,
            0,0,1,1,5,2, 0,0,5,2,4,3,
            2,0,3,3,7,2, 2,0,7,2,6,1,
            0,0,4,3,7,2, 0,0,7,2,3,1,
            1,0,2,1,6,2, 1,0,6,2,5,3
        };
        TriangleMesh m = new TriangleMesh();
        m.getPoints().addAll(pts);
        m.getTexCoords().addAll(tex);
        m.getFaces().addAll(faces);
        return m;
    }

    /** Tiny tetrahedron shard. */
    private static TriangleMesh makeShardTetra(float s) {
        float[] pts = {
             s, 0,-s*0.7f,
            -s, 0,-s*0.7f,
             0, s, s*0.7f,
             0,-s, s*0.7f
        };
        float[] tex = {0.5f,0, 0,1, 1,1};
        int[] faces = {
            0,0,1,1,2,2,
            0,0,2,2,3,1,
            0,0,3,1,1,2,
            1,0,3,2,2,1
        };
        TriangleMesh m = new TriangleMesh();
        m.getPoints().addAll(pts);
        m.getTexCoords().addAll(tex);
        m.getFaces().addAll(faces);
        return m;
    }
}