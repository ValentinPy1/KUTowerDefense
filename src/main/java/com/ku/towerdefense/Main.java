package com.ku.towerdefense;

import com.ku.towerdefense.ui.MainMenuScreen;
import com.ku.towerdefense.ui.UIAssets;
import javafx.application.Application;
import javafx.scene.ImageCursor;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import java.net.URL;

/**
 * Main entry point for the KU Tower Defense game.
 */
public class Main extends Application {

    private static MediaPlayer backgroundMusicPlayer;

    @Override
    public void start(Stage primaryStage) {
        // Initialize UI assets
        UIAssets.initialize();
        startBackgroundMusic();

        // Set up the primary stage
        primaryStage.setTitle("KU Tower Defense");
        primaryStage.setFullScreen(true);
        primaryStage.setFullScreenExitHint("");

        // Create and show main menu
        MainMenuScreen mainMenu = new MainMenuScreen(primaryStage);
        Scene scene = new Scene(mainMenu);

        // Add CSS if needed
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        // Set custom cursor if available
        ImageCursor customCursor = UIAssets.getCustomCursor();
        if (customCursor != null) {
            scene.setCursor(customCursor);
        }

        primaryStage.setScene(scene);
        
        // Enforce custom cursor throughout the application
        UIAssets.enforceCustomCursor(scene);
        UIAssets.startCursorEnforcement(scene);
        
        primaryStage.show();
    }

    public static void startBackgroundMusic() {
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.stop();
            backgroundMusicPlayer.dispose();
        }
        try {
            URL musicFileUrl = Main.class.getResource("/Asset_pack/Musics/Yeah.mp3");
            if (musicFileUrl == null) {
                System.err.println("Background music file not found: /Asset_pack/Musics/Yeah.mp3");
                return;
            }
            Media media = new Media(musicFileUrl.toExternalForm());
            backgroundMusicPlayer = new MediaPlayer(media);
            backgroundMusicPlayer.setOnEndOfMedia(() -> backgroundMusicPlayer.seek(Duration.ZERO));
            backgroundMusicPlayer.setVolume(0.5);
            backgroundMusicPlayer.play();
            System.out.println("Background music started: Yeah.mp3");
        } catch (Exception e) {
            System.err.println("Error playing background music: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void stopBackgroundMusic() {
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.stop();
            backgroundMusicPlayer.dispose();
            backgroundMusicPlayer = null;
            System.out.println("Background music stopped.");
        }
    }
    
    /**
     * Switch to a different background music track.
     * 
     * @param fileName the name of the music file (e.g., "Yeah.mp3")
     */
    public static void switchBackgroundMusic(String fileName) {
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.stop();
            backgroundMusicPlayer.dispose();
        }
        
        try {
            URL musicFileUrl = Main.class.getResource("/Asset_pack/Musics/" + fileName);
            if (musicFileUrl == null) {
                System.err.println("Music file not found: /Asset_pack/Musics/" + fileName);
                return;
            }
            
            Media media = new Media(musicFileUrl.toExternalForm());
            backgroundMusicPlayer = new MediaPlayer(media);
            backgroundMusicPlayer.setOnEndOfMedia(() -> backgroundMusicPlayer.seek(Duration.ZERO));
            backgroundMusicPlayer.setVolume(0.5);
            backgroundMusicPlayer.play();
            System.out.println("Background music switched to: " + fileName);
        } catch (Exception e) {
            System.err.println("Error switching background music to " + fileName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        stopBackgroundMusic();
        super.stop();
    }

    /**
     * Main method that launches the application.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}