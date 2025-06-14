package com.ku.towerdefense.model.entity;

import com.ku.towerdefense.util.GameSettings;

import java.io.File;
import java.io.Serializable;
import javafx.scene.paint.Color;

/**
 * Mage tower shoots magical spells at enemies.
 * Has a balanced fire rate and damage between archer and artillery towers.
 * More effective against knights, less effective against goblins.
 */
public class MageTower extends Tower implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int BASE_COST = 75; // Added base cost
    private static final int BASE_DAMAGE = GameSettings.getInstance().getMageTowerDamage();
    private static final int BASE_RANGE = GameSettings.getInstance().getMageTowerRange();
    private static final long BASE_FIRE_RATE = GameSettings.getInstance().getMageTowerFireRate();
    private static final String BASE_IMAGE_FILENAME = "Tower_spell128.png";
    private static final String UPGRADED_IMAGE_FILENAME = "mage_up.png";

    private static final double PROJECTILE_WIDTH = 12;
    private static final double PROJECTILE_HEIGHT = 12;
    private static final double PROJECTILE_SPEED = 300;
    private static final String L1_PROJECTILE_IMAGE_FILE = "magic_bolt.png"; // Placeholder
    private static final String L2_PROJECTILE_IMAGE_FILE = "magic_bolt_level2.png"; // Placeholder

    /**
     * Create a new mage tower at the specified position.
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    public MageTower(double x, double y) {
        super(x, y, 64, 64, BASE_DAMAGE, BASE_RANGE, BASE_FIRE_RATE, BASE_COST, DamageType.MAGIC);
    }

    /**
     * Create a magical spell projectile targeting the specified enemy.
     *
     * @param target the target enemy
     * @return a spell projectile
     */
    @Override
    protected Projectile createProjectile(Enemy target) {
        double projectileX = getCenterX() - PROJECTILE_WIDTH / 2;
        double projectileY = getCenterY() - PROJECTILE_HEIGHT / 2;

        String currentProjectileImage = L1_PROJECTILE_IMAGE_FILE;
        if (this.level == 2) {
            currentProjectileImage = L2_PROJECTILE_IMAGE_FILE;
        }

        Projectile projectile = new Projectile(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT, target,
                this.damage, DamageType.MAGIC, PROJECTILE_SPEED, this);
        projectile.setImageFile(currentProjectileImage);
        projectile.setImpactEffect(Projectile.ImpactEffect.FIRE);
        
        // Set different colors based on tower level
        if (this.level == 2) {
            // Level 2: Bright blue magical energy
            projectile.setColor(Color.DODGERBLUE);
        } else {
            // Level 1: Traditional purple magic
            projectile.setColor(Color.PURPLE);
        }
        
        return projectile;
    }

    @Override
    public String getName() {
        return "Mage Tower";
    }

    @Override
    public int getBaseCost() {
        return BASE_COST;
    }

    @Override
    protected String getBaseImageName() {
        return "Asset_pack/Towers/" + BASE_IMAGE_FILENAME;
    }

    @Override
    protected String getUpgradedImageName() {
        return "Asset_pack/Towers/" + UPGRADED_IMAGE_FILENAME;
    }

    @Override
    public Tower cloneTower() {
        MageTower clone = new MageTower(this.x, this.y);
        return clone;
    }
}