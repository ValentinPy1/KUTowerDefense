package towerdefense.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Represents the game map data, including tile layout and start/end points.
 */
public class GameMap implements Serializable {

    private static final long serialVersionUID = 1L; // For serialization

    private final int width;
    private final int height;
    private TileType[][] tiles;
    private int startRow = -1, startCol = -1;
    private int endRow = -1, endCol = -1;

    public GameMap(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Map dimensions must be positive.");
        }
        this.width = width;
        this.height = height;
        this.tiles = new TileType[height][width];
        // Initialize with GRASS
        for (int r = 0; r < height; r++) {
            Arrays.fill(tiles[r], TileType.GRASS);
        }
        System.out.println("GameMap created: " + width + "x" + height);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public TileType getTile(int row, int col) {
        if (isValid(row, col)) {
            return tiles[row][col];
        }
        return null; // Or throw exception
    }

    public boolean setTile(int row, int col, TileType type) {
        if (isValid(row, col)) {
            // Prevent overwriting START/END directly, use specific methods
            if (tiles[row][col] == TileType.START && type != TileType.START)
                return false;
            if (tiles[row][col] == TileType.END && type != TileType.END)
                return false;

            tiles[row][col] = type;
            // If setting START/END, update coordinates implicitly (or require specific
            // methods)
            if (type == TileType.START)
                setStartPoint(row, col, true);
            if (type == TileType.END)
                setEndPoint(row, col, true);
            return true;
        }
        return false;
    }

    public boolean setStartPoint(int row, int col, boolean forceSetTile) {
        if (!isValid(row, col) || !isEdge(row, col) || getTile(row, col) != TileType.PATH) {
            return false; // Start must be PATH on edge
        }
        // Clear old start point tile if exists
        if (startRow != -1 && startCol != -1) {
            tiles[startRow][startCol] = TileType.PATH; // Revert old start to path
        }
        // Set new start point
        this.startRow = row;
        this.startCol = col;
        if (forceSetTile) {
            tiles[row][col] = TileType.START;
        }
        return true;
    }

    public boolean setEndPoint(int row, int col, boolean forceSetTile) {
        if (!isValid(row, col) || !isEdge(row, col) || getTile(row, col) != TileType.PATH) {
            return false; // End must be PATH on edge
        }
        // Clear old end point tile if exists
        if (endRow != -1 && endCol != -1) {
            tiles[endRow][endCol] = TileType.PATH; // Revert old end to path
        }
        // Set new end point
        this.endRow = row;
        this.endCol = col;
        if (forceSetTile) {
            tiles[row][col] = TileType.END;
        }
        return true;
    }

    public int getStartRow() {
        return startRow;
    }

    public int getStartCol() {
        return startCol;
    }

    public int getEndRow() {
        return endRow;
    }

    public int getEndCol() {
        return endCol;
    }

    private boolean isValid(int row, int col) {
        return row >= 0 && row < height && col >= 0 && col < width;
    }

    private boolean isEdge(int row, int col) {
        return row == 0 || row == height - 1 || col == 0 || col == width - 1;
    }

    /**
     * Validates the map based on requirements.
     * 
     * @return true if the map is valid, false otherwise.
     */
    public boolean validate() {
        // 1. Start point exists, is on edge, and is marked as START
        if (startRow == -1 || startCol == -1 || !isEdge(startRow, startCol)
                || getTile(startRow, startCol) != TileType.START) {
            System.err.println("Validation Error: Start point invalid or missing.");
            return false;
        }
        // 2. End point exists, is on edge, and is marked as END
        if (endRow == -1 || endCol == -1 || !isEdge(endRow, endCol) || getTile(endRow, endCol) != TileType.END) {
            System.err.println("Validation Error: End point invalid or missing.");
            return false;
        }
        // 3. Path connectivity (requires pathfinding - placeholder)
        if (!isPathConnected()) {
            System.err.println("Validation Error: Path is not fully connected from start to end.");
            return false;
        }
        // 4. At least 4 tower slots
        int towerSlotCount = 0;
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                if (tiles[r][c] == TileType.TOWER_SLOT) {
                    towerSlotCount++;
                }
            }
        }
        if (towerSlotCount < 4) {
            System.err.println("Validation Error: Less than 4 tower slots found (" + towerSlotCount + ").");
            return false;
        }

        System.out.println("Map Validation Successful!");
        return true;
    }

    /**
     * Placeholder for pathfinding logic (e.g., BFS or DFS) to check connectivity.
     */
    private boolean isPathConnected() {
        // TODO: Implement actual pathfinding from startRow, startCol to endRow, endCol
        // only traversing PATH tiles.
        System.out.println("Warning: Path connectivity check not implemented.");
        return true; // Placeholder
    }
}