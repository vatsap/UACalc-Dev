package org.uacalc.io.lua;

import org.latdraw.diagram.*;
import org.latdraw.orderedset.*;
import org.latdraw.util.*;
import java.awt.geom.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class DiagramData {
    public final List<double[]> vertices = new ArrayList<>();
    public final List<double[][]> edges = new ArrayList<>();
    public final List<String> verticesLabels = new ArrayList<>();  // new property

    public void collectVertexAndEdgeData(Diagram diagram) {
        final Vertex[] vertexArray = diagram.getVertices();
        final OrderedSet poset = diagram.getOrderedSet();

        // Collect vertices and their names
        for (Vertex v : vertexArray) {
            double x = v.getProjectedX();
            double y = v.getProjectedY();
            vertices.add(new double[]{x, y});

            // Assuming Vertex has a getName() or similar method:
            verticesLabels.add(v.getLabel());  // keep names in the same order
        }

        // Collect edges (source -> target as coordinate pairs)
        for (Vertex v : vertexArray) {
            double x1 = v.getProjectedX();
            double y1 = v.getProjectedY();

            for (Iterator<?> covs = v.getUnderlyingElem().upperCovers().iterator(); covs.hasNext(); ) {
                POElem coverElem = (POElem) covs.next();
                Vertex target = vertexArray[poset.elemOrder(coverElem)];
                double x2 = target.getProjectedX();
                double y2 = target.getProjectedY();

                edges.add(new double[][]{
                    {x1, y1},
                    {x2, y2}
                });
            }
        }
    }
    public void upSideDown(){
        // Invert y in vertices
        for (double[] vertex : vertices) {
            if (vertex.length > 1) {
                if (vertex[1] != 0.0) {
                    vertex[1] = -vertex[1];
                }
            }
        }
        
        // Invert y in edges
        for (double[][] edge : edges) {
            for (double[] vertex : edge) {
                if (vertex.length > 1) {
                    if (vertex[1] != 0.0) {
                        vertex[1] = -vertex[1];
                    }
                }
            }
        }
    }

    public void roundAndScaleData(double scaleX, double scaleY) {
        double grid = 5.0;
        double scale = 1.0;
        if (vertices.size() < 11) {
            grid = 5.0;
            scale = 1.0;
        } else if (vertices.size() < 33) {
            grid = 10.0;
            scale = 1.5;
        } else if (vertices.size() < 77) {
            grid = 50.0;
            scale = 1.8;
        } else {
            grid = 100.0;
            scale = 2.2;
        }
        for (double[] coord : vertices) {
            coord[0] = scale*Math.round(coord[0] * scaleX *grid)/grid;  // X coordinate
            coord[1] = scale*Math.round(coord[1] * scaleY *grid)/grid;  // Y coordinate
        }

        for (double[][] edge : edges) {
            for (int i = 0; i < edge.length; i++) {        // source or target vertex
                edge[i][0] = scale*Math.round(edge[i][0] * scaleX*grid)/grid;  // X coordinate
                edge[i][1] = scale*Math.round(edge[i][1] * scaleY*grid)/grid;  // Y coordinate
            }
        }
    }
}
