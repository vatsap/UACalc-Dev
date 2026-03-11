package org.uacalc.io.lua;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class TikzExporter {
    private final String verticesLabels;
    private final String verticesString;
    private final String edgesString;
    private final String alg_name;
    private final boolean labelInsideVertex;
    

    public TikzExporter(List<String> verticesLabels, List<double[]> vertices, List<double[][]> edges, String alg_name, boolean labelInsideVertex) {
        this.verticesLabels = buildVerticesLabelsString(verticesLabels);
        this.verticesString = buildVertexString(vertices);
        this.edgesString = buildEdgeString(edges);
        this.labelInsideVertex = labelInsideVertex;
        this.alg_name = alg_name;
    }

    public static boolean pointInBox(double[] p, double[] box) {
        double x = p[0], y = p[1];
        return x >= box[0] && x <= box[2] && y >= box[1] && y <= box[3];
    }

    private static boolean ccw(double[] a, double[] b, double[] c) {
        return (c[1] - a[1]) * (b[0] - a[0]) > (b[1] - a[1]) * (c[0] - a[0]);
    }

    public static boolean segmentsIntersect(double[] p1, double[] p2, double[] q1, double[] q2) {
        return ccw(p1, q1, q2) != ccw(p2, q1, q2) && ccw(p1, p2, q1) != ccw(p1, p2, q2);
    }


    public static boolean edgeIntersectsBox(double[] p1, double[] p2, double[] box) {
        double xMin = box[0], yMin = box[1], xMax = box[2], yMax = box[3];

        // Trivial rejection: both points on same outside side of box
        if (p1[0] < xMin && p2[0] < xMin) return false;
        if (p1[0] > xMax && p2[0] > xMax) return false;
        if (p1[1] < yMin && p2[1] < yMin) return false;
        if (p1[1] > yMax && p2[1] > yMax) return false;

        // Trivial acceptance: either endpoint is inside box
        if (pointInBox(p1, box) || pointInBox(p2, box)) return true;

        // Check intersection with each of the 4 box edges
        double[][] boxEdges = {
            {xMin, yMin, xMax, yMin}, // bottom
            {xMax, yMin, xMax, yMax}, // right
            {xMax, yMax, xMin, yMax}, // top
            {xMin, yMax, xMin, yMin}  // left
        };

        for (double[] edge : boxEdges) {
            double[] q1 = {edge[0], edge[1]};
            double[] q2 = {edge[2], edge[3]};
            if (segmentsIntersect(p1, p2, q1, q2)) return true;
        }

        return false;
    }


    public List<String> computeLabelPositions(List<double[]> vertices, List<double[][]> edges) {
        List<String> positions = new ArrayList<>();
        double margin = 0.2;  // How far from the node to place the label
        double labelBoxSize = 0.5;  // Width/height estimate of label box

        for (double[] v : vertices) {
            String[] candidates = {"above", "below", "left", "right"};
            int bestScore = Integer.MAX_VALUE;
            String bestPosition = "below";  // fallback default

            for (String candidate : candidates) {
                double dx = 0, dy = 0;
                switch (candidate) {
                    case "above": dy = +margin; break;
                    case "below": dy = -margin; break;
                    case "left": dx = -margin; break;
                    case "right": dx = +margin; break;
                }

                double labelX = v[0] + dx;
                double labelY = v[1] + dy;
                double[] labelBox = {
                    labelX - labelBoxSize/2, labelY - labelBoxSize/2,
                    labelX + labelBoxSize/2, labelY + labelBoxSize/2
                };

                int overlapCount = 0;
                for (double[][] edge : edges) {
                    double[] p1 = edge[0];
                    double[] p2 = edge[1];
                    if (edgeIntersectsBox(p1, p2, labelBox)) {
                        overlapCount++;
                    }
                }

                if (overlapCount < bestScore) {
                    bestScore = overlapCount;
                    bestPosition = candidate;
                }
            }

            positions.add(bestPosition);
        }

        return positions;
    }


    public String buildVerticesLabelsString(List<String> verticesLabels) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < verticesLabels.size(); i++) {
            sb.append(verticesLabels.get(i));
            if (i < verticesLabels.size() - 1) sb.append(" & ");
        }
        sb.append("}");
        return sb.toString();
    }

    public String buildVertexString(List<double[]> vertices) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < vertices.size(); i++) {
            double[] v = vertices.get(i);
            sb.append(String.format("{%.3f, %.3f}", v[0], v[1]));
            if (i < vertices.size() - 1) sb.append(", ");
        }
        sb.append("}");
        return sb.toString();
    }

    public String buildEdgeString(List<double[][]> edges) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < edges.size(); i++) {
            double[][] e = edges.get(i);
            double[] p1 = e[0], p2 = e[1];
            sb.append(String.format("{{%.3f, %.3f}, {%.3f, %.3f}}", p1[0], p1[1], p2[0], p2[1]));
            if (i < edges.size() - 1) sb.append(", ");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Runs the Lua exporter.lua script, sending the input string to its stdin,
     * and returns the full output from Lua as a String.
     */
    public String runLua() throws IOException, InterruptedException {

        InputStream in = TikzExporter.class.getResourceAsStream("/org/uacalc/io/lua/TikzExporter.lua");
        File tmp = File.createTempFile("TikzExporter", ".lua");
        tmp.deleteOnExit();
        Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        ProcessBuilder pb = new ProcessBuilder("lua", tmp.getAbsolutePath());

        Process process = pb.start();
        StringBuilder input = new StringBuilder();
        if (labelInsideVertex == true){
            input.append("Options:labelinside");
            input.append("\n");
        } else {
            input.append("Options:noinside");
            input.append("\n");
        }
        input.append(alg_name);
        input.append("\n");
        input.append(verticesLabels);
        input.append("\n");
        input.append(verticesString);
        input.append("\n");
        input.append(edgesString);

        // Write some input to Lua process stdin
        try (BufferedWriter luaInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
            System.out.println(input.toString());
            luaInput.write(input.toString());
            luaInput.flush();
            luaInput.close();  // signal EOF
        }
        // Read output from Lua
        StringBuilder outputBuilder = new StringBuilder();
        try (BufferedReader luaOutput = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = luaOutput.readLine()) != null) {
                outputBuilder.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Lua process exited with non-zero code: " + exitCode);
        }
        System.out.println("Lua exited with code " + exitCode);
        return outputBuilder.toString();
    }
}
