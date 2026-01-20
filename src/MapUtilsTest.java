import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for core map-based utilities used by the algorithm.
 * Tests: allDistance, shortestPath, getNearestEatable, fill.
 */
public class MapUtilsTest {

    @Test
    public void testAllDistance_basic() {
        // 3x3 empty map, non-cyclic for deterministic distances
        int[][] arr = new int[3][3];
        Map m = new Map(arr);
        m.setCyclic(false);

        Map2D d = m.allDistance(new Index2D(1, 1), 1); // obsColor = 1 (no obstacles)

        // center should be distance 0
        assertEquals(0, d.getPixel(1, 1));
        // direct neighbors distance 1
        assertEquals(1, d.getPixel(1, 2));
        assertEquals(1, d.getPixel(2, 1));
        // corner (0,0) has Manhattan distance 2 from center (1,1)
        assertEquals(2, d.getPixel(0, 0));
    }

    @Test
    public void testShortestPath_simple() {
        // 3x3 empty map, path from (0,0) to (2,2) is Manhattan distance 4
        int[][] arr = new int[3][3];
        Map m = new Map(arr);
        m.setCyclic(false);

        Pixel2D start = new Index2D(0, 0);
        Pixel2D goal = new Index2D(2, 2);
        Pixel2D[] path = m.shortestPath(start, goal, 1);

        assertNotNull(path, "expected a path");
        // Manhattan distance 4 => path length should be distance+1 = 5
        assertEquals(5, path.length);
        // path should start at start and end at goal
        assertEquals(start.getX(), path[0].getX());
        assertEquals(start.getY(), path[0].getY());
        assertEquals(goal.getX(), path[path.length - 1].getX());
        assertEquals(goal.getY(), path[path.length - 1].getY());
    }

    @Test
    public void testGetNearestEatable_adjacent() {
        // Build a 3x3 map and place an eatable (3) to the right of the starting cell
        int[][] arr = new int[3][3];
        arr[1][0] = 3; // set cell (1,0) to be an eatable
        Map m = new Map(arr);
        m.setCyclic(false);

        Pixel2D start = new Index2D(0, 0);
        Pixel2D nearest = m.getNearestEatable(start, 1, new int[]{3, 5});
        assertNotNull(nearest, "expected to find adjacent eatable");
        assertEquals(1, nearest.getX());
        assertEquals(0, nearest.getY());
    }

    @Test
    public void testFill_updatesConnectedRegion() {
        // Create a 3x3 map with a single pixel of value 1 in center
        int[][] arr = new int[3][3];
        arr[1][1] = 1;
        Map m = new Map(arr);
        m.setCyclic(false);

        int changed = m.fill(new Index2D(1, 1), 9);
        assertEquals(1, changed, "only the single connected pixel should be changed");
        assertEquals(9, m.getPixel(1, 1));
    }
}

