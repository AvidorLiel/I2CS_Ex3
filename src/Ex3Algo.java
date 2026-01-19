
import exe.ex3.game.Game;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;

import java.awt.*;

import java.util.*;
import java.awt.Color;

// וודאי שהחבילה (package) רשומה כאן אם הקובץ נמצא בתיקייה מסוימת
// package exe.ex3;

/**
 * Ex3Algo - Smart Pacman navigation logic.
 * This implementation avoids ghosts and targets food using BFS.
 */
public class Ex3Algo implements PacmanAlgo {
    private Map2D _map;

    @Override
    public String getInfo() {
        return "Custom BFS Navigator - Optimized for Safety and Speed";
    }

    @Override
    public int getDirection(GameInfo game) {
        if (game == null || game.getPacman() == null) return -1;

        this._map = game.getMap();
        Pixel2D currentPos = game.getPacman().getLocation();

        // Dynamic ghost avoidance list
        List<Ghost> ghosts = game.getGhosts();

        // Find the next best step
        Pixel2D nextStep = findOptimalPath(game, currentPos, ghosts);

        if (nextStep == null) {
            // If no path to food is safe, try to just move away from nearest ghost
            return escapeLogic(currentPos, ghosts);
        }

        return calculateDirection(currentPos, nextStep);
    }

    private Pixel2D findOptimalPath(GameInfo game, Pixel2D start, List<Ghost> ghosts) {
        Queue<Pixel2D> queue = new LinkedList<>();
        Map<Pixel2D, Pixel2D> parentMap = new HashMap<>();
        Set<Pixel2D> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Pixel2D curr = queue.poll();

            // Check if this pixel is a pink dot (food)
            if (_map.getPixel(curr) == 1) {
                return backtrack(curr, parentMap, start);
            }

            for (Pixel2D neighbor : getValidNeighbors(curr, ghosts)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parentMap.put(neighbor, curr);
                    queue.add(neighbor);
                }
            }
        }
        return null;
    }

    private List<Pixel2D> getValidNeighbors(Pixel2D p, List<Ghost> ghosts) {
        List<Pixel2D> neighbors = new ArrayList<>();
        int[] dx = {0, 1, 0, -1};
        int[] dy = {1, 0, -1, 0};

        for (int i = 0; i < 4; i++) {
            Pixel2D next = new Index2D(p.getX() + dx[i], p.getY() + dy[i]);
            if (_map.isInside(next) && _map.getPixel(next) != 0) {
                if (isLocationSafe(next, ghosts)) {
                    neighbors.add(next);
                }
            }
        }
        return neighbors;
    }

    private boolean isLocationSafe(Pixel2D p, List<Ghost> ghosts) {
        for (Ghost g : ghosts) {
            // Stay at least 2 steps away from any ghost
            if (p.distance(g.getLocation()) < 2.1) return false;
        }
        return true;
    }

    private int escapeLogic(Pixel2D current, List<Ghost> ghosts) {
        // Simple logic to move to the first valid neighbor if trapped
        for (int i = 0; i < 4; i++) {
            int[] dx = {0, 1, 0, -1};
            int[] dy = {1, 0, -1, 0};
            Pixel2D next = new Index2D(current.getX() + dx[i], current.getY() + dy[i]);
            if (_map.isInside(next) && _map.getPixel(next) != 0) return i;
        }
        return -1;
    }

    private Pixel2D backtrack(Pixel2D target, Map<Pixel2D, Pixel2D> parentMap, Pixel2D start) {
        Pixel2D curr = target;
        while (parentMap.get(curr) != null && !parentMap.get(curr).equals(start)) {
            curr = parentMap.get(curr);
        }
        return curr;
    }

    private int calculateDirection(Pixel2D from, Pixel2D to) {
        if (to.getY() > from.getY()) return 0; // UP
        if (to.getX() > from.getX()) return 1; // RIGHT
        if (to.getY() < from.getY()) return 2; // DOWN
        if (to.getX() < from.getX()) return 3; // LEFT
        return -1;
    }
}