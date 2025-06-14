package com.ku.towerdefense.model.entity;

import com.ku.towerdefense.util.GameSettings;

import java.io.File;
import java.io.Serializable;

/**
 * Archer tower shoots arrows at enemies.
 * Has the fastest fire rate of all tower types, but deals the least damage per
 * shot.
 * More effective against goblins, less effective against knights.
 */
public class ArcherTower extends Tower implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int BASE_COST = 50;
    private static final int BASE_DAMAGE = 20; // Assuming GameSettings would provide these, hardcoding for now
    private static final int BASE_RANGE = 150;
    private static final long BASE_FIRE_RATE = 1000; // milliseconds
    private static final String BASE_IMAGE_FILENAME = "Tower_archer128.png";
    private static final String UPGRADED_IMAGE_FILENAME = "archer_up.png";

    private static final double PROJECTILE_WIDTH = 16;
    private static final double PROJECTILE_HEIGHT = 8;
    private static final double PROJECTILE_SPEED = 450;
    private static final String PROJECTILE_IMAGE_FILE = "arrow.png"; // Example, if arrows have their own image

    /**
     * Create a new archer tower at the specified position.
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    public ArcherTower(double x, double y) {
        super(x, y, 64, 64, BASE_DAMAGE, BASE_RANGE, BASE_FIRE_RATE, BASE_COST, DamageType.ARROW); // Consistent 64x64 size for all levels
        // Stats are now initialized in the super constructor using base values.
        // Level 1 specific stats (damage, range, fireRate) are set from baseDamage,
        // baseRange, baseFireRate.
    }

    /**
     * Create an arrow projectile targeting the specified enemy.
     *
     * @param target the target enemy
     * @return an arrow projectile
     */
    @Override
    protected Projectile createProjectile(Enemy target) {
        double projectileX = getCenterX() - PROJECTILE_WIDTH / 2;
        double projectileY = getCenterY() - PROJECTILE_HEIGHT / 2;
        Projectile projectile = new Projectile(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT, target,
                this.damage, DamageType.ARROW, PROJECTILE_SPEED, this); // Added this as sourceTower
        projectile.setImageFile(PROJECTILE_IMAGE_FILE); // Set image for projectile if applicable
        projectile.setImpactEffect(Projectile.ImpactEffect.NONE);
        
        // Set a more realistic arrow color (forest green for fletching)
        projectile.setColor(javafx.scene.paint.Color.FORESTGREEN);
        
        return projectile;
    }

    @Override
    public String getName() {
        return "Archer Tower";
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
        ArcherTower clone = new ArcherTower(this.x, this.y);
        return clone;
    }

    @Override
    public boolean upgrade() {
        if (!super.upgrade()) { // This increments level and changes imageFile
            return false;
        }

        if (this.level == 2) {
            // Apply Level 2 specific stats for Archer Tower
            this.range = (int) (this.baseRange * 1.5); // 50% wider attack range
            this.fireRate = this.baseFireRate / 2; // 2x higher rate of fire
            // this.damage remains this.baseDamage as per requirements

            System.out.println(getName() + " L2 stats applied: Range=" + this.range + ", FireRate=" + this.fireRate);
        } else {
            // If somehow upgraded beyond L2 or back to L1 (if supported), reset to L1 stats
            this.damage = this.baseDamage;
            this.range = this.baseRange;
            this.fireRate = this.baseFireRate;
            System.out.println(getName() + " reverted to L1 stats.");
        }
        return true;
    }
}