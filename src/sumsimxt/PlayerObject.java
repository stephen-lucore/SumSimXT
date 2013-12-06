/**
 * Stephen LuCore and Chad Stapes
 * Intro to Software Design: Group Project
 * 12-2-13
 */
package sumsimxt;

import java.awt.Dimension;
import java.awt.Point;

/**
 *
 * @author slucore
 */
public class PlayerObject extends GameObject {
    
    private int totalHP, currentHP;
    private double xVelocity, yVelocity;
    private double horizontalAcceleration = 100;
    private double maxVelocityX = 500;
    private double horizontalDecelerationFactor = horizontalAcceleration / 8;
    private double shotCooldown = 0.5;    // seconds
    private double lastShotTime = 0;
    private double cooldownCounter = 0;
    private double shotVelocity = 200;
    
    public PlayerObject(Sprite sprite, Point point, Dimension size, int totalHP) {
        super(sprite, point, size);
        this.totalHP = totalHP;
        this.currentHP = totalHP;
    }
    
    public void takeDamage(int dmg) {
        currentHP -= dmg;
        if (currentHP <= 0) {
            this.die();
        }
    }
    
    public void setTotalHP(int hp) {
        totalHP = hp;
    }    
    public void setCurrentHP(int hp) {
        currentHP = hp;
    }
    public int getTotalHP() {
        return totalHP;
    }
    public int getCurrentHP() {
        return currentHP;
    }
    
    public void move(long millis) {
        double seconds = millis / 1000.0;
        moveX(xVelocity * seconds);
        moveY(yVelocity * seconds);
        cooldownCounter += seconds;
    }
    
    public void startShotCooldown() {
        lastShotTime = cooldownCounter;
    }
    
    public boolean shotCooled() {
        if (cooldownCounter > lastShotTime + shotCooldown) {
            return true;
        }
        return false;
    }
    
    public double getHorizontalAcceleration() {
        return horizontalAcceleration;
    }
    
    public double getHorizontalDecelerationFactor() {
        return horizontalDecelerationFactor;
    }
    
    public void setXVelocity(double set) {
        xVelocity = set;
        if (xVelocity > maxVelocityX) {
            xVelocity = maxVelocityX;
        } else if (xVelocity < -maxVelocityX) {
            xVelocity = -maxVelocityX;
        }
    }
    public void setYVelocity(double set) {
        yVelocity = set;
    }
    
    public void decelerateX(double amount) {
        if (xVelocity > 0) {
            xVelocity = (xVelocity - amount >= 0) ? xVelocity - amount : 0;
        } else if (xVelocity < 0) {
            xVelocity = (xVelocity + amount <= 0) ? xVelocity + amount : 0;
        }
    }
    
    public double getShotVelocity() {
        return shotVelocity;
    }
    
    public double getXVelocity() {
        return xVelocity;
    }
    public double getYVelocity() {
        return yVelocity;
    }
    
}