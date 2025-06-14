package com.ku.towerdefense.model.map;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Represents a single tile on the map.
 */
public class Tile implements Serializable {

    /* ───────────────────────────── Constants ───────────────────────────── */

    private static final long serialVersionUID = 1L;
    public static final int SOURCE_TILE_SIZE = 64; // source atlas images are 64px
    private static final int RENDER_TILE_SIZE = 64; // we always draw @64 px

    /** Stores coordinate points (x, y) for tile types within the tileset **/
    private static final Map<TileType, Point2D> TILE_COORDS = new EnumMap<>(TileType.class);

    static {
        // Updated coordinates based on the tileset image
        // First row (top) in tileset
        TILE_COORDS.put(TileType.PATH_CIRCLE_NW, new Point2D(0, 0));
        TILE_COORDS.put(TileType.PATH_CIRCLE_N, new Point2D(1, 0));
        TILE_COORDS.put(TileType.PATH_CIRCLE_NE, new Point2D(2, 0));
        TILE_COORDS.put(TileType.PATH_VERTICAL_N_DE, new Point2D(3, 0));
        TILE_COORDS.put(TileType.PATH_CIRCLE_E, new Point2D(0, 1));
        TILE_COORDS.put(TileType.GRASS, new Point2D(1, 1));
        TILE_COORDS.put(TileType.PATH_CIRCLE_SE, new Point2D(2, 1));
        TILE_COORDS.put(TileType.PATH_VERTICAL, new Point2D(3, 1));
        TILE_COORDS.put(TileType.PATH_CIRCLE_S, new Point2D(0, 2));
        TILE_COORDS.put(TileType.PATH_CIRCLE_SW, new Point2D(1, 2));
        TILE_COORDS.put(TileType.PATH_CIRCLE_W, new Point2D(2, 2));
        TILE_COORDS.put(TileType.PATH_VERTICAL_S_DE, new Point2D(3, 2));
        TILE_COORDS.put(TileType.PATH_HORIZONTAL_W_DE, new Point2D(0, 3));
        TILE_COORDS.put(TileType.PATH_HORIZONTAL, new Point2D(1, 3));
        TILE_COORDS.put(TileType.PATH_HORIZONTAL_E_DE, new Point2D(2, 3));
        TILE_COORDS.put(TileType.TOWER_SLOT, new Point2D(3, 3));
        TILE_COORDS.put(TileType.START_POINT, new Point2D(2, 7)); // Use TOWER_BARACK coordinates for START_POINT
        TILE_COORDS.put(TileType.TREE_BIG, new Point2D(0, 4));
        TILE_COORDS.put(TileType.TREE_MEDIUM, new Point2D(1, 4));
        TILE_COORDS.put(TileType.TREE_SMALL, new Point2D(2, 4));
        TILE_COORDS.put(TileType.ROCK_SMALL, new Point2D(3, 4));
        TILE_COORDS.put(TileType.TOWER_ARTILLERY, new Point2D(0, 5));
        TILE_COORDS.put(TileType.TOWER_MAGE, new Point2D(1, 5));
        TILE_COORDS.put(TileType.HOUSE, new Point2D(2, 5));
        TILE_COORDS.put(TileType.ROCK_MEDIUM, new Point2D(3, 5));
        TILE_COORDS.put(TileType.CASTLE1, new Point2D(0, 6));
        TILE_COORDS.put(TileType.CASTLE2, new Point2D(1, 6));
        TILE_COORDS.put(TileType.ARCHER_TOWER, new Point2D(2, 6));
        TILE_COORDS.put(TileType.WELL, new Point2D(3, 6));
        TILE_COORDS.put(TileType.CASTLE3, new Point2D(0, 7));
        TILE_COORDS.put(TileType.CASTLE4, new Point2D(1, 7));
        TILE_COORDS.put(TileType.TOWER_BARACK, new Point2D(2, 7));
        TILE_COORDS.put(TileType.LOG_PILE, new Point2D(3, 7));

        // Add coordinates for the logical END_POINT, matching CASTLE1
        TILE_COORDS.put(TileType.END_POINT, new Point2D(0, 6));

        // Add coordinates for the deprecated PATH type, matching PATH_HORIZONTAL
        TILE_COORDS.put(TileType.PATH, new Point2D(1, 3));
    }

    /* ────────────────────────── Static Resources ───────────────────────── */

    private static boolean imagesLoaded = false;
    private static Image tileset; // The whole atlas
    private static Image castleImage; // Specific image for end point
    private static Image towerSlotImage; // Specific image for tower slot
    private static final Map<TileType, Image> CACHE = new EnumMap<>(TileType.class);

    public static boolean isFxAvailable = true; // New flag for FX availability

    /* ────────────────────────────── Fields ─────────────────────────────── */

    private final int x, y;
    private TileType type;
    private transient Image image;

    /* ───────────────────────────── Public API ──────────────────────────── */

    public Tile(int x, int y, TileType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        loadImagesIfNeeded();
        initTransientFields();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public TileType getType() {
        return type;
    }

    public void setType(TileType type) {
        if (this.type != type) {
            this.type = type;
            initTransientFields();
        }
    }

    public boolean canPlaceTower() {
        return type == TileType.TOWER_SLOT;
    }

    public boolean isWalkable() {
        return type == TileType.PATH_HORIZONTAL ||
                type == TileType.PATH_VERTICAL ||
                type == TileType.PATH || // Include legacy PATH type
                type == TileType.PATH_CIRCLE_N ||
                type == TileType.PATH_CIRCLE_NE ||
                type == TileType.PATH_CIRCLE_E ||
                type == TileType.PATH_CIRCLE_SE ||
                type == TileType.PATH_CIRCLE_S ||
                type == TileType.PATH_CIRCLE_SW ||
                type == TileType.PATH_CIRCLE_W ||
                type == TileType.PATH_CIRCLE_NW ||
                type == TileType.PATH_VERTICAL_N_DE ||
                type == TileType.PATH_VERTICAL_S_DE ||
                type == TileType.PATH_HORIZONTAL_W_DE ||
                type == TileType.PATH_HORIZONTAL_E_DE ||
                type == TileType.START_POINT; // Start point is also walkable
    }

    public Image getImage() {
        return image;
    }

    public static Image getBaseImageForType(TileType type) {
        loadImagesIfNeeded();
        return switch (type) {
            // Treat END_POINT, CASTLE1-4, and TOWER_SLOT like regular tiles from the
            // tileset
            // Their appearance will be determined by slicing based on TILE_COORDS
            // and compositing if they are overlay props.
            case END_POINT, CASTLE1, CASTLE2, CASTLE3, CASTLE4, TOWER_SLOT -> tileset;
            default -> tileset; // All others use the main tileset
        };
    }

    public Rectangle2D getSourceViewport() {
        Point2D coords = TILE_COORDS.get(type);
        if (coords != null) {
            return new Rectangle2D(
                    coords.getX() * SOURCE_TILE_SIZE,
                    coords.getY() * SOURCE_TILE_SIZE,
                    SOURCE_TILE_SIZE,
                    SOURCE_TILE_SIZE);
        }
        return null;
    }

    public static Rectangle2D getSourceViewportForType(TileType type) {
        Point2D coords = TILE_COORDS.get(type);
        if (coords != null) {
            return new Rectangle2D(
                    coords.getX() * SOURCE_TILE_SIZE,
                    coords.getY() * SOURCE_TILE_SIZE,
                    SOURCE_TILE_SIZE,
                    SOURCE_TILE_SIZE);
        }
        return null;
    }

    public void render(GraphicsContext gc, int x, int y, int tileSize, boolean isEditorMode) {
        if (image != null) {
            gc.drawImage(image, x * tileSize, y * tileSize, tileSize, tileSize);
        } else {
            // Fallback drawing if image is null (e.g. asset loading issue)
            gc.setFill(Color.MAGENTA); // Bright color to indicate missing asset
            gc.fillRect(x * tileSize, y * tileSize, tileSize, tileSize);
            gc.setStroke(Color.BLACK);
            gc.strokeText("?", x * tileSize + tileSize / 2.0 - 5, y * tileSize + tileSize / 2.0 + 5);
        }

        if (isEditorMode) {
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, tileSize * 0.5));
            if (type == TileType.START_POINT) {
                // Assuming START_POINT tile visual from tileset is sufficient.
                // Just add a clear text label.
                gc.setStroke(Color.WHITE);
                gc.setFill(Color.rgb(0, 0, 180)); // Darker blue text fill for contrast on potentially red 'S' tile
                gc.setLineWidth(1);
                gc.strokeText("S", x * tileSize + tileSize * 0.30, y * tileSize + tileSize * 0.70);
                gc.fillText("S", x * tileSize + tileSize * 0.30, y * tileSize + tileSize * 0.70);
            } else if (type == TileType.END_POINT) {
                // Assuming END_POINT tile visual from tileset is sufficient.
                // Just add a clear text label on the base tile.
                gc.setStroke(Color.WHITE);
                gc.setFill(Color.rgb(180, 0, 0)); // Darker red text fill for contrast on potentially blue 'E' tile
                gc.setLineWidth(1);
                gc.strokeText("E", x * tileSize + tileSize * 0.30, y * tileSize + tileSize * 0.70);
                gc.fillText("E", x * tileSize + tileSize * 0.30, y * tileSize + tileSize * 0.70);

                // For editor, also show the 2x2 footprint of the castle faintly if this is the
                // base
                // Note: This doesn't check map boundaries, so it might draw partially
                // off-canvas
                // if the END_POINT is at the very edge of the map. This is an editor-only
                // visual aid.
                gc.setGlobalAlpha(0.3); // Faint
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(1);
                // Top-right
                gc.strokeRect((x + 1) * tileSize + 1, y * tileSize + 1, tileSize - 2, tileSize - 2);
                // Bottom-left
                gc.strokeRect(x * tileSize + 1, (y + 1) * tileSize + 1, tileSize - 2, tileSize - 2);
                // Bottom-right
                gc.strokeRect((x + 1) * tileSize + 1, (y + 1) * tileSize + 1, tileSize - 2, tileSize - 2);
                gc.setGlobalAlpha(1.0);
            }
        }

        // Health bar rendering (for enemies/towers, not map tiles generally)
        // ... (existing health bar code) ...

        // Actual 2x2 Castle rendering for END_POINT (only in non-editor mode)
        if (type == TileType.END_POINT && !isEditorMode) {
            renderEndPoint(gc, x, y, tileSize);
        }
    }

    /**
     * Special rendering for the castle (END_POINT)
     * Draws a proper 2x2 castle using the four castle tiles
     */
    private void renderEndPoint(GraphicsContext gc, int tileX, int tileY, int renderTileSize) {
        loadImagesIfNeeded();

        // Use a larger castle that spans 2x2 tiles
        double castleSize = renderTileSize * 2;

        // Grass background
        Image grassImage = CACHE.get(TileType.GRASS);
        if (grassImage != null) {
            // Draw grass under all castle tiles
            gc.drawImage(grassImage, tileX * renderTileSize, tileY * renderTileSize,
                    renderTileSize, renderTileSize);
            gc.drawImage(grassImage, (tileX + 1) * renderTileSize, tileY * renderTileSize,
                    renderTileSize, renderTileSize);
            gc.drawImage(grassImage, tileX * renderTileSize, (tileY + 1) * renderTileSize,
                    renderTileSize, renderTileSize);
            gc.drawImage(grassImage, (tileX + 1) * renderTileSize, (tileY + 1) * renderTileSize,
                    renderTileSize, renderTileSize);
        }

        // Draw the 4 castle quarters
        drawCastleTile(gc, TileType.CASTLE1, tileX, tileY, renderTileSize);
        drawCastleTile(gc, TileType.CASTLE2, tileX + 1, tileY, renderTileSize);
        drawCastleTile(gc, TileType.CASTLE3, tileX, tileY + 1, renderTileSize);
        drawCastleTile(gc, TileType.CASTLE4, tileX + 1, tileY + 1, renderTileSize);
    }

    /**
     * Helper to draw a specific castle tile part
     */
    private void drawCastleTile(GraphicsContext gc, TileType castleTileType, int tileX, int tileY, int renderTileSize) {
        Rectangle2D viewport = getSourceViewportForType(castleTileType);
        if (viewport != null && tileset != null) {
            // Draw the castle part from the tileset using the viewport
            gc.drawImage(tileset,
                    viewport.getMinX(), viewport.getMinY(),
                    viewport.getWidth(), viewport.getHeight(),
                    tileX * renderTileSize, tileY * renderTileSize,
                    renderTileSize, renderTileSize);
        }
    }

    /**
     * Static getter for the full castle image, primarily for UI elements.
     */
    public static Image getCastleImage() {
        loadImagesIfNeeded();
        return castleImage;
    }

    /* ───────────────────────── Private Helpers ────────────────────────── */

    /**
     * Determines if a given TileType should be rendered by overlaying its image
     * onto the base GRASS tile.
     */
    private static boolean isOverlayProp(TileType type) {
        return switch (type) {
            case TREE_BIG, TREE_MEDIUM, TREE_SMALL,
                    ROCK_SMALL, ROCK_MEDIUM,
                    HOUSE, WELL, LOG_PILE,
                    TOWER_ARTILLERY, TOWER_MAGE, ARCHER_TOWER, TOWER_BARACK,
                    TOWER_SLOT,
                    CASTLE1, CASTLE2, CASTLE3, CASTLE4, END_POINT,
                    START_POINT -> // Add START_POINT as overlay prop
                true;
            default -> false;
        };
    }

    private void initTransientFields() {
        if (!isFxAvailable) {
            this.image = null; // Or a placeholder non-FX image if available/needed
            return;
        }
        // Try to get pre-processed image from cache
        image = CACHE.get(type);
        if (image == null) {
            // Get the base image (e.g., tileset, tower slot image)
            Image baseImage = getBaseImageForType(type);

            if (baseImage != null && !baseImage.isError()) {
                Image processedImage = null; // This will hold the final image to cache

                if (baseImage == tileset) { // Is this type derived from the main tileset?
                    Point2D coords = TILE_COORDS.get(type);
                    if (coords != null) {
                        // Slice the specific tile layer from the tileset
                        ImageView view = new ImageView(baseImage);
                        view.setViewport(new Rectangle2D(
                                coords.getX() * SOURCE_TILE_SIZE,
                                coords.getY() * SOURCE_TILE_SIZE,
                                SOURCE_TILE_SIZE,
                                SOURCE_TILE_SIZE));
                        SnapshotParameters params = new SnapshotParameters();
                        params.setFill(Color.TRANSPARENT);
                        Image tileLayer = view.snapshot(params, null);

                        // Check if this type is a prop that needs overlaying
                        if (isOverlayProp(type)) {
                            // Retrieve the base GRASS image from cache (must be loaded first!)
                            Image grassBase = CACHE.get(TileType.GRASS);
                            if (grassBase != null && !grassBase.isError()) {
                                // --- Use Canvas for Compositing ---
                                Canvas tempCanvas = new Canvas(RENDER_TILE_SIZE, RENDER_TILE_SIZE);
                                GraphicsContext g = tempCanvas.getGraphicsContext2D();

                                // Draw grass first
                                g.drawImage(grassBase, 0, 0, RENDER_TILE_SIZE, RENDER_TILE_SIZE);
                                // Draw the prop tile layer on top
                                g.drawImage(tileLayer, 0, 0, RENDER_TILE_SIZE, RENDER_TILE_SIZE);

                                // Snapshot the canvas to get the composited image
                                SnapshotParameters snapParams = new SnapshotParameters();
                                snapParams.setFill(Color.TRANSPARENT);
                                processedImage = tempCanvas.snapshot(snapParams, null);
                                // --- End Canvas Compositing ---

                                System.out.println("    -> Composited " + type + " onto GRASS for cache.");
                            } else {
                                // Fallback if grass isn't cached (should not happen ideally)
                                System.err
                                        .println("    -> ERROR: GRASS tile not found in cache for compositing " + type);
                                processedImage = tileLayer; // Use the sliced layer as fallback
                            }
                        } else {
                            // Not an overlay prop, use the sliced tile directly
                            processedImage = tileLayer;
                        }
                    }
                } else {
                    // For special images like TOWER_SLOT (no longer castle), use as is
                    processedImage = baseImage;
                }

                // Cache the final processed image (either sliced or composited)
                if (processedImage != null) {
                    CACHE.put(type, processedImage);
                    image = processedImage; // Assign to the instance field
                } else {
                    System.err.println("    -> Failed to process/slice image for type: " + type);
                }
            } else {
                System.err.println("    -> Base image is null or in error for type: " + type);
            }
        }
    }

    private static synchronized void loadImagesIfNeeded() {
        if (!isFxAvailable || imagesLoaded) {
            return;
        }
        System.out.println("--> Entering loadImagesIfNeeded...");
        try {
            System.out.println("    Loading tileset...");
            // Make sure we use the tileset with the character, castle and other elements
            tileset = loadPNG("/Asset_pack/Tiles/Tileset 64x64.png");
            if (tileset == null) {
                System.err.println("    -> Tileset load returned NULL, trying alternative tileset");
                // Try loading from the other tilesets as backup
                tileset = loadPNG("/Asset_pack/Tiles/Tileset 96x96.png");
                if (tileset == null) {
                    tileset = loadPNG("/Asset_pack/Tiles/Tileset 128x128.png");
                    if (tileset == null) {
                        System.err.println("    -> All tileset load attempts failed");
                    } else {
                        System.out.println("    -> Alternative tileset 128x128 loaded");
                    }
                } else {
                    System.out.println("    -> Alternative tileset 96x96 loaded");
                }
            } else {
                System.out.println("    -> Tileset loaded successfully. ID: "
                        + Integer.toHexString(System.identityHashCode(tileset)));
            }

            // --- Pre-cache GRASS tile ---
            if (tileset != null && !tileset.isError()) {
                System.out.println("    Pre-caching GRASS tile...");
                Point2D grassCoords = TILE_COORDS.get(TileType.GRASS);
                if (grassCoords != null) {
                    ImageView grassView = new ImageView(tileset);
                    grassView.setViewport(new Rectangle2D(
                            grassCoords.getX() * SOURCE_TILE_SIZE,
                            grassCoords.getY() * SOURCE_TILE_SIZE,
                            SOURCE_TILE_SIZE,
                            SOURCE_TILE_SIZE));
                    SnapshotParameters grassParams = new SnapshotParameters();
                    grassParams.setFill(Color.TRANSPARENT);
                    Image grassImage = grassView.snapshot(grassParams, null);
                    if (grassImage != null && !grassImage.isError()) {
                        CACHE.put(TileType.GRASS, grassImage);
                        System.out.println("    -> GRASS tile pre-cached successfully.");
                    } else {
                        System.err.println("    -> FAILED to create snapshot for GRASS tile pre-caching.");
                    }
                } else {
                    System.err.println("    -> FAILED: Coordinates for GRASS not found in TILE_COORDS.");
                }
            } else {
                System.err.println("    -> Cannot pre-cache GRASS because tileset failed to load.");
            }
            // --- End Pre-cache GRASS ---

            System.out.println("    Loading castle image...");
            castleImage = loadPNG("/Asset_pack/Towers/Castle128.png", RENDER_TILE_SIZE);
            if (castleImage == null)
                System.err.println("    -> Castle image load returned NULL");
            else
                System.out.println("    -> Castle image loaded successfully. ID: "
                        + Integer.toHexString(System.identityHashCode(castleImage)));

            // Try to load the tower slot with transparent background
            String towerSlotFilename = "TowerSlotwithoutbackground128.png";
            System.out.println("    Loading tower slot image (" + towerSlotFilename + ")...");
            towerSlotImage = loadPNG("/Asset_pack/Towers/" + towerSlotFilename, RENDER_TILE_SIZE);
            if (towerSlotImage == null) {
                System.err.println("    -> Tower slot image load returned NULL, trying alternate...");
                // Try an alternate image if available
                towerSlotImage = loadPNG("/Asset_pack/Towers/TowerSlot128.png", RENDER_TILE_SIZE);
                if (towerSlotImage == null) {
                    System.err.println("    -> Alternate tower slot image also failed");
                } else {
                    System.out.println("    -> Alternate tower slot image loaded successfully");
                }
            } else {
                System.out.println("    -> Tower slot image loaded successfully. ID: "
                        + Integer.toHexString(System.identityHashCode(towerSlotImage)));
            }

            System.out.println("--> Base Tile images loading process finished.");
            imagesLoaded = true;

        } catch (Exception ex) {
            System.err.println("‼‼ UNCAUGHT EXCEPTION in loadImagesIfNeeded: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            System.out.println("<-- Exiting loadImagesIfNeeded. imagesLoaded = " + imagesLoaded);
        }
    }

    /* ─────────────────── Static utility methods ─────────────────── */

    private static Image loadPNG(String classpath, int target) {
        InputStream in = Tile.class.getResourceAsStream(classpath);
        if (in == null) {
            System.err.println("      -> PNG stream NULL for: " + classpath);
            return null;
        }
        try {
            Image img = new Image(in, target, target, true, true);
            if (img.isError()) {
                System.err.println("      -> Failed to load image: " + classpath);
                return null;
            }
            return img;
        } catch (Exception e) {
            System.err.println("      -> Exception loading image: " + e.getMessage());
            return null;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                System.err.println("      -> Exception closing stream: " + e.getMessage());
            }
        }
    }

    private static Image loadPNG(String classpath) {
        return loadPNG(classpath, 0); // 0 = use native size
    }

    @Override
    public String toString() {
        return "Tile[x=" + x + ", y=" + y + ", type=" + type + "]";
    }

    /**
     * Called after deserialization to reinitialize transient fields.
     * This ensures images are properly reloaded when the game is loaded from a
     * saved file.
     */
    public void reinitializeAfterLoad() {
        loadImagesIfNeeded();
        initTransientFields();
    }
}
