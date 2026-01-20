import java.awt.*;

import exe.ex3.game.Game;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;

/**
 * This is the major algorithmic class for Ex3 - the PacMan game:
 * <p>
 * This code is a very simple example (random-walk algorithm).
 * Your task is to implement (here) your PacMan algorithm.
 */
public class Ex3Algo implements PacManAlgo {
    private int _count;

    public Ex3Algo() {
        _count = 0;
    }

    @Override
    // Add a short description for the algorithm as a String.
    public String getInfo() {
        return "BFS-based navigator with dynamic ghost-avoidance (Option B)";
    }

    @Override
    // This is the main method - design, implement and test.
    public int move(PacmanGame game) {
        if (_count == 0 || _count == 300) { // print debug info at first move and after 300 moves
            int code = 0;
            int[][] board = game.getGame(0);
            printBoard(board);
            int blue = Game.getIntColor(Color.BLUE, code); // get color int code
            int pink = Game.getIntColor(Color.PINK, code);
            int black = Game.getIntColor(Color.BLACK, code);
            int green = Game.getIntColor(Color.GREEN, code);
            System.out.println("Blue=" + blue + ", Pink=" + pink + ", Black=" + black + ", Green=" + green);
            String pos = game.getPos(code);
            System.out.println("Pacman coordinate: " + pos);
            // parse str to pixel
            System.out.println(stringToPixel(pos));
            //
            GhostCL[] ghosts = game.getGhosts(code);
            printGhosts(ghosts);

        }
        _count++;

        GhostCL[] gh = game.getGhosts(0);
        Pixel2D pp = stringToPixel(game.getPos(0));
        Pixel2D[] gp = new Pixel2D[gh.length];
        for (int i = 0; i < gh.length; i++) { // parse ghost positions
            gp[i] = new Index2D(stringToPixel(gh[i].getPos(0)));
        }

        // If any ghost is currently eatable (power mode), chase the nearest edible ghost immediately
        java.util.List<Integer> edibleIdx = new java.util.ArrayList<>();
        for (int i = 0; i < gh.length; i++) {
            try {
                if (gh[i] != null && gh[i].remainTimeAsEatable(0) > 0) edibleIdx.add(i);
            } catch (RuntimeException ignored) {}
        }
        if (!edibleIdx.isEmpty()) {
            // choose nearest edible ghost by shortest path
            int bestI = -1;
            Pixel2D[] bestPath = null;
            Map mapTmp = new Map(game.getGame(0));
            for (int idx : edibleIdx) { // for each edible ghost
                Pixel2D target = gp[idx];
                try {
                    Pixel2D[] path = mapTmp.shortestPath(pp, target, 1);
                    if (path != null && path.length > 0) {
                        if (bestPath == null || path.length < bestPath.length) { bestPath = path; bestI = idx; }
                    }
                } catch (RuntimeException ignored) {}
            }
            if (bestPath != null && bestPath.length > 1) {
                // go to the first step toward the edible ghost
                return toMove(mapTmp, pp, bestPath[1]);
            }
        }

        Map map = new Map(game.getGame(0));
        //helps avoid being eaten even if the game's DT or ghost speed varies
        int w = map.getWidth();
        int h = map.getHeight();
        int[][] base = map.getMap();
        boolean[][] forbidden = new boolean[w][h];
        // scale safeSteps by the game's DT: if DT is smaller (more frequent updates / faster movement),
        // increase safeSteps to be more conservative. Use GameInfo.DT for tuning.
        // Keep forbidden radius small and bounded so we don't mark the whole map as forbidden.
        // safeSteps is in [2,4] depending on DT (more conservative for smaller DT).
        int safeSteps = Math.max(2, Math.min(4, 200 / Math.max(1, GameInfo.DT)));
        for (int i = 0; i < gp.length; i++) {
            Pixel2D gpos = gp[i];
            if (gpos == null) continue;
            // Skip marking forbidden around edible ghosts (we want to chase them)
            boolean edible = false;
            try { edible = (gh[i] != null && gh[i].remainTimeAsEatable(0) > 0); } catch (RuntimeException ignored) {}
            if (edible) continue;
            try {
                Map2D distMap = map.allDistance(gpos, 1); // obstacle-aware distances from ghost
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        int d = distMap.getPixel(x, y);
                        // Do not mark eatables as forbidden (in order to allow Pacman to go eat them)
                        int cell = base[x][y];
                        if (d >= 0 && d <= safeSteps && cell != 3 && cell != 5) forbidden[x][y] = true;
                    }
                }
            } catch (RuntimeException ex) {
                // if any issue computing distances, fall back to marking the ghost cell itself
                int gx = gpos.getX();
                int gy = gpos.getY();
                if (map.isCyclic()) {
                    gx = (gx % w + w) % w; gy = (gy % h + h) % h;
                }
                if (gx >= 0 && gx < w && gy >= 0 && gy < h) forbidden[gx][gy] = true;
            }
        }

        // Create a safe map copy and mark forbidden cells as obstacles (value 1)
        int[][] safeArr = new int[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                safeArr[x][y] = base[x][y];
                if (forbidden[x][y]) safeArr[x][y] = 1; // treat as obstacle
            }
        }
        Map safeMap = new Map(safeArr);
        // find nearest eatable and path on the safe map
        Pixel2D nearest = safeMap.getNearestEatable(pp, 1, new int[]{3, 5});
        Pixel2D[] sp = null;
        if (nearest != null) {
            sp = safeMap.shortestPath(pp, nearest, 1);
        }

        // If safe path exists on safeMap, follow it
        // If Pacman is currently in a forbidden cell, force immediate escape (don't wait for BFS)
        if (forbidden[pp.getX()][pp.getY()]) {
            // choose adjacent non-forbidden tile maximizing distance
            int[] dx = {0, 1, 0, -1};
            int[] dy = {1, 0, -1, 0};
            int bestIdxNow = -1;
            double bestMinDistNow = -1;
            for (int i = 0; i < 4; i++) {
                int nx = pp.getX() + dx[i];
                int ny = pp.getY() + dy[i];
                if (map.isCyclic()) {
                    nx = (nx % w + w) % w;
                    ny = (ny % h + h) % h;
                } else {
                    if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                }
                if (forbidden[nx][ny]) continue;
                if (map.getPixel(nx, ny) == 1) continue;
                double minDist = Double.MAX_VALUE;
                for (Pixel2D gpos : gp) {
                    if (gpos == null) continue;
                    double d = Math.hypot(nx - gpos.getX(), ny - gpos.getY());
                    if (d < minDist) minDist = d;
                }
                if (minDist > bestMinDistNow) { bestMinDistNow = minDist; bestIdxNow = i; }
            }
            if (bestIdxNow != -1) {
                int pref = (bestIdxNow==0?Game.UP:bestIdxNow==1?Game.RIGHT:bestIdxNow==2?Game.DOWN:Game.LEFT);
                return pref;
            }
            // if can't escape to a non-forbidden neighbor, pick any non-wall neighbor
            for (int i = 0; i < 4; i++) {
                int nx = pp.getX() + dx[i];
                int ny = pp.getY() + dy[i];
                if (map.isCyclic()) { nx = (nx % w + w) % w; ny = (ny % h + h) % h; }
                else { if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue; }
                if (map.getPixel(nx, ny) != 1) {
                    switch (i) { case 0: return Game.UP; case 1: return Game.RIGHT; case 2: return Game.DOWN; case 3: return Game.LEFT; }
                }
            }
            return Game.PAUSE;
        }

        if (sp != null && sp.length > 1) {
            int dir = toMove(safeMap, pp, sp[1]);
            return dir;
        }

        // If no safe path to food exists, pick an immediate neighbor that is not forbidden
        // and maximizes distance to nearest ghost (emergency escape)
        // As an additional stronger strategy: compute a "maximin" path to any eatable
        // where we maximize the minimal distance to the nearest ghost along the path.
        // This helps find longer but safer paths when directly-safe BFS fails.
        // Build nearest-ghost distance map (in ghost steps) - smaller is more dangerous.
        int[][] nearestGhostDist = new int[w][h];
        final int LARGE = w + h + 1000;
        for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) nearestGhostDist[x][y] = LARGE;
        for (Pixel2D gpos : gp) {
            if (gpos == null) continue;
            try {
                Map2D gdist = map.allDistance(gpos, 1);
                for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) {
                    int d = gdist.getPixel(x, y);
                    if (d >= 0 && d < nearestGhostDist[x][y]) nearestGhostDist[x][y] = d;
                }
            } catch (RuntimeException ex) {
                int gx = gpos.getX(); int gy = gpos.getY();
                if (map.isCyclic()) { gx = (gx % w + w) % w; gy = (gy % h + h) % h; }
                if (gx >= 0 && gx < w && gy >= 0 && gy < h) nearestGhostDist[gx][gy] = 0;
            }
        }

        // Priority search: maximize pathSafety = min(nearestGhostDist along path), tie-breaker shorter path
        class Node { final int x,y; final int safety; final int steps; Node(int x,int y,int safety,int steps){this.x=x;this.y=y;this.safety=safety;this.steps=steps;} }
        java.util.PriorityQueue<Node> pq = new java.util.PriorityQueue<>((a,b) -> {
            if (a.safety != b.safety) return Integer.compare(b.safety, a.safety); // higher safety first
            return Integer.compare(a.steps, b.steps); // fewer steps next
        });
        int[][] bestSafety = new int[w][h];
        for (int x=0;x<w;x++) for (int y=0;y<h;y++) bestSafety[x][y] = -1;
        int[][] parentDir = new int[w][h]; for (int[] row: parentDir) java.util.Arrays.fill(row, -1);

        int sx = pp.getX(), sy = pp.getY();
        int startSafety = Math.min(nearestGhostDist[sx][sy], LARGE);
        pq.add(new Node(sx, sy, startSafety, 0));
        bestSafety[sx][sy] = startSafety;
        boolean foundPath = false;
        int goalX= -1, goalY = -1;
        while (!pq.isEmpty()) {
            Node cur = pq.poll();
            if (bestSafety[cur.x][cur.y] != cur.safety) continue; // stale
            // if this cell is an eatable on original map (3 or 5), we can stop
            int cellVal = map.getPixel(cur.x, cur.y);
            if (cellVal == 3 || cellVal == 5) { foundPath = true; goalX = cur.x; goalY = cur.y; break; }

            int[] ddx = {0,1,0,-1}; int[] ddy = {1,0,-1,0};
            for (int k=0;k<4;k++) {
                int nx = cur.x + ddx[k]; int ny = cur.y + ddy[k];
                if (map.isCyclic()) { nx = (nx % w + w) % w; ny = (ny % h + h) % h; }
                else { if (nx<0||nx>=w||ny<0||ny>=h) continue; }
                if (map.getPixel(nx, ny) == 1) continue; // obstacle
                if (forbidden[nx][ny]) continue; // avoid forbidden entirely in this search
                int nsafety = Math.min(cur.safety, nearestGhostDist[nx][ny]);
                int nsteps = cur.steps + 1;
                if (nsafety > bestSafety[nx][ny] || (nsafety == bestSafety[nx][ny] && (bestSafety[nx][ny] >= 0 && nsteps < Integer.MAX_VALUE))) {
                    bestSafety[nx][ny] = nsafety;
                    parentDir[nx][ny] = (k+2)%4; // store reverse direction to return: neighbor -> current
                    pq.add(new Node(nx, ny, nsafety, nsteps));
                }
            }
        }

        if (foundPath) {
            // reconstruct first move from start to goal using parentDir
            int cx = goalX, cy = goalY;
            while (!(cx == sx && cy == sy)) {
                int pd = parentDir[cx][cy];
                if (pd < 0) break;
                int px = cx + (pd==1?1:pd==3?-1:0);
                int py = cy + (pd==0?1:pd==2?-1:0);
                if (px == sx && py == sy) {
                    // child (cx,cy) is the neighbor to move to from (sx,sy)
                    int ddx = cx - sx;
                    int ddy = cy - sy;
                    if (ddx == 0 && ddy == 1) return Game.UP;
                    if (ddx == 1 && ddy == 0) return Game.RIGHT;
                    if (ddx == 0 && ddy == -1) return Game.DOWN;
                    if (ddx == -1 && ddy == 0) return Game.LEFT;
                    int dir = toMove(map, pp, new Index2D(cx, cy));
                    return dir;
                }
                // move one step towards parent
                cx = px; cy = py;
            }
        }

        // fallback to previous neighbor-max-distance logic
         int[] dx = {0, 1, 0, -1};
         int[] dy = {1, 0, -1, 0};
         int bestIdx = -1;
         double bestMinDist = -1;
         for (int i = 0; i < 4; i++) {
             int nx = pp.getX() + dx[i];
             int ny = pp.getY() + dy[i];
             if (map.isCyclic()) {
                 nx = (nx % w + w) % w;
                 ny = (ny % h + h) % h;
             } else {
                 if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
             }
             if (forbidden[nx][ny]) continue; // never step into forbidden
             if (map.getPixel(nx, ny) == 1) continue; // obstacle

             double minDist = Double.MAX_VALUE;
             for (Pixel2D gpos : gp) {
                 if (gpos == null) continue;
                 double d = Math.hypot(nx - gpos.getX(), ny - gpos.getY());
                 if (d < minDist) minDist = d;
             }
             if (minDist > bestMinDist) {
                 bestMinDist = minDist;
                 bestIdx = i;
             }
         }
         if (bestIdx != -1) {
             int pref = (bestIdx==0?Game.UP:bestIdx==1?Game.RIGHT:bestIdx==2?Game.DOWN:Game.LEFT);
             return pref;
         }
         // no safe neighbor found in this selection — pick any non-wall neighbor to avoid getting stuck
         for (int i = 0; i < 4; i++) {
             int nx = pp.getX() + dx[i];
             int ny = pp.getY() + dy[i];
             if (map.isCyclic()) { nx = (nx % w + w) % w; ny = (ny % h + h) % h; }
             else { if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue; }
             if (map.getPixel(nx, ny) != 1) {
                 switch (i) { case 0: return Game.UP; case 1: return Game.RIGHT; case 2: return Game.DOWN; case 3: return Game.LEFT; }
             }
         }

        // If nothing found, final fallback: do not move into ghost-occupied tiles; try any non-forbidden neighbor
        for (int i = 0; i < 4; i++) {
            int nx = pp.getX() + dx[i];
            int ny = pp.getY() + dy[i];
            if (map.isCyclic()) {
                nx = (nx % w + w) % w;
                ny = (ny % h + h) % h;
            } else {
                if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
            }
            if (forbidden[nx][ny]) continue;
            if (map.getPixel(nx, ny) == 1) continue;
            switch (i) {
                case 0: return Game.UP;
                case 1: return Game.RIGHT;
                case 2: return Game.DOWN;
                case 3: return Game.LEFT;
            }
        }

        // final fallback
        return Game.PAUSE;
    }

    private static void printBoard(int[][] b) {
        int h = b[0].length;
        for (int x = 0; x < b.length; x++) {
            for (int y = 0; y < h; y++) {
                int v = b[x][y];
                System.out.print(v + "\t");
            }
            System.out.println();
        }
    }

    private static void printGhosts(GhostCL[] gs) { // print ghosts info
        for (GhostCL g : gs) {
            System.out.println("status: " + g.getStatus() + ",  type: " + g.getType() + ",  pos: " + g.getPos(0) + ",  time: " + g.remainTimeAsEatable(0));
        }
    }


    private static Pixel2D stringToPixel(String str) { // parse "x,y" to Pixel2D
        String[] arr = str.split(",");
        int x = Integer.parseInt(arr[0]);
        int y = Integer.parseInt(arr[1]);
        return new Index2D(x, y);
    }

    private static int toMove(Map map, Pixel2D from, Pixel2D to) { // from Pixel2D 'from' to Pixel2D 'to', return direction

        System.out.println("from:" + from);
        System.out.println("to:" + to);

        if (map.isCyclic()) { //
            if (from.getX() == to.getX()) {
                if (from.getY() == map.getHeight() - 1 && to.getY() == 0) {
                    return Game.UP;
                }
                if (from.getY() == 0 && to.getY() == map.getHeight() - 1) {
                    return Game.DOWN;
                }
            }
            if (from.getY() == to.getY()) {
                if (from.getX() == map.getWidth() - 1 && to.getX() == 0) {
                    return Game.RIGHT;
                }
                if (from.getX() == 0 && to.getX() == map.getWidth() - 1) {
                    return Game.LEFT;
                }
            }
        }
        if (to.getX() == from.getX() && to.getY() < from.getY()) {
            return Game.DOWN;
            //	dirs[a] = down;
        }
        if (to.getX() == from.getX() && to.getY() > from.getY()) {
            return Game.UP;
            //	dirs[a] = up;
        }
        if (to.getX() < from.getX() && to.getY() == from.getY()) {
            return Game.LEFT;
            //dirs[a] = right;
        }
        if (to.getX() > from.getX() && to.getY() == from.getY()) {
            return Game.RIGHT;
            //dirs[a] = left;
        }
        return Game.PAUSE;
    }
}
