import javafx.scene.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;

public class ShapeFactory {

    public static Node create(String name, double hue) {
        Color base     = Color.hsb(hue, 0.85, 0.9);
        Color specular = Color.hsb(hue, 0.3, 1.0);
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseColor(base);
        mat.setSpecularColor(specular);
        mat.setSpecularPower(96);
        return createWithMaterial(name, mat);
    }

    /** Create a shape using a pre-built material (used for named color input). */
    public static Node createWithMaterial(String name, PhongMaterial mat) {
        return switch (name) {
            case "Sphere"      -> makeSphere(mat);
            case "Box"         -> makeBox(mat);
            case "Cylinder"    -> makeCylinder(mat);
            case "Torus"       -> makeTorus(mat);
            case "Icosahedron" -> makeIcosahedron(mat);
            default            -> makeSphere(mat);
        };
    }

    // ── SPHERE ────────────────────────────────────────────────────────────────

    private static Node makeSphere(PhongMaterial mat) {
        Sphere s = new Sphere(120, 64);
        s.setMaterial(mat);
        return s;
    }

    // ── BOX ───────────────────────────────────────────────────────────────────

    private static Node makeBox(PhongMaterial mat) {
        Box b = new Box(180, 180, 180);
        b.setMaterial(mat);
        return b;
    }

    // ── CYLINDER ──────────────────────────────────────────────────────────────

    private static Node makeCylinder(PhongMaterial mat) {
        Cylinder c = new Cylinder(80, 220, 64);
        c.setMaterial(mat);
        return c;
    }

    // ── TORUS ─────────────────────────────────────────────────────────────────

    private static Node makeTorus(PhongMaterial mat) {
        int majorSeg = 60;
        int minorSeg = 30;
        double R = 110;
        double r = 40;

        int vertCount = majorSeg * minorSeg;
        float[] points    = new float[vertCount * 3];
        float[] texCoords = new float[vertCount * 2];

        for (int i = 0; i < majorSeg; i++) {
            double theta = 2 * Math.PI * i / majorSeg;
            for (int j = 0; j < minorSeg; j++) {
                double phi = 2 * Math.PI * j / minorSeg;
                int idx = i * minorSeg + j;
                double x = (R + r * Math.cos(phi)) * Math.cos(theta);
                double y = (R + r * Math.cos(phi)) * Math.sin(theta);
                double z = r * Math.sin(phi);
                points[idx * 3]     = (float) x;
                points[idx * 3 + 1] = (float) y;
                points[idx * 3 + 2] = (float) z;
                texCoords[idx * 2]     = (float) i / majorSeg;
                texCoords[idx * 2 + 1] = (float) j / minorSeg;
            }
        }

        int[] faces = new int[majorSeg * minorSeg * 12];
        int fi = 0;
        for (int i = 0; i < majorSeg; i++) {
            for (int j = 0; j < minorSeg; j++) {
                int a = i * minorSeg + j;
                int b = ((i + 1) % majorSeg) * minorSeg + j;
                int c = ((i + 1) % majorSeg) * minorSeg + (j + 1) % minorSeg;
                int d = i * minorSeg + (j + 1) % minorSeg;
                faces[fi++]=a; faces[fi++]=a; faces[fi++]=b; faces[fi++]=b; faces[fi++]=c; faces[fi++]=c;
                faces[fi++]=a; faces[fi++]=a; faces[fi++]=c; faces[fi++]=c; faces[fi++]=d; faces[fi++]=d;
            }
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(points);
        mesh.getTexCoords().addAll(texCoords);
        mesh.getFaces().addAll(faces);

        MeshView mv = new MeshView(mesh);
        mv.setMaterial(mat);
        mv.setDrawMode(DrawMode.FILL);
        mv.setCullFace(CullFace.BACK);
        return mv;
    }

    // ── ICOSAHEDRON ───────────────────────────────────────────────────────────

    private static Node makeIcosahedron(PhongMaterial mat) {
        double phi   = (1.0 + Math.sqrt(5.0)) / 2.0;
        double scale = 120;
        double[][] verts = {
            {-1, phi,0},{1, phi,0},{-1,-phi,0},{1,-phi,0},
            {0,-1, phi},{0, 1, phi},{0,-1,-phi},{0,1,-phi},
            {phi,0,-1},{phi,0,1},{-phi,0,-1},{-phi,0,1}
        };
        int[][] tris = {
            {0,11,5},{0,5,1},{0,1,7},{0,7,10},{0,10,11},
            {1,5,9},{5,11,4},{11,10,2},{10,7,6},{7,1,8},
            {3,9,4},{3,4,2},{3,2,6},{3,6,8},{3,8,9},
            {4,9,5},{2,4,11},{6,2,10},{8,6,7},{9,8,1}
        };

        float[] points = new float[verts.length * 3];
        for (int i = 0; i < verts.length; i++) {
            double[] v = normalize(verts[i], scale);
            points[i*3] = (float)v[0]; points[i*3+1] = (float)v[1]; points[i*3+2] = (float)v[2];
        }
        float[] texCoords = new float[verts.length * 2];
        for (int i = 0; i < verts.length; i++) { texCoords[i*2] = 0.5f; texCoords[i*2+1] = 0.5f; }

        int[] faces = new int[tris.length * 6];
        for (int i = 0; i < tris.length; i++) {
            faces[i*6]=tris[i][0]; faces[i*6+1]=tris[i][0];
            faces[i*6+2]=tris[i][1]; faces[i*6+3]=tris[i][1];
            faces[i*6+4]=tris[i][2]; faces[i*6+5]=tris[i][2];
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(points);
        mesh.getTexCoords().addAll(texCoords);
        mesh.getFaces().addAll(faces);

        MeshView mv = new MeshView(mesh);
        mv.setMaterial(mat);
        mv.setDrawMode(DrawMode.FILL);
        mv.setCullFace(CullFace.BACK);
        return mv;
    }

    private static double[] normalize(double[] v, double scale) {
        double len = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
        return new double[]{v[0]/len*scale, v[1]/len*scale, v[2]/len*scale};
    }

    // ── DESCRIPTIONS ──────────────────────────────────────────────────────────

    public static String getDescription(String name) {
        return switch (name) {
            case "Sphere"      -> "A perfect round surface\nwith uniform curvature\nin all directions.";
            case "Box"         -> "Six square faces meeting\nat right angles. The most\nfundamental 3D primitive.";
            case "Cylinder"    -> "Two parallel circular caps\njoined by a curved surface.\nHeight: 220 units.";
            case "Torus"       -> "A donut-shaped surface\ngenerated by revolving a\ncircle around an axis.";
            case "Icosahedron" -> "A Platonic solid with\n20 equilateral triangular\nfaces and 12 vertices.";
            default            -> "A 3D geometric shape.";
        };
    }

    // ── STATS ──────────────────────────────────────────────────────────────────

    /** Returns {vertices, faces, divisions, size} for the info panel. */
    public static String[] getStats(String name) {
        return switch (name) {
            case "Sphere"      -> new String[]{"~2,050", "~4,096", "64", "Radius: 120 units"};
            case "Box"         -> new String[]{"8", "12", "—", "180 × 180 × 180"};
            case "Cylinder"    -> new String[]{"130", "~252", "64", "r=80, h=220"};
            case "Torus"       -> new String[]{"1,800", "3,600", "60 × 30", "R=110, r=40"};
            case "Icosahedron" -> new String[]{"12", "20", "—", "Radius: 120 units"};
            default            -> new String[]{"—", "—", "—", "—"};
        };
    }
}