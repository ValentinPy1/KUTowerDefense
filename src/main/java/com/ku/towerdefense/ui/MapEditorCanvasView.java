package com.ku.towerdefense.ui;

import com.ku.towerdefense.model.map.GameMap;
import com.ku.towerdefense.model.map.Tile;
import com.ku.towerdefense.model.map.TileType;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent; // Import MouseEvent
import javafx.scene.input.MouseButton; // Import MouseButton
import javafx.scene.input.ScrollEvent; // Import ScrollEvent
import javafx.scene.Cursor; // Import Cursor
import javafx.scene.ImageCursor; // Import ImageCursor
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority; // Import Priority
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.scene.image.Image;

/**
 * Manages the map canvas, rendering, zoom, and placement logic for the Map
 * Editor.
 */
public class MapEditorCanvasView extends VBox {

    private GameMap gameMap;
    private final Canvas mapCanvas;
    private final Label zoomLabel;
    private final MapEditorTilePalette tilePalette; // Need access to selected tile

    private int tileSize = 64; // Keep tileSize management here
    private double zoomLevel = 1.0;
    private static final double MIN_ZOOM = 0.25; // Adjusted min zoom
    private static final double MAX_ZOOM = 3.0; // Adjusted max zoom
    private static final double ZOOM_STEP = 0.1;
    private static final double VIEW_MARGIN_FACTOR = 0.9; // e.g., map uses 90% of view

    private double viewOffsetX = 0; // Panning offset X
    private double viewOffsetY = 0; // Panning offset Y

    // Keep track of the current click handler mode
    private enum ClickMode {
        PALETTE, SET_START
    }

    private ClickMode currentClickMode = ClickMode.PALETTE;

    private double lastPanX, lastPanY;
    private boolean isPanning = false;

    public MapEditorCanvasView(GameMap initialMap, MapEditorTilePalette palette) {
        super(10); // Spacing for VBox
        this.gameMap = initialMap;
        this.tilePalette = palette;

        setAlignment(Pos.CENTER);

        // Create zoom controls
        HBox zoomControls = createZoomControls();
        this.zoomLabel = (Label) zoomControls.getChildren().get(1); // Store label reference

        // Create canvas and group
        mapCanvas = new Canvas(800, 600); // Set initial size to prevent zero-size issues
        
        // Set custom cursor on canvas initialization
        ImageCursor customCursor = UIAssets.getCustomCursor();
        if (customCursor != null) {
            mapCanvas.setCursor(customCursor);
        }
        
        // Make mapCanvas fill available space in VBox with better binding
        VBox.setVgrow(mapCanvas, Priority.ALWAYS);
        
        // Simplified binding approach - bind to parent size with proper calculations
        this.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 100) { // Avoid tiny sizes
                mapCanvas.setWidth(newVal.doubleValue() - 20); // Account for padding
                centerAndZoomMap(false); // Recenter when size changes
            }
        });
        
        this.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 100) { // Avoid tiny sizes
                double availableHeight = newVal.doubleValue() - zoomControls.getHeight() - 40; // Account for controls and padding
                if (availableHeight > 100) {
                    mapCanvas.setHeight(availableHeight);
                    centerAndZoomMap(false); // Recenter when size changes
                }
            }
        });

        // Add scroll event for zooming directly to mapCanvas
        mapCanvas.setOnScroll(event -> {
            double scrollDelta = event.getDeltaY();
            double oldZoom = zoomLevel;

            if (scrollDelta > 0) {
                zoomLevel = Math.min(MAX_ZOOM, zoomLevel + ZOOM_STEP);
            } else if (scrollDelta < 0) {
                zoomLevel = Math.max(MIN_ZOOM, zoomLevel - ZOOM_STEP);
            }

            if (oldZoom != zoomLevel) {
                // Get mouse position relative to canvas
                double mouseX = event.getX();
                double mouseY = event.getY();

                // Adjust offsets to zoom towards/away from mouse pointer
                // viewOffsetX_after = cursorX - (cursorX - viewOffsetX_before) *
                // (zoomLevel_after / zoomLevel_before);
                viewOffsetX = mouseX - (mouseX - viewOffsetX) * (zoomLevel / oldZoom);
                viewOffsetY = mouseY - (mouseY - viewOffsetY) * (zoomLevel / oldZoom);

                applyZoom(); // This will call renderMap
            }
            event.consume();
        });

        // Add canvas directly, after zoom controls
        getChildren().addAll(zoomControls, mapCanvas);

        // Initial setup
        setupMouseHandlers();
        
        // Ensure initial render happens when the view is shown
        javafx.application.Platform.runLater(() -> {
            centerAndZoomMap(true);
        });
    }

    /**
     * Sets a new GameMap instance for the canvas view.
     * Triggers a re-render.
     * 
     * @param newMap The new GameMap object to display.
     */
    public void setGameMap(GameMap newMap) {
        System.out.println("--- CanvasView.setGameMap() called ---");
        this.gameMap = newMap;
        // Reset view offsets and zoom for new map to sensible defaults
        this.viewOffsetX = 0;
        this.viewOffsetY = 0;
        // this.zoomLevel = 1.0; // Or keep current zoom? Let's keep it for now.
        updateCanvasSize(); // This will call renderMap()
        System.out.println("--- CanvasView.setGameMap() finished ---");
    }

    /**
     * Sets the canvas size when the map is resized. (Now canvas binds to parent)
     */
    public void updateCanvasSize() {
        // renderMap(); // renderMap will be called by centerAndZoomMap if needed, or if
        // view changes
        // If the map content (tile count) changes, it might need recentering.
        if (mapCanvas.getWidth() > 0 && mapCanvas.getHeight() > 0) {
            centerAndZoomMap(true); // Recenter/refit when underlying map (not canvas) changes size
        }
    }

    // --- Zoom Logic ---

    private HBox createZoomControls() {
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER);
        Button zoomOutButton = new Button("-");
        zoomOutButton.setOnAction(e -> zoomChange(-ZOOM_STEP));
        Label label = new Label(); // Temporary label, stored in field
        label.setTextFill(Color.WHITE);
        Button zoomInButton = new Button("+");
        zoomInButton.setOnAction(e -> zoomChange(ZOOM_STEP));
        Button resetZoomButton = new Button("Reset Zoom");
        resetZoomButton.setOnAction(e -> {
            zoomLevel = 1.0;
            applyZoom();
        });
        controls.getChildren().addAll(zoomOutButton, label, zoomInButton, resetZoomButton);
        return controls;
    }

    private void zoomChange(double delta) {
        double oldZoom = zoomLevel;
        double newZoom = zoomLevel + delta;
        if (newZoom >= MIN_ZOOM && newZoom <= MAX_ZOOM) {
            zoomLevel = newZoom;

            // If called from buttons, zoom around center of current view
            // This requires knowing the current center of the canvas view
            double centerX = mapCanvas.getWidth() / 2;
            double centerY = mapCanvas.getHeight() / 2;

            viewOffsetX = centerX - (centerX - viewOffsetX) * (zoomLevel / oldZoom);
            viewOffsetY = centerY - (centerY - viewOffsetY) * (zoomLevel / oldZoom);

            applyZoom();
        }
    }

    private void applyZoom() {
        zoomLabel.setText(String.format("Zoom: %.0f%%", zoomLevel * 100));
        renderMap(); // Re-render with new zoomLevel used in gc.scale()
    }

    private void centerAndZoomMap(boolean fitToView) {
        if (gameMap == null || mapCanvas.getWidth() <= 0 || mapCanvas.getHeight() <= 0) {
            return;
        }

        double canvasWidth = mapCanvas.getWidth();
        double canvasHeight = mapCanvas.getHeight();

        double mapPixelWidth = gameMap.getWidth() * tileSize;
        double mapPixelHeight = gameMap.getHeight() * tileSize;

        if (fitToView) {
            if (mapPixelWidth == 0 || mapPixelHeight == 0) {
                zoomLevel = 1.0; // Default zoom if map is empty
            } else {
                double zoomX = (canvasWidth * VIEW_MARGIN_FACTOR) / mapPixelWidth;
                double zoomY = (canvasHeight * VIEW_MARGIN_FACTOR) / mapPixelHeight;
                zoomLevel = Math.min(zoomX, zoomY); // Fit by the more constrained dimension
            }
            // Clamp zoom level to defined min/max
            zoomLevel = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoomLevel));
        }

        // Calculate offsets to center the map
        viewOffsetX = (canvasWidth - (mapPixelWidth * zoomLevel)) / 2.0;
        viewOffsetY = (canvasHeight - (mapPixelHeight * zoomLevel)) / 2.0;

        System.out.println(
                String.format("Centering: Canvas(%.1f, %.1f), MapPixels(%.1f, %.1f), Zoom: %.2f, Offset(%.1f, %.1f)",
                        canvasWidth, canvasHeight, mapPixelWidth, mapPixelHeight, zoomLevel, viewOffsetX, viewOffsetY));

        applyZoom(); // This updates the label and calls renderMap()
    }

    // --- Rendering ---

    public void renderMap() {
        if (gameMap == null || mapCanvas.getWidth() <= 0 || mapCanvas.getHeight() <= 0)
            return;

        GraphicsContext gc = mapCanvas.getGraphicsContext2D();

        // Clear the entire visible canvas
        gc.setFill(Color.web("#64B464")); // Set desired background color for map area
        gc.fillRect(0, 0, mapCanvas.getWidth(), mapCanvas.getHeight());

        gc.save(); // Save default state

        // Apply transformations: pan then zoom
        gc.translate(viewOffsetX, viewOffsetY);
        gc.scale(zoomLevel, zoomLevel);

        // Draw the map tiles
        int mapWidth = gameMap.getWidth();
        int mapHeight = gameMap.getHeight();
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                Tile tile = gameMap.getTile(x, y);
                if (tile != null) {
                    // Render tile using its map coordinates (x*tileSize, y*tileSize)
                    // The GC transform handles on-screen position and size
                    tile.render(gc, x, y, tileSize, true);
                }
            }
        }

        // Add visual indicators for START_POINT and END_POINT (using transformed GC)
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                Tile tile = gameMap.getTile(x, y);
                if (tile != null) {
                    if (tile.getType() == TileType.START_POINT) {
                        // Subtle green border indicator for START_POINT
                        gc.setStroke(Color.GREEN);
                        gc.setLineWidth(2);
                        gc.strokeRect(x * tileSize + 2, y * tileSize + 2, tileSize - 4, tileSize - 4);

                        // Draw direction arrow based on position (but smaller and more subtle)
                        drawDirectionArrow(gc, x, y, mapWidth, mapHeight);
                    } else if (tile.getType() == TileType.END_POINT) {
                        // Subtle red diamond indicator for END_POINT
                        double midX = x * tileSize + tileSize / 2;
                        double midY = y * tileSize + tileSize / 2;
                        double size = tileSize / 4;

                        gc.setStroke(Color.RED);
                        gc.setLineWidth(2);

                        // Draw a diamond
                        gc.beginPath();
                        gc.moveTo(midX, midY - size); // Top
                        gc.lineTo(midX + size, midY); // Right
                        gc.lineTo(midX, midY + size); // Bottom
                        gc.lineTo(midX - size, midY); // Left
                        gc.closePath();
                        gc.stroke();
                    }
                }
            }
        }

        // Draw grid lines (using transformed GC)
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(0.5 / zoomLevel); // Keep grid lines thin regardless of zoom
        // Calculate visible map range in map pixel coordinates to optimize grid drawing
        double renderAreaX1 = (0 - viewOffsetX) / zoomLevel;
        double renderAreaY1 = (0 - viewOffsetY) / zoomLevel;
        double renderAreaX2 = (mapCanvas.getWidth() - viewOffsetX) / zoomLevel;
        double renderAreaY2 = (mapCanvas.getHeight() - viewOffsetY) / zoomLevel;

        int startGridX = Math.max(0, (int) (renderAreaX1 / tileSize) - 1);
        int endGridX = Math.min(mapWidth, (int) (renderAreaX2 / tileSize) + 1);
        int startGridY = Math.max(0, (int) (renderAreaY1 / tileSize) - 1);
        int endGridY = Math.min(mapHeight, (int) (renderAreaY2 / tileSize) + 1);

        for (int x = startGridX; x <= endGridX; x++) {
            gc.strokeLine(x * tileSize, startGridY * tileSize, x * tileSize, endGridY * tileSize);
        }
        for (int y = startGridY; y <= endGridY; y++) {
            gc.strokeLine(startGridX * tileSize, y * tileSize, endGridX * tileSize, y * tileSize);
        }

        gc.restore(); // Restore to default state (no translation/scale)
    }

    /**
     * Draw a directional arrow from the START_POINT toward the map center
     */
    private void drawDirectionArrow(GraphicsContext gc, int x, int y, int mapWidth, int mapHeight) {
        double centerX = x * tileSize + tileSize / 2;
        double centerY = y * tileSize + tileSize / 2;
        double arrowLength = tileSize / 4; // Make arrow shorter

        // Determine arrow direction based on edge position
        double dirX = 0, dirY = 0;

        if (x == 0)
            dirX = 1;
        else if (x == mapWidth - 1)
            dirX = -1;

        if (y == 0)
            dirY = 1;
        else if (y == mapHeight - 1)
            dirY = -1;

        // If at a corner, point diagonally inward
        if (dirX != 0 && dirY != 0) {
            double length = Math.sqrt(dirX * dirX + dirY * dirY);
            dirX /= length;
            dirY /= length;
        }

        // Calculate arrow end points
        double endX = centerX + dirX * arrowLength;
        double endY = centerY + dirY * arrowLength;

        // Draw arrow line
        gc.setStroke(Color.GREEN);
        gc.setLineWidth(1.5); // Thinner line
        gc.strokeLine(centerX, centerY, endX, endY);

        // Draw arrowhead
        double headSize = tileSize / 8; // Smaller arrowhead
        double angle = Math.atan2(dirY, dirX);
        double angle1 = angle - Math.PI / 4;
        double angle2 = angle + Math.PI / 4;

        double head1X = endX - headSize * Math.cos(angle1);
        double head1Y = endY - headSize * Math.sin(angle1);
        double head2X = endX - headSize * Math.cos(angle2);
        double head2Y = endY - headSize * Math.sin(angle2);

        gc.strokeLine(endX, endY, head1X, head1Y);
        gc.strokeLine(endX, endY, head2X, head2Y);
    }

    // --- Placement Logic ---

    public void activateSetStartMode() {
        currentClickMode = ClickMode.SET_START;
        if (tilePalette.getToggleGroup().getSelectedToggle() != null)
            tilePalette.getToggleGroup().getSelectedToggle().setSelected(false);
        setupMouseHandlers();
    }

    // Reset placement mode back to using the palette selection
    // Made public to be called after loading a map
    public void resetPlacementMode() {
        currentClickMode = ClickMode.PALETTE;
        setupMouseHandlers();
        TileType currentSelection = tilePalette.getSelectedTileType();
        System.out.println("CanvasView: Placement mode reset to palette selection. Current: " + currentSelection);
    }

    private void setupMouseHandlers() {
        mapCanvas.setOnMousePressed(this::handleMousePressForPanning);
        mapCanvas.setOnMouseDragged(this::handleMouseDragForPanning);
        mapCanvas.setOnMouseReleased(this::handleMouseReleaseForPanning);
        mapCanvas.addEventFilter(MouseEvent.MOUSE_CLICKED, this::handleCanvasClick);
    }

    private void handleMousePressForPanning(MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY) {
            // Right mouse button starts panning
            isPanning = true;
            lastPanX = event.getX();
            lastPanY = event.getY();
            
            // Use custom cursor for panning instead of system MOVE cursor
            ImageCursor customCursor = UIAssets.getCustomCursor();
            if (customCursor != null) {
                mapCanvas.setCursor(customCursor);
            } else {
                mapCanvas.setCursor(Cursor.MOVE); // Fallback only if custom cursor unavailable
            }
            
            event.consume();
        }
    }

    private void handleMouseDragForPanning(MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY && isPanning) {
            double deltaX = event.getX() - lastPanX;
            double deltaY = event.getY() - lastPanY;

            viewOffsetX += deltaX;
            viewOffsetY += deltaY;

            lastPanX = event.getX();
            lastPanY = event.getY();

            renderMap();
            event.consume();
        }
    }

    private void handleMouseReleaseForPanning(MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY && isPanning) {
            isPanning = false;
            
            // Restore custom cursor after panning instead of DEFAULT cursor
            ImageCursor customCursor = UIAssets.getCustomCursor();
            if (customCursor != null) {
                mapCanvas.setCursor(customCursor);
            } else {
                mapCanvas.setCursor(Cursor.DEFAULT); // Fallback only if custom cursor unavailable
            }
            
            event.consume();
        }
    }

    // Single click handler routes based on mode
    private void handleCanvasClick(MouseEvent e) {
        if (isPanning) {
            // If a pan gesture just occurred, the MOUSE_RELEASED handler should have set
            // isPanning to false and consumed.
            // This is an additional check. If event was consumed by release handler, this
            // click might not even fire.
            // If it does fire, and isPanning is somehow still true, we prevent action.
            e.consume(); // Consume to prevent tile placement if click occurs after pan
            return;
        }

        // Convert mouse click coordinates (relative to Canvas node) to grid coordinates
        double mouseX = e.getX();
        double mouseY = e.getY();

        // Reverse the transformations:
        // 1. Reverse translation: (mouseX - viewOffsetX), (mouseY - viewOffsetY)
        // 2. Reverse scaling: Divide by zoomLevel
        double mapPixelX = (mouseX - viewOffsetX) / zoomLevel;
        double mapPixelY = (mouseY - viewOffsetY) / zoomLevel;

        int gridX = (int) (mapPixelX / tileSize);
        int gridY = (int) (mapPixelY / tileSize);

        if (currentClickMode == ClickMode.SET_START) {
            handleSpecialPlacementClick(e, TileType.START_POINT);
            resetPlacementMode(); // Automatically switch back after attempting placement
        } else {
            handlePaletteClick(e);
        }
    }

    // Handles clicks when in standard palette mode
    private void handlePaletteClick(MouseEvent e) {
        // This method is now part of handleCanvasClick's "else" branch.
        // The coordinate conversion is done at the start of handleCanvasClick.
        // So, gridX and gridY are already calculated based on the new transformation
        // logic.

        // We need to extract gridX and gridY from the event, or pass them.
        // For now, let's re-calculate them here for clarity, but ideally, they are
        // passed
        // or handleCanvasClick becomes the single source of truth for this.

        double mouseX = e.getX();
        double mouseY = e.getY();
        double mapPixelX = (mouseX - viewOffsetX) / zoomLevel;
        double mapPixelY = (mouseY - viewOffsetY) / zoomLevel;
        int gridX = (int) (mapPixelX / tileSize);
        int gridY = (int) (mapPixelY / tileSize);

        // Boundary check
        if (gridX < 0 || gridX >= gameMap.getWidth() || gridY < 0 || gridY >= gameMap.getHeight()) {
            return;
        }

        if (e.getButton() == MouseButton.SECONDARY) { // Handle Right-click
            System.out.println("Right-click detected at (" + gridX + "," + gridY + "). Placing GRASS.");
            gameMap.setTileType(gridX, gridY, TileType.GRASS);
            renderMap();
            e.consume(); // Consume the event so it's not processed further
            return;
        }

        // --- Existing Left-click logic (Palette Tile Placement) ---
        TileType selectedType = tilePalette.getSelectedTileType();
        if (selectedType == null)
            return; // Nothing selected
        if (gridX < 0 || gridX >= gameMap.getWidth() || gridY < 0 || gridY >= gameMap.getHeight())
            return; // Out of bounds

        if (selectedType == TileType.CASTLE1) {
            handleCastlePlacement(gridX, gridY);
        } else if (selectedType == TileType.START_POINT) {
            handleStartPointPlacement(gridX, gridY);
        } else if (selectedType == TileType.END_POINT) {
            handleEndPointPlacement(gridX, gridY);
        } else {
            handleSingleTilePlacement(gridX, gridY, selectedType);
        }
        renderMap();
    }

    // Handles clicks ONLY when in Set Start mode
    private void handleSpecialPlacementClick(MouseEvent e, TileType typeToPlace) {
        if (typeToPlace != TileType.START_POINT) {
            System.err.println("Error: handleSpecialPlacementClick called for non-START_POINT");
            return;
        }

        double mouseX = e.getX();
        double mouseY = e.getY();
        double mapPixelX = (mouseX - viewOffsetX) / zoomLevel;
        double mapPixelY = (mouseY - viewOffsetY) / zoomLevel;
        int x = (int) (mapPixelX / tileSize);
        int y = (int) (mapPixelY / tileSize);

        if (x < 0 || x >= gameMap.getWidth() || y < 0 || y >= gameMap.getHeight()) {
            showAlert("Invalid Placement", "Placement out of bounds.");
            return;
        }
        boolean onEdge = (x == 0 || x == gameMap.getWidth() - 1 || y == 0 || y == gameMap.getHeight() - 1);
        if (!onEdge) {
            showAlert("Invalid Placement", "Start Point must be placed on the edge.");
            return;
        }
        Tile targetTile = gameMap.getTile(x, y);
        if (targetTile != null && targetTile.getType() != TileType.GRASS && !targetTile.isWalkable()) {
            if (targetTile.getType() == TileType.END_POINT || targetTile.getType() == TileType.CASTLE1
                    || /* ... */ targetTile.getType() == TileType.CASTLE4) {
                showAlert("Invalid Placement", "Start Point cannot overlap the Castle/End Point.");
            } else {
                showAlert("Invalid Placement", "Start Point can only be placed on Grass or Path tiles on the edge.");
            }
            return;
        }

        clearExistingStartPoint();
        gameMap.setTileType(x, y, TileType.START_POINT);
        System.out.println("Placed Start Point at (" + x + "," + y + ")");
        renderMap();
    }

    private void handleSingleTilePlacement(int x, int y, TileType typeToPlace) {
        if (typeToPlace == TileType.START_POINT || typeToPlace == TileType.END_POINT ||
                typeToPlace == TileType.CASTLE1 || typeToPlace == TileType.CASTLE2 ||
                typeToPlace == TileType.CASTLE3 || typeToPlace == TileType.CASTLE4) {
            return; // Should not be placed via this method
        }

        Tile existingTile = gameMap.getTile(x, y);
        if (existingTile != null) {
            TileType existingType = existingTile.getType();
            if (existingType == TileType.START_POINT || existingType == TileType.END_POINT ||
                    existingType == TileType.CASTLE1 || existingType == TileType.CASTLE2 ||
                    existingType == TileType.CASTLE3 || existingType == TileType.CASTLE4) {
                if (typeToPlace != TileType.GRASS) {
                    showAlert("Action Needed", "Cannot overwrite " + existingType + ". Place GRASS first.");
                    return;
                } else {
                    if (existingType == TileType.START_POINT)
                        clearExistingStartPoint();
                    if (existingType == TileType.END_POINT || existingType == TileType.CASTLE1 ||
                            existingType == TileType.CASTLE2 || existingType == TileType.CASTLE3 ||
                            existingType == TileType.CASTLE4) {
                        clearExistingEndPoint();
                        clearCastleRemnants(x, y, existingType); // Still need remnant clearing
                    }
                }
            }
        }
        gameMap.setTileType(x, y, typeToPlace);
        System.out.println("Placed single tile: " + typeToPlace + " at (" + x + "," + y + ")");
    }

    private void handleCastlePlacement(int x, int y) {
        // Check if 2x2 structure fits
        if (x + 1 >= gameMap.getWidth() || y + 1 >= gameMap.getHeight()) {
            showAlert("Invalid Castle Placement", 
                "Castle doesn't fit on map!\n\n" +
                "❌ Castle structure extends beyond map boundaries at position (" + (x+1) + "," + (y+1) + ")\n" +
                "✅ Solution: Move the Castle away from the map edges\n\n" +
                "💡 Tip: Castles need a 2x2 area - leave space from map borders");
            return;
        }

        // Check if all 4 tiles are grass
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                Tile tile = gameMap.getTile(x + i, y + j);
                if (tile == null || tile.getType() != TileType.GRASS) {
                    showAlert("Invalid Castle Placement", 
                        "Castle needs clear grass area!\n\n" +
                        "❌ Castle requires all 4 tiles to be grass at position (" + (x+1) + "," + (y+1) + ")\n" +
                        "✅ Solution: Clear the 2x2 area by placing grass tiles first\n\n" +
                        "💡 Tip: Select 'Grass' from palette and clear the area before placing Castle");
                    return;
                }
            }
        }

        // Check if castle right side has adjacent path tiles
        // Castle structure: END_POINT at (x,y), right side at (x+1, y)
        int castleRightX = x + 1;
        int castleRightY = y;
        boolean hasAdjacentPath = false;
        
        // Check tiles adjacent to castle right side specifically
        int[][] rightSideDirections = { 
            { castleRightX - 1, castleRightY },     // Left of right side (back toward castle center)
            { castleRightX + 1, castleRightY },     // Right of right side (outside castle)
            { castleRightX, castleRightY - 1 },     // Above right side
            { castleRightX, castleRightY + 1 }      // Below right side
        };
        
        for (int[] pos : rightSideDirections) {
            int nx = pos[0];
            int ny = pos[1];
            if (nx >= 0 && nx < gameMap.getWidth() && ny >= 0 && ny < gameMap.getHeight()) {
                Tile tile = gameMap.getTile(nx, ny);
                if (tile != null && tile.isWalkable()) {
                    hasAdjacentPath = true;
                    break;
                }
            }
        }

        if (!hasAdjacentPath) {
            showAlert("Invalid Castle Placement", 
                "Castle needs adjacent path at entrance!\n\n" +
                "❌ No path tiles found adjacent to Castle right side (entrance) at position (" + (castleRightX+1) + "," + (castleRightY+1) + ")\n" +
                "✅ Solution: Place path tiles next to the Castle's right side\n\n" +
                "💡 Tip: Enemies enter the Castle from the right side - ensure there's a path connection there");
            return;
        }

        // Clear any existing castle/end point
        clearExistingEndPoint();

        // Place the 2x2 castle with the END_POINT at the bottom-left
        gameMap.setTileType(x, y, TileType.END_POINT); // Bottom-left is END_POINT
        gameMap.setTileType(x + 1, y, TileType.CASTLE2);
        gameMap.setTileType(x, y + 1, TileType.CASTLE3);
        gameMap.setTileType(x + 1, y + 1, TileType.CASTLE4);

        System.out.println("Castle placed with END_POINT at (" + x + "," + y + ")");
        renderMap();

        // Regenerate the path
        gameMap.generatePath();
    }

    /**
     * Handle the placement of a START_POINT tile
     */
    private void handleStartPointPlacement(int x, int y) {
        // Validate edge placement
        boolean onEdge = (x == 0 || x == gameMap.getWidth() - 1 || y == 0 || y == gameMap.getHeight() - 1);
        if (!onEdge) {
            showAlert("Invalid Start Point Placement", 
                "Start Point must be at map edge!\n\n" +
                "❌ Cannot place Start Point at position (" + (x+1) + "," + (y+1) + ") - it's in the center\n" +
                "✅ Solution: Place the Start Point on any edge tile of the map\n\n" +
                "💡 Tip: Enemies spawn at the Start Point and need to enter from the map edge");
            return;
        }

        // Validate not on castle
        Tile targetTile = gameMap.getTile(x, y);
        if (targetTile != null && (targetTile.getType() == TileType.END_POINT ||
                targetTile.getType() == TileType.CASTLE1 ||
                targetTile.getType() == TileType.CASTLE2 ||
                targetTile.getType() == TileType.CASTLE3 ||
                targetTile.getType() == TileType.CASTLE4)) {
            showAlert("Invalid Start Point Placement", 
                "Cannot place Start Point on Castle!\n\n" +
                "❌ Start Point cannot overlap with Castle structures\n" +
                "✅ Solution: Choose a different edge location away from the Castle\n\n" +
                "💡 Tip: Start and End points must be separate for a valid path");
            return;
        }

        // Check if there's a path tile adjacent to the START_POINT
        boolean hasAdjacentPath = false;
        int[][] directions = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } }; // All four directions
        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (nx >= 0 && nx < gameMap.getWidth() && ny >= 0 && ny < gameMap.getHeight()) {
                TileType neighborType = gameMap.getTileType(nx, ny);
                if (isPathTile(neighborType)) {
                    hasAdjacentPath = true;
                    break;
                }
            }
        }

        if (!hasAdjacentPath) {
            showAlert("Invalid Start Point Placement", 
                "Start Point needs adjacent path!\n\n" +
                "❌ No path tiles found next to position (" + (x+1) + "," + (y+1) + ")\n" +
                "✅ Solution: Place path tiles adjacent to the Start Point\n\n" +
                "💡 Tip: Enemies need to immediately enter a path when they spawn");
            return;
        }

        // Clear any existing start point
        clearExistingStartPoint();

        // Place the new start point
        gameMap.setTileType(x, y, TileType.START_POINT);
        System.out.println("Placed Start Point at (" + x + "," + y + ")");

        // Regenerate the path
        gameMap.generatePath();
    }

    /**
     * Check if a tile type is a path tile
     */
    private boolean isPathTile(TileType type) {
        return type == TileType.PATH_HORIZONTAL ||
                type == TileType.PATH_VERTICAL ||
                type == TileType.PATH ||
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
                type == TileType.PATH_HORIZONTAL_E_DE;
    }

    /**
     * Handle the placement of an END_POINT tile
     */
    private void handleEndPointPlacement(int x, int y) {
        // END_POINT is always placed with a castle structure, so we just redirect to
        // castle placement
        handleCastlePlacement(x, y);
    }

    // --- Clearing Logic (Moved from MapEditorScreen) ---

    private void clearExistingStartPoint() {
        for (int x = 0; x < gameMap.getWidth(); x++) {
            for (int y = 0; y < gameMap.getHeight(); y++) {
                if (gameMap.getTileType(x, y) == TileType.START_POINT) {
                    gameMap.setTileType(x, y, TileType.GRASS);
                    System.out.println("Cleared existing Start Point at (" + x + "," + y + ")");
                    return;
                }
            }
        }
    }

    private void clearExistingEndPoint() {
        // First find the END_POINT (which serves as castle base marker)
        for (int x = 0; x < gameMap.getWidth(); x++) {
            for (int y = 0; y < gameMap.getHeight(); y++) {
                if (gameMap.getTileType(x, y) == TileType.END_POINT) {
                    System.out.println("Clearing existing Castle/END_POINT at (" + x + "," + y + ")");

                    // Clear the entire 2x2 castle structure
                    gameMap.setTileType(x, y, TileType.GRASS); // END_POINT

                    // Clear other castle parts if they exist
                    if (x + 1 < gameMap.getWidth())
                        gameMap.setTileType(x + 1, y, TileType.GRASS); // CASTLE2
                    if (y + 1 < gameMap.getHeight())
                        gameMap.setTileType(x, y + 1, TileType.GRASS); // CASTLE3
                    if (x + 1 < gameMap.getWidth() && y + 1 < gameMap.getHeight())
                        gameMap.setTileType(x + 1, y + 1, TileType.GRASS); // CASTLE4

                    return;
                }
            }
        }

        // Also check for any stray castle parts without an END_POINT
        for (int x = 0; x < gameMap.getWidth(); x++) {
            for (int y = 0; y < gameMap.getHeight(); y++) {
                TileType type = gameMap.getTileType(x, y);
                if (type == TileType.CASTLE1 || type == TileType.CASTLE2 ||
                        type == TileType.CASTLE3 || type == TileType.CASTLE4) {
                    System.out.println("Clearing stray castle part at (" + x + "," + y + ")");
                    gameMap.setTileType(x, y, TileType.GRASS);
                }
            }
        }
    }

    private void clearCastleRemnants(int x, int y, TileType overwrittenType) {
        int topLeftX = x, topLeftY = y;
        if (overwrittenType == TileType.CASTLE2)
            topLeftX = x - 1;
        else if (overwrittenType == TileType.CASTLE3)
            topLeftY = y - 1;
        else if (overwrittenType == TileType.CASTLE4) {
            topLeftX = x - 1;
            topLeftY = y - 1;
        } else if (overwrittenType == TileType.END_POINT) {
            /* Already top-left */ }

        int[] xs = { topLeftX, topLeftX + 1, topLeftX, topLeftX + 1 };
        int[] ys = { topLeftY, topLeftY, topLeftY + 1, topLeftY + 1 };

        for (int i = 0; i < 4; i++) {
            if (xs[i] == x && ys[i] == y)
                continue;
            if (xs[i] >= 0 && xs[i] < gameMap.getWidth() && ys[i] >= 0 && ys[i] < gameMap.getHeight()) {
                TileType type = gameMap.getTileType(xs[i], ys[i]);
                if (type == TileType.CASTLE1 || type == TileType.CASTLE2 ||
                        type == TileType.CASTLE3 || type == TileType.CASTLE4 ||
                        type == TileType.END_POINT) {
                    gameMap.setTileType(xs[i], ys[i], TileType.GRASS);
                }
            }
        }
        System.out.println("Cleared castle remnants around (" + x + "," + y + ")");
    }

    // Enhanced alert helper - matches MapEditorScreen behavior
    private void showAlert(String title, String content) {
        try {
            Alert.AlertType type = Alert.AlertType.INFORMATION;
            if (title.toLowerCase().contains("error") || title.toLowerCase().contains("failed")) {
                type = Alert.AlertType.ERROR;
            } else if (title.toLowerCase().contains("success") || title.toLowerCase().contains("saved")
                    || title.toLowerCase().contains("deleted")) {
                type = Alert.AlertType.INFORMATION;
            } else if (title.toLowerCase().contains("confirm")) {
                type = Alert.AlertType.CONFIRMATION;
            }

            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);

            // Apply styling to match main editor
            try {
                String cssPath = getClass().getResource("/css/style.css").toExternalForm();
                alert.getDialogPane().getStylesheets().add(cssPath);
                alert.getDialogPane().getStyleClass().add("dialog-pane");
            } catch (Exception e) {
                System.err.println("Could not load CSS for canvas dialog: " + e.getMessage());
            }

            // Add type-specific styling
            if (alert.getAlertType() == Alert.AlertType.ERROR) {
                alert.getDialogPane().getStyleClass().add("error-dialog");
            } else if (alert.getAlertType() == Alert.AlertType.CONFIRMATION) {
                alert.getDialogPane().getStyleClass().add("confirmation-dialog");
            } else if (alert.getAlertType() == Alert.AlertType.INFORMATION) {
                alert.getDialogPane().getStyleClass().add("info-dialog");
            }

            // CRITICAL: Ensure dialog shows on top WITHOUT exiting fullscreen
            // Find the primary stage by traversing up the scene graph
            javafx.stage.Stage ownerStage = null;
            try {
                javafx.stage.Window window = this.getScene().getWindow();
                if (window instanceof javafx.stage.Stage) {
                    ownerStage = (javafx.stage.Stage) window;
                }
            } catch (Exception e) {
                System.err.println("Could not find owner stage: " + e.getMessage());
            }

            if (ownerStage != null) {
                alert.initOwner(ownerStage);
            }
            alert.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            
            // Prevent fullscreen exit by keeping dialog centered and properly sized
            alert.setResizable(false);
            alert.getDialogPane().setPrefWidth(400);
            alert.getDialogPane().setMinWidth(400);
            alert.getDialogPane().setPrefHeight(200);
            
            // Show in center of parent window, not system desktop
            alert.showAndWait();
            
        } catch (Exception e) {
            // Fallback if alert fails - log to console instead of crashing
            System.err.println("Canvas Alert Error - " + title + ": " + content);
            System.err.println("Exception showing canvas alert: " + e.getMessage());
            e.printStackTrace();
            
            // Try a minimal system dialog as last resort
            try {
                Alert fallbackAlert = new Alert(Alert.AlertType.ERROR);
                fallbackAlert.setTitle("Map Editor Alert");
                fallbackAlert.setContentText(title + ": " + content);
                fallbackAlert.setResizable(false);
                fallbackAlert.showAndWait();
            } catch (Exception ex) {
                // If even that fails, just log it
                System.err.println("Complete canvas dialog failure: " + ex.getMessage());
            }
        }
    }
}