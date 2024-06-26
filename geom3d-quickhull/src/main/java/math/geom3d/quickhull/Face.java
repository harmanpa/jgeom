/*
  * Copyright John E. Lloyd, 2003. All rights reserved. Permission
  * to use, copy, and modify, without fee, is granted for non-commercial 
  * and research purposes, provided that this copyright notice appears 
  * in all copies.
  *
  * This  software is distributed "as is", without any warranty, including 
  * any implied warranty of merchantability or fitness for a particular
  * use. The authors assume no responsibility for, and shall not be liable
  * for, any special, indirect, or consequential damages, or any damages
  * whatsoever, arising out of or in connection with the use of this
  * software.
 */
package math.geom3d.quickhull;

import math.geom3d.Point3D;
import math.geom3d.Vector3D;

/**
 * Basic triangular face used to form the hull.
 *
 * The information stored for each face consists of a planar normal, a planar
 * offset, and a doubly-linked list of three <a
 * href=HalfEdge>HalfEdges which surround the face in a counter-clockwise
 * direction.
 *
 * @author John E. Lloyd, Fall 2004
 */
class Face {

    /**
     * The he0.
     */
    HalfEdge he0;

    /**
     * The normal.
     */
    private Vector3D normal;

    /**
     * The area.
     */
    double area;

    /**
     * The centroid.
     */
    private Point3D centroid;

    /**
     * The plane offset.
     */
    double planeOffset;

    /**
     * The index.
     */
    int index;

    /**
     * The num verts.
     */
    int numVerts;

    /**
     * The next.
     */
    Face next;

    /**
     * The Constant VISIBLE.
     */
    static final int VISIBLE = 1;

    /**
     * The Constant NON_CONVEX.
     */
    static final int NON_CONVEX = 2;

    /**
     * The Constant DELETED.
     */
    static final int DELETED = 3;

    /**
     * The mark.
     */
    int mark = VISIBLE;

    /**
     * The outside.
     */
    Vertex outside;

    /**
     * Compute centroid.
     *
     * @param centroid the centroid
     */
    public void computeCentroid() {
        centroid = new Point3D();
        HalfEdge he = he0;
        do {
            centroid = centroid.plus(he.head().pnt);
            he = he.next;
        } while (he != he0);
        centroid = centroid.times(1 / (double) numVerts);
    }

    /**
     * Compute normal.
     *
     * @param normal the normal
     * @param minArea the min area
     */
    public void computeNormal(double minArea) {
        computeNormal();

        if (area < minArea) {
            // make the normal more robust by removing
            // components parallel to the longest edge

            HalfEdge hedgeMax = null;
            double lenSqrMax = 0;
            HalfEdge hedge = he0;
            do {
                double lenSqr = hedge.lengthSquared();
                if (lenSqr > lenSqrMax) {
                    hedgeMax = hedge;
                    lenSqrMax = lenSqr;
                }
                hedge = hedge.next;
            } while (hedge != he0);

            Point3D p2 = hedgeMax.head().pnt;
            Point3D p1 = hedgeMax.tail().pnt;
            double lenMax = Math.sqrt(lenSqrMax);
            double ux = (p2.getX() - p1.getX()) / lenMax;
            double uy = (p2.getY() - p1.getY()) / lenMax;
            double uz = (p2.getZ() - p1.getZ()) / lenMax;
            double dot = normal.getX() * ux + normal.getY() * uy + normal.getZ() * uz;
            normal = normal.minus(new Vector3D(ux, uy, uz).times(dot)).normalize();
        }
    }

    /**
     * Compute normal.
     *
     * @param normal the normal
     */
    public void computeNormal() {
        HalfEdge he1 = he0.next;
        HalfEdge he2 = he1.next;

        Point3D p0 = he0.head().pnt;
        Point3D p2 = he1.head().pnt;

        double d2x = p2.getX() - p0.getX();
        double d2y = p2.getY() - p0.getY();
        double d2z = p2.getZ() - p0.getZ();

        normal = new Vector3D();

        numVerts = 2;

        while (he2 != he0) {
            double d1x = d2x;
            double d1y = d2y;
            double d1z = d2z;

            p2 = he2.head().pnt;
            d2x = p2.getX() - p0.getX();
            d2y = p2.getY() - p0.getY();
            d2z = p2.getZ() - p0.getZ();

            normal = normal.plus(new Vector3D(d1y * d2z - d1z * d2y, d1z * d2x - d1x * d2z, d1x * d2y - d1y * d2x));

            he1 = he2;
            he2 = he2.next;
            numVerts++;
        }
        area = normal.norm();
        normal = normal.times(1 / area);
    }

    /**
     * Compute normal and centroid.
     */
    private void computeNormalAndCentroid() throws QuickHullException {
        computeNormal();
        computeCentroid();
        planeOffset = normal.dot(centroid.asVector());
        int numv = 0;
        HalfEdge he = he0;
        do {
            numv++;
            he = he.next;
        } while (he != he0);
        if (numv != numVerts) {
            throw new QuickHullException(
                    "face " + getVertexString() + " numVerts=" + numVerts + " should be " + numv);
        }
    }

    /**
     * Compute normal and centroid.
     *
     * @param minArea the min area
     */
    private void computeNormalAndCentroid(double minArea) {
        computeNormal(minArea);
        computeCentroid();
        planeOffset = normal.dot(centroid.asVector());
    }

    /**
     * Creates the triangle.
     *
     * @param v0 the v0
     * @param v1 the v1
     * @param v2 the v2
     * @return the face
     */
    public static Face createTriangle(Vertex v0, Vertex v1, Vertex v2) {
        return createTriangle(v0, v1, v2, 0);
    }

    /**
     * Constructs a triangule Face from vertices v0, v1, and v2.
     *
     * @param v0 first vertex
     * @param v1 second vertex
     * @param v2 third vertex
     * @param minArea the min area
     * @return the face
     */
    public static Face createTriangle(Vertex v0, Vertex v1, Vertex v2,
            double minArea) {
        Face face = new Face();
        HalfEdge he0 = new HalfEdge(v0, face);
        HalfEdge he1 = new HalfEdge(v1, face);
        HalfEdge he2 = new HalfEdge(v2, face);

        he0.prev = he2;
        he0.next = he1;
        he1.prev = he0;
        he1.next = he2;
        he2.prev = he1;
        he2.next = he0;

        face.he0 = he0;

        // compute the normal and offset
        face.computeNormalAndCentroid(minArea);
        return face;
    }

    /**
     * Creates the.
     *
     * @param vtxArray the vtx array
     * @param indices the indices
     * @return the face
     */
    public static Face create(Vertex[] vtxArray, int[] indices) throws QuickHullException {
        Face face = new Face();
        HalfEdge hePrev = null;
        for (int i = 0; i < indices.length; i++) {
            HalfEdge he = new HalfEdge(vtxArray[indices[i]], face);
            if (hePrev != null) {
                he.setPrev(hePrev);
                hePrev.setNext(he);
            } else {
                face.he0 = he;
            }
            hePrev = he;
        }
        face.he0.setPrev(hePrev);
        hePrev.setNext(face.he0);

        // compute the normal and offset
        face.computeNormalAndCentroid();
        return face;
    }

    /**
     * Instantiates a new face.
     */
    public Face() {
        normal = new Vector3D();
        centroid = new Point3D();
        mark = VISIBLE;
    }

    /**
     * Gets the i-th half-edge associated with the face.
     *
     * @param i the half-edge index, in the range 0-2.
     * @return the half-edge
     */
    public HalfEdge getEdge(int i) {
        HalfEdge he = he0;
        while (i > 0) {
            he = he.next;
            i--;
        }
        while (i < 0) {
            he = he.prev;
            i++;
        }
        return he;
    }

    /**
     * Gets the first edge.
     *
     * @return the first edge
     */
    public HalfEdge getFirstEdge() {
        return he0;
    }

    /**
     * Finds the half-edge within this face which has tail <code>vt</code> and
     * head <code>vh</code>.
     *
     * @param vt tail point
     * @param vh head point
     * @return the half-edge, or null if none is found.
     */
    public HalfEdge findEdge(Vertex vt, Vertex vh) {
        HalfEdge he = he0;
        do {
            if (he.head() == vh && he.tail() == vt) {
                return he;
            }
            he = he.next;
        } while (he != he0);
        return null;
    }

    /**
     * Computes the distance from a point p to the plane of this face.
     *
     * @param p the point
     * @return distance from the point to the plane
     */
    public double distanceToPlane(Point3D p) {
        return normal.getX() * p.getX() + normal.getY() * p.getY() + normal.getZ() * p.getZ() - planeOffset;
    }

    /**
     * Returns the normal of the plane associated with this face.
     *
     * @return the planar normal
     */
    public Vector3D getNormal() {
        return normal;
    }

    /**
     * Gets the centroid.
     *
     * @return the centroid
     */
    public Point3D getCentroid() {
        return centroid;
    }

    /**
     * Num vertices.
     *
     * @return the int
     */
    public int numVertices() {
        return numVerts;
    }

    /**
     * Gets the vertex string.
     *
     * @return the vertex string
     */
    public String getVertexString() {
        String s = null;
        HalfEdge he = he0;
        do {
            if (s == null) {
                s = "" + he.head().index;
            } else {
                s += " " + he.head().index;
            }
            he = he.next;
        } while (he != he0);
        return s;
    }

    /**
     * Gets the vertex indices.
     *
     * @param idxs the idxs
     */
    public void getVertexIndices(int[] idxs) {
        HalfEdge he = he0;
        int i = 0;
        do {
            idxs[i++] = he.head().index;
            he = he.next;
        } while (he != he0);
    }

    /**
     * Connect half edges.
     *
     * @param hedgePrev the hedge prev
     * @param hedge the hedge
     * @return the face
     */
    private Face connectHalfEdges(
            HalfEdge hedgePrev, HalfEdge hedge) throws QuickHullException {
        Face discardedFace = null;

        if (hedgePrev.oppositeFace() == hedge.oppositeFace()) { // then there is a redundant edge that we can get rid off

            Face oppFace = hedge.oppositeFace();
            HalfEdge hedgeOpp;

            if (hedgePrev == he0) {
                he0 = hedge;
            }
            if (oppFace.numVertices() == 3) { // then we can get rid of the opposite face altogether
                hedgeOpp = hedge.getOpposite().prev.getOpposite();

                oppFace.mark = DELETED;
                discardedFace = oppFace;
            } else {
                hedgeOpp = hedge.getOpposite().next;

                if (oppFace.he0 == hedgeOpp.prev) {
                    oppFace.he0 = hedgeOpp;
                }
                hedgeOpp.prev = hedgeOpp.prev.prev;
                hedgeOpp.prev.next = hedgeOpp;
            }
            hedge.prev = hedgePrev.prev;
            hedge.prev.next = hedge;

            hedge.opposite = hedgeOpp;
            hedgeOpp.opposite = hedge;

            // oppFace was modified, so need to recompute
            oppFace.computeNormalAndCentroid();
        } else {
            hedgePrev.next = hedge;
            hedge.prev = hedgePrev;
        }
        return discardedFace;
    }

    /**
     * Check consistency.
     */
    void checkConsistency() throws QuickHullException {
        // do a sanity check on the face
        HalfEdge hedge = he0;
        double maxd = 0;
        int numv = 0;

        if (numVerts < 3) {
            throw new QuickHullException(
                    "degenerate face: " + getVertexString());
        }
        do {
            HalfEdge hedgeOpp = hedge.getOpposite();
            if (hedgeOpp == null) {
                throw new QuickHullException(
                        "face " + getVertexString() + ": "
                        + "unreflected half edge " + hedge.getVertexString());
            } else if (hedgeOpp.getOpposite() != hedge) {
                throw new QuickHullException(
                        "face " + getVertexString() + ": "
                        + "opposite half edge " + hedgeOpp.getVertexString()
                        + " has opposite "
                        + hedgeOpp.getOpposite().getVertexString());
            }
            if (hedgeOpp.head() != hedge.tail()
                    || hedge.head() != hedgeOpp.tail()) {
                throw new QuickHullException(
                        "face " + getVertexString() + ": "
                        + "half edge " + hedge.getVertexString()
                        + " reflected by " + hedgeOpp.getVertexString());
            }
            Face oppFace = hedgeOpp.face;
            if (oppFace == null) {
                throw new QuickHullException(
                        "face " + getVertexString() + ": "
                        + "no face on half edge " + hedgeOpp.getVertexString());
            } else if (oppFace.mark == DELETED) {
                throw new QuickHullException(
                        "face " + getVertexString() + ": "
                        + "opposite face " + oppFace.getVertexString()
                        + " not on hull");
            }
            double d = Math.abs(distanceToPlane(hedge.head().pnt));
            if (d > maxd) {
                maxd = d;
            }
            numv++;
            hedge = hedge.next;
        } while (hedge != he0);

        if (numv != numVerts) {
            throw new QuickHullException(
                    "face " + getVertexString() + " numVerts=" + numVerts + " should be " + numv);
        }

    }

    /**
     * Merge adjacent face.
     *
     * @param hedgeAdj the hedge adj
     * @param discarded the discarded
     * @return the int
     */
    public int mergeAdjacentFace(HalfEdge hedgeAdj,
            Face[] discarded) throws QuickHullException {
        Face oppFace = hedgeAdj.oppositeFace();
        int numDiscarded = 0;

        discarded[numDiscarded++] = oppFace;
        oppFace.mark = DELETED;

        HalfEdge hedgeOpp = hedgeAdj.getOpposite();

        HalfEdge hedgeAdjPrev = hedgeAdj.prev;
        HalfEdge hedgeAdjNext = hedgeAdj.next;
        HalfEdge hedgeOppPrev = hedgeOpp.prev;
        HalfEdge hedgeOppNext = hedgeOpp.next;

        while (hedgeAdjPrev.oppositeFace() == oppFace) {
            hedgeAdjPrev = hedgeAdjPrev.prev;
            hedgeOppNext = hedgeOppNext.next;
        }

        while (hedgeAdjNext.oppositeFace() == oppFace) {
            hedgeOppPrev = hedgeOppPrev.prev;
            hedgeAdjNext = hedgeAdjNext.next;
        }

        HalfEdge hedge;

        for (hedge = hedgeOppNext; hedge != hedgeOppPrev.next; hedge = hedge.next) {
            hedge.face = this;
        }

        if (hedgeAdj == he0) {
            he0 = hedgeAdjNext;
        }

        // handle the half edges at the head
        Face discardedFace;

        discardedFace = connectHalfEdges(hedgeOppPrev, hedgeAdjNext);
        if (discardedFace != null) {
            discarded[numDiscarded++] = discardedFace;
        }

        // handle the half edges at the tail
        discardedFace = connectHalfEdges(hedgeAdjPrev, hedgeOppNext);
        if (discardedFace != null) {
            discarded[numDiscarded++] = discardedFace;
        }

        computeNormalAndCentroid();
        checkConsistency();

        return numDiscarded;
    }

    /**
     * Area squared.
     *
     * @param hedge0 the hedge0
     * @param hedge1 the hedge1
     * @return the double
     */
    private double areaSquared(HalfEdge hedge0, HalfEdge hedge1) {
        // return the squared area of the triangle defined
        // by the half edge hedge0 and the point at the
        // head of hedge1.

        Point3D p0 = hedge0.tail().pnt;
        Point3D p1 = hedge0.head().pnt;
        Point3D p2 = hedge1.head().pnt;

        double dx1 = p1.getX() - p0.getX();
        double dy1 = p1.getY() - p0.getY();
        double dz1 = p1.getZ() - p0.getZ();

        double dx2 = p2.getX() - p0.getX();
        double dy2 = p2.getY() - p0.getY();
        double dz2 = p2.getZ() - p0.getZ();

        double x = dy1 * dz2 - dz1 * dy2;
        double y = dz1 * dx2 - dx1 * dz2;
        double z = dx1 * dy2 - dy1 * dx2;

        return x * x + y * y + z * z;
    }

    /**
     * Triangulate.
     *
     * @param newFaces the new faces
     * @param minArea the min area
     */
    public void triangulate(FaceList newFaces, double minArea) throws QuickHullException {
        HalfEdge hedge;

        if (numVertices() < 4) {
            return;
        }

        Vertex v0 = he0.head();
        Face prevFace = null;

        hedge = he0.next;
        HalfEdge oppPrev = hedge.opposite;
        Face face0 = null;

        for (hedge = hedge.next; hedge != he0.prev; hedge = hedge.next) {
            Face face
                    = createTriangle(v0, hedge.prev.head(), hedge.head(), minArea);
            face.he0.next.setOpposite(oppPrev);
            face.he0.prev.setOpposite(hedge.opposite);
            oppPrev = face.he0;
            newFaces.add(face);
            if (face0 == null) {
                face0 = face;
            }
        }
        hedge = new HalfEdge(he0.prev.prev.head(), this);
        hedge.setOpposite(oppPrev);

        hedge.prev = he0;
        hedge.prev.next = hedge;

        hedge.next = he0.prev;
        hedge.next.prev = hedge;

        computeNormalAndCentroid(minArea);
        checkConsistency();

        for (Face face = face0; face != null; face = face.next) {
            face.checkConsistency();
        }

    }
}
