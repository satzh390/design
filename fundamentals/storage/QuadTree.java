package storage;

import java.util.List;

public class QuadTree {
    List<int[]> points;
    int capacity;
    QuadTree[] children;
    int xMin, xMax, yMin, yMax;

    public QuadTree(int capacity, int xMin, int xMax, int yMin, int yMax) {
        this.capacity = capacity;
        this.points = new java.util.ArrayList<>();
        this.children = null;
        this.xMin = xMin;
        this.xMax = xMax;   
        this.yMin = yMin;
        this.yMax = yMax;
    }

    public boolean insert(int[] point) {
        if (children != null) {
            for (QuadTree child : children) {
                if (child.contains(point)) {
                    return child.insert(point);
                }
            }
            return false;
        }

        if (points.size() < capacity) {
            points.add(point);
            return true;
        } 

        subdivide();
        for (int[] p : points) {
            for (QuadTree child : children) {
                if (child.contains(p)) {
                    child.insert(p);
                    break;
                }
            }
        }
        points.clear();
        return insert(point);
    }

    public boolean remove(int[] point) {
        if (children != null) {
            for (QuadTree child : children) {
                if (child.contains(point)) {
                    return child.remove(point);
                }
            }
            return false;
        }

        return points.removeIf(p -> p[0] == point[0] && p[1] == point[1]);
    }

    private boolean contains(int[] point) {
        return point[0] >= xMin && point[0] <= xMax && point[1] >= yMin && point[1] <= yMax;
    }

    private void subdivide() {
        int midX = (xMin + xMax) / 2;
        int midY = (yMin + yMax) / 2;

        children = new QuadTree[4];
        children[0] = new QuadTree(capacity, xMin, midX, yMin, midY); // SW
        children[1] = new QuadTree(capacity, midX, xMax, yMin, midY); // SE
        children[2] = new QuadTree(capacity, xMin, midX, midY, yMax); // NW
        children[3] = new QuadTree(capacity, midX, xMax, midY, yMax); // NE
    }

    public List<int[]> queryRange(int xMin, int xMax, int yMin, int yMax) {
        List<int[]> result = new java.util.ArrayList<>();
        if (!intersectsRange(xMin, xMax, yMin, yMax)) {
            return result;
        }

        for (int[] point : points) {
            if (point[0] >= xMin && point[0] <= xMax && point[1] >= yMin && point[1] <= yMax) {
                result.add(point);
            }
        }

        if (children != null) {
            for (QuadTree child : children) {
                result.addAll(child.queryRange(xMin, xMax, yMin, yMax));
            }
        }

        return result;
    }

    private boolean intersectsRange(int xMin, int xMax, int yMin, int yMax) {
        return !(this.xMax < xMin || this.xMin > xMax || this.yMax < yMin || this.yMin > yMax);
    }

    public void clear() {
        points.clear();
        if (children != null) {
            for (QuadTree child : children) {
                child.clear();
            }
            children = null;
        }
    }

    public int size() {
        int size = points.size();
        if (children != null) {
            for (QuadTree child : children) {
                size += child.size();
            }
        }
        return size;
    }   

    public static void main(String[] args) {
        QuadTree qt = new QuadTree(4, 0, 100, 0, 100);
        qt.insert(new int[]{10, 10});
        qt.insert(new int[]{20, 20});
        qt.insert(new int[]{30, 30});
        qt.insert(new int[]{40, 40});
        qt.insert(new int[]{50, 50});

        List<int[]> results = qt.queryRange(15, 45, 15, 45);
        for (int[] point : results) {
            System.out.println("Point: (" + point[0] + ", " + point[1] + ")");
        }

        System.out.println("Total points in QuadTree: " + qt.size());

        qt.remove(new int[]{30, 30});
        System.out.println("Total points after removal: " + qt.size());
    }
}
