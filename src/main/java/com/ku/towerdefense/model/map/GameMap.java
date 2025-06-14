package com.ku.towerdefense.model.map;

import com.ku.towerdefense.model.GamePath;
import com.ku.towerdefense.model.entity.Tower;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializable game‑map that stores a 2‑D array of {@link Tile}s plus the
 * derived enemy {@link GamePath}. All JavaFX objects are kept
 * <em>transient</em>
 * and rebuilt after loading so maps can safely be written with plain Java
 * serialization.
 */
public class GameMap implements Serializable {
    private static final long serialVersionUID = 1L;

    /*
     * ------------------------------------------------------------------
     * Core data – these are written to disk
     * ------------------------------------------------------------------
     */
    private String name;
    private int width, height;
    private Tile[][] tiles;

    /* mirror of the (transient) start/end Points so they survive I/O */
    private int[] startXY; // [px, py]
    private int[] endXY; // [px, py]

    /*
     * ------------------------------------------------------------------
     * Transient caches – rebuilt on demand / after load
     * ------------------------------------------------------------------
     */
    private transient Point2D startPoint;
    private transient Point2D endPoint;
    private transient GamePath enemyPath;

    public static final int TILE_SIZE = 64; // Made public and static

    /*
     * ------------------------------------------------------------------
     * C‑TOR
     * ------------------------------------------------------------------
     */
    public GameMap(String name, int width, int height) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.tiles = new Tile[width][height];

        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                tiles[x][y] = new Tile(x, y, TileType.GRASS);
    }

    /*
     * ------------------------------------------------------------------
     * Basic getters/setters that UI code relies on
     * ------------------------------------------------------------------
     */
    public String getName() {
        return name;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTileSize() {
        return TILE_SIZE;
    }

    public Tile getTile(int x, int y) {
        return inBounds(x, y) ? tiles[x][y] : null;
    }

    public TileType getTileType(int x, int y) {
        Tile t = getTile(x, y);
        return t == null ? null : t.getType();
    }

    /*
     * ------------------------------------------------------------------
     * Map editing helpers
     * ------------------------------------------------------------------
     */
    public void setTileType(int x, int y, TileType type) {
        if (!inBounds(x, y))
            return;
        // keep only ONE start / end on the map
        if (type == TileType.START_POINT)
            clearType(TileType.START_POINT);
        if (type == TileType.END_POINT)
            clearType(TileType.END_POINT);
        tiles[x][y].setType(type);
        // Regenerate path if start/end points are affected or if path tiles change.
        // For simplicity, regenerate if any of these critical types are set.
        if (type == TileType.START_POINT || type == TileType.END_POINT || type.toString().startsWith("PATH")) {
            generatePath();
        }
    }

    private void clearType(TileType tt) {
        for (Tile[] row : tiles)
            for (Tile t : row)
                if (t.getType() == tt)
                    t.setType(TileType.GRASS);
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /*
     * ------------------------------------------------------------------
     * Enemy path generation – improved version that follows actual path tiles
     * ------------------------------------------------------------------
     */
    public void generatePath() {
        // Find START_POINT and END_POINT tiles
        Tile startTile = null, endTile = null;
        for (Tile[] row : tiles) {
            for (Tile t : row) {
                if (t.getType() == TileType.START_POINT)
                    startTile = t;
                else if (t.getType() == TileType.END_POINT)
                    endTile = t;
            }
        }

        // If we don't have both start and end points, we can't generate a path
        if (startTile == null || endTile == null) {
            // System.err.println("Cannot generate path: Missing " +
            // (startTile == null ? "START_POINT" : "") +
            // (endTile == null ? "END_POINT" : ""));
            enemyPath = null;
            return;
        }

        final int TS = 32; // logic coords: 32 px per tile
        startPoint = new Point2D(startTile.getX() * TS + TS / 2, startTile.getY() * TS + TS / 2);
        
        // MODIFIED: Make enemies target the RIGHT SIDE of the castle (x+1, y) instead of bottom-left (x, y)
        // Castle structure: END_POINT is at (x,y), so right side is at (x+1, y)
        int castleRightX = endTile.getX() + 1; // Move one tile to the right
        int castleRightY = endTile.getY();     // Same Y coordinate
        endPoint = new Point2D(castleRightX * TS + TS / 2, castleRightY * TS + TS / 2);
        
        startXY = new int[] { (int) startPoint.getX(), (int) startPoint.getY() };
        endXY = new int[] { (int) endPoint.getX(), (int) endPoint.getY() };

        // Use BFS to find a path from start to the tile adjacent to castle right side
        // We need to find path to a walkable tile next to the castle right side
        List<int[]> pathPoints = findPathBFS(startTile, endTile, castleRightX, castleRightY);

        // If no path found, show error and return
        if (pathPoints == null || pathPoints.isEmpty()) {
            // System.err.println(
            // "No valid path found from START_POINT to END_POINT! Make sure they're
            // connected by path tiles.");
            enemyPath = null;
            return;
        }

        // Create GamePath from the points
        enemyPath = new GamePath(pathPoints);
        // System.out.println("Path generated successfully with " + pathPoints.size() +
        // " points");
    }

    /**
     * Uses Breadth-First Search to find a path from startTile to a walkable tile adjacent to the castle right side.
     * The path search aims to reach a tile adjacent to the castle right side position.
     * The coordinates in the returned list are pixel coordinates representing the
     * center of each tile.
     *
     * @param startTile The tile where the path should begin.
     * @param endTile   The END_POINT tile (bottom-left of castle).
     * @param castleRightX The x-coordinate of the castle right side (endTile.x + 1).
     * @param castleRightY The y-coordinate of the castle right side (endTile.y).
     * @return List of [x,y] pixel coordinates for the path in tile-center space
     *         (e.g., tileX * 64 + 32).
     *         Returns null if no path is found.
     */
    public List<int[]> findPathBFS(Tile startTile, Tile endTile, int castleRightX, int castleRightY) {
        final int TS = 64; // pixel size of tiles

        // Directions: right, down, left, up
        int[][] directions = { { 1, 0 }, { 0, 1 }, { -1, 0 }, { 0, -1 } };

        // Keep track of visited tiles and their parent tiles
        boolean[][] visited = new boolean[width][height];
        int[][][] parent = new int[width][height][2]; // Store x,y of parent

        // Initialize queue with start tile
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();
        queue.add(new int[] { startTile.getX(), startTile.getY() });
        visited[startTile.getX()][startTile.getY()] = true;

        // Find a walkable tile adjacent to the castle right side
        int finalTargetX = -1;
        int finalTargetY = -1;

        // BFS loop
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int cx = current[0];
            int cy = current[1];

            // Check if current tile is adjacent to castle right side
            if (isAdjacentToCastleRightSide(cx, cy, castleRightX, castleRightY)) {
                finalTargetX = cx;
                finalTargetY = cy;
                break; // Found a path to adjacent tile, exit loop
            }

            // Check all four directions
            for (int[] dir : directions) {
                int nx = cx + dir[0];
                int ny = cy + dir[1];

                // Skip if out of bounds
                if (nx < 0 || nx >= width || ny < 0 || ny >= height)
                    continue;

                // Skip if already visited
                if (visited[nx][ny])
                    continue;

                // Only consider walkable tiles
                Tile nextTile = tiles[nx][ny];
                if (!nextTile.isWalkable())
                    continue;

                // Mark as visited and save parent
                visited[nx][ny] = true;
                parent[nx][ny] = new int[] { cx, cy };

                // Add to queue
                queue.add(new int[] { nx, ny });
            }
        }

        // If we didn't find a path to any tile adjacent to castle right side
        if (finalTargetX == -1 || finalTargetY == -1) {
            return null;
        }

        // Reconstruct the path from the final target to start
        List<int[]> reversePath = new ArrayList<>();
        int reconstructX = finalTargetX;
        int reconstructY = finalTargetY;

        // Start with the final target point
        reversePath.add(new int[] { reconstructX * TS + TS / 2, reconstructY * TS + TS / 2 });

        // Work backwards to the start
        while (!(reconstructX == startTile.getX() && reconstructY == startTile.getY())) {
            int[] p = parent[reconstructX][reconstructY];
            reconstructX = p[0];
            reconstructY = p[1];
            reversePath.add(new int[] { reconstructX * TS + TS / 2, reconstructY * TS + TS / 2 });
        }

        // Reverse the path to get start-to-end order
        List<int[]> forwardPath = new ArrayList<>();
        for (int i = reversePath.size() - 1; i >= 0; i--) {
            forwardPath.add(reversePath.get(i));
        }

        // Add the castle right side as the final destination
        forwardPath.add(new int[] { castleRightX * TS + TS / 2, castleRightY * TS + TS / 2 });

        return forwardPath;
    }

    /**
     * Check if a tile is adjacent to the castle right side position.
     * Adjacent means directly touching (not diagonal).
     */
    private boolean isAdjacentToCastleRightSide(int tileX, int tileY, int castleRightX, int castleRightY) {
        // Check all four directions from the castle right side
        int[][] adjacentOffsets = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } }; // left, right, up, down
        
        for (int[] offset : adjacentOffsets) {
            int adjX = castleRightX + offset[0];
            int adjY = castleRightY + offset[1];
            
            if (tileX == adjX && tileY == adjY) {
                return true;
            }
        }
        return false;
    }

    public GamePath getEnemyPath() {
        return enemyPath;
    }

    public Point2D getStartPoint() {
        if (startPoint == null && startXY != null)
            startPoint = new Point2D(startXY[0], startXY[1]);
        return startPoint;
    }

    public Point2D getEndPoint() {
        if (endPoint == null && endXY != null)
            endPoint = new Point2D(endXY[0], endXY[1]);
        return endPoint;
    }

    /*
     * ------------------------------------------------------------------
     * Tower placement rule used by gameplay layer
     * ------------------------------------------------------------------
     */
    public boolean canPlaceTower(double px, double py, List<Tower> towers) {
        int tx = (int) (px / getTileSize()), ty = (int) (py / getTileSize());
        Tile t = getTile(tx, ty);
        if (t == null || !t.canPlaceTower())
            return false;
        // Check if any existing tower's center falls into the target tile
        // This logic assumes tower.getX() and .getY() are top-left of the tile.
        return towers.stream().noneMatch(existingTower -> {
            int existingTowerTileX = (int) (existingTower.getX() / getTileSize());
            int existingTowerTileY = (int) (existingTower.getY() / getTileSize());
            return existingTowerTileX == tx && existingTowerTileY == ty;
        });
    }

    /*
     * ------------------------------------------------------------------
     * Minimal rendering (editor / preview)
     * ------------------------------------------------------------------
     */
    public void render(GraphicsContext gc) {
        gc.setFill(Color.web("#282828"));
        gc.fillRect(0, 0, width * getTileSize(), height * getTileSize());
        for (int y_coord = 0; y_coord < height; y_coord++) {
            for (int x_coord = 0; x_coord < width; x_coord++) {
                if (tiles[x_coord][y_coord] != null) {
                    tiles[x_coord][y_coord].render(gc, x_coord, y_coord, getTileSize(), false);
                }
            }
        }
        // Path rendering removed - now handled by flash system in GameController
        generatePath();
    }

    // New method for rendering a scaled preview of the map
    public void renderPreview(GraphicsContext gc, double canvasWidth, double canvasHeight) {
        if (width <= 0 || height <= 0)
            return; // Cannot render an empty or invalid map

        double tileRenderSize = Math.min(canvasWidth / width, canvasHeight / height);

        // Ensure tileRenderSize is at least 1 pixel to avoid issues with tiny maps on
        // large canvases
        tileRenderSize = Math.max(1.0, tileRenderSize);

        double totalMapRenderWidth = tileRenderSize * width;
        double totalMapRenderHeight = tileRenderSize * height;

        // Center the map on the canvas if it's smaller
        double offsetX = (canvasWidth - totalMapRenderWidth) / 2.0;
        double offsetY = (canvasHeight - totalMapRenderHeight) / 2.0;

        gc.clearRect(0, 0, canvasWidth, canvasHeight); // Clear canvas
        // Optional: fill background for the preview area
        gc.setFill(Color.rgb(50, 50, 50)); // Dark gray background for preview
        gc.fillRect(0, 0, canvasWidth, canvasHeight);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Tile tile = tiles[x][y];
                if (tile != null) {
                    gc.setFill(getColorForTileType(tile.getType()));
                    gc.fillRect(offsetX + x * tileRenderSize,
                            offsetY + y * tileRenderSize,
                            tileRenderSize,
                            tileRenderSize);
                }
            }
        }

        // Optionally, draw start and end points if they exist
        Tile startTile = findTileByType(TileType.START_POINT);
        Tile endTile = findTileByType(TileType.END_POINT);

        if (startTile != null) {
            gc.setFill(Color.GREENYELLOW);
            gc.fillRect(offsetX + startTile.getX() * tileRenderSize,
                    offsetY + startTile.getY() * tileRenderSize,
                    tileRenderSize, tileRenderSize);
        }
        if (endTile != null) {
            gc.setFill(Color.INDIANRED);
            gc.fillRect(offsetX + endTile.getX() * tileRenderSize,
                    offsetY + endTile.getY() * tileRenderSize,
                    tileRenderSize, tileRenderSize);
        }
    }

    // Helper to find the first occurrence of a tile type (used for preview)
    private Tile findTileByType(TileType typeToFind) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y] != null && tiles[x][y].getType() == typeToFind) {
                    return tiles[x][y];
                }
            }
        }
        return null;
    }

    // Helper method to get a distinct color for each tile type for preview
    private Color getColorForTileType(TileType type) {
        if (type == null)
            return Color.BLACK; // Should not happen
        switch (type) {
            case GRASS:
                return Color.DARKGREEN;
            case PATH: // Legacy
            case PATH_HORIZONTAL:
            case PATH_VERTICAL:
            case PATH_CIRCLE_N:
            case PATH_CIRCLE_NE:
            case PATH_CIRCLE_E:
            case PATH_CIRCLE_SE:
            case PATH_CIRCLE_S:
            case PATH_CIRCLE_SW:
            case PATH_CIRCLE_W:
            case PATH_CIRCLE_NW:
            case PATH_VERTICAL_N_DE:
            case PATH_VERTICAL_S_DE:
            case PATH_HORIZONTAL_W_DE:
            case PATH_HORIZONTAL_E_DE:
                return Color.SANDYBROWN;
            case TOWER_SLOT:
                return Color.LIGHTSLATEGRAY;
            case START_POINT:
                return Color.LIMEGREEN; // Will be overridden by specific draw logic
            case END_POINT:
                return Color.TOMATO; // Will be overridden
            case TREE_BIG:
            case TREE_MEDIUM:
            case TREE_SMALL:
                return Color.FORESTGREEN;
            case ROCK_SMALL:
            case ROCK_MEDIUM:
                return Color.SLATEGRAY;
            case HOUSE:
                return Color.CHOCOLATE;
            case WELL:
                return Color.AQUAMARINE;
            case LOG_PILE:
                return Color.SIENNA;
            // Towers and Castle parts might not be directly on map as primary types for
            // preview
            // but if they are:
            case TOWER_ARTILLERY:
                return Color.DARKRED;
            case TOWER_MAGE:
                return Color.BLUEVIOLET;
            case ARCHER_TOWER:
                return Color.DARKOLIVEGREEN;
            case TOWER_BARACK:
                return Color.CADETBLUE;
            case CASTLE1:
            case CASTLE2:
            case CASTLE3:
            case CASTLE4:
                return Color.DARKGOLDENROD;
            default:
                return Color.GRAY; // Default for any other types
        }
    }

    /*
     * ------------------------------------------------------------------
     * Custom serialisation so transient fields get rebuilt
     * ------------------------------------------------------------------
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject(); // Read non-transient fields

        // Rebuild transient Point2D representations from serialized int arrays
        if (startXY != null) {
            startPoint = new Point2D(startXY[0], startXY[1]);
        }
        if (endXY != null) {
            endPoint = new Point2D(endXY[0], endXY[1]);
        }

        // Reinitialize all tiles as they have transient fields (like their Image)
        if (tiles != null) {
            for (int x_coord = 0; x_coord < width; x_coord++) {
                for (int y_coord = 0; y_coord < height; y_coord++) {
                    if (tiles[x_coord][y_coord] != null) {
                        tiles[x_coord][y_coord].reinitializeAfterLoad();
                    } else {
                        // Handle cases where a tile might be unexpectedly null after deserialization
                        // This might indicate an issue with map saving or a very old map format.
                        // For robustness, we could replace it with a default tile.
                        System.err.println("Warning: Null tile found at (" + x_coord + "," + y_coord
                                + ") after loading map '" + name + "'. Replacing with GRASS.");
                        tiles[x_coord][y_coord] = new Tile(x_coord, y_coord, TileType.GRASS);
                        tiles[x_coord][y_coord].reinitializeAfterLoad();
                    }
                }
            }
        } else {
            // If the entire tiles array is null, the map is fundamentally corrupt or empty.
            // Initialize to a default empty state or throw an error.
            System.err
                    .println("Warning: Tile array was null after loading map '" + name + "'. Reinitializing to Grass.");
            this.tiles = new Tile[width][height];
            for (int x_coord = 0; x_coord < width; x_coord++) {
                for (int y_coord = 0; y_coord < height; y_coord++) {
                    tiles[x_coord][y_coord] = new Tile(x_coord, y_coord, TileType.GRASS);
                    tiles[x_coord][y_coord].reinitializeAfterLoad();
                }
            }
        }

        // Regenerate the enemy path using the loaded and reinitialized tile data
        generatePath();
    }

    public void setTileAsOccupiedByTower(int tileX, int tileY, boolean isOccupied) {
        if (tileX >= 0 && tileX < width && tileY >= 0 && tileY < height) {
            Tile tile = getTile(tileX, tileY);
            if (tile != null) {
                if (isOccupied) {
                    // Make sure it was a tower slot before changing it, to avoid issues if logic is
                    // flawed
                    if (tile.getType() == TileType.TOWER_SLOT) {
                        tile.setType(TileType.GRASS); // Occupy by changing to a non-placeable type
                        System.out.println("Tile (" + tileX + "," + tileY + ") changed to GRASS (occupied).");
                    } else {
                        System.err.println("Attempted to occupy a non-TOWER_SLOT tile at (" + tileX + "," + tileY
                                + ") Type: " + tile.getType());
                    }
                } else {
                    // When selling, change it back to a tower slot
                    // This assumes the tile was originally a TOWER_SLOT and became GRASS (or other)
                    // A more robust system might store original tile type or use a specific
                    // OCCUPIED_TOWER_SLOT type
                    tile.setType(TileType.TOWER_SLOT); // Free up by changing back to TOWER_SLOT
                    System.out.println("Tile (" + tileX + "," + tileY + ") changed back to TOWER_SLOT (unoccupied).");
                }
            }
        }
    }
}
