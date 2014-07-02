package uk.co.jpowell.particles;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.Random;

/**
 * Created by jpowell on 30/06/2014.
 */
public class Particle {

    private static Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final double INFINITY = Double.POSITIVE_INFINITY;

    private static final int[] colors = {Color.BLUE, Color.RED, Color.GREEN};

    public double getRx() {
        return rx;
    }

    public double getRy() {
        return ry;
    }

    private double rx, ry;    // position
    private double vx, vy;    // velocity

    public double getRadius() {
        return radius;
    }

    private double radius;    // radius
    private double mass;      // mass
    private int color;      // color
    private int count;        // number of collisions so far

    private int width;
    private int height;


    // create a new particle with given parameters
    public Particle(double rx, double ry, double vx, double vy, double radius, double mass, int color) {
        this.vx = vx;
        this.vy = vy;
        this.rx = rx;
        this.ry = ry;
        this.radius = radius;
        this.mass   = mass;
        this.color  = color;
    }

    // create a random particle in the unit box (overlaps not checked)
    public Particle(int width, int height) {
        this.width = width;
        this.height = height;

        Random r = new Random();

        radius = 20*Math.random() + 10;

        rx     = (width - 2*radius) * Math.random() + radius;
        ry     = (height - 2*radius) * Math.random() + radius;
        vx     = 20 * (Math.random() - 0.5);
        vy     = 20 * (Math.random() - 0.5);

        mass   = radius*radius;
        color  = colors[r.nextInt(colors.length)];
    }

    // updates position
    public void move(double dt) {
        rx += vx * dt;
        ry += vy * dt;
    }

    // draw the particle
    public void draw(Canvas canvas) {

        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(color);
        canvas.drawCircle((float)rx, (float)ry, (float)radius,paint);
    }

    // return the number of collisions involving this particle
    public int count() { return count; }


    // how long into future until collision between this particle a and b?
    public double timeToHit(Particle b) {
        Particle a = this;
        if (a == b) return INFINITY;
        double dx  = b.rx - a.rx;
        double dy  = b.ry - a.ry;
        double dvx = b.vx - a.vx;
        double dvy = b.vy - a.vy;
        double dvdr = dx*dvx + dy*dvy;
        if (dvdr > 0) return INFINITY;
        double dvdv = dvx*dvx + dvy*dvy;
        double drdr = dx*dx + dy*dy;
        double sigma = a.radius + b.radius;
        double d = (dvdr*dvdr) - dvdv * (drdr - sigma*sigma);
        // if (drdr < sigma*sigma) StdOut.println("overlapping particles");
        if (d < 0) return INFINITY;
        return -(dvdr + Math.sqrt(d)) / dvdv;
    }

    // how long into future until this particle collides with a vertical wall?
    public double timeToHitVerticalWall() {
        if      (vx > 0) return (width - rx - radius) / vx;
        else if (vx < 0) return (radius - rx) / vx;
        else             return INFINITY;
    }

    // how long into future until this particle collides with a horizontal wall?
    public double timeToHitHorizontalWall() {
        if      (vy > 0) return (height - ry - radius) / vy;
        else if (vy < 0) return (radius - ry) / vy;
        else             return INFINITY;
    }

    public boolean overlap(Particle b) {
        Particle a = this;
        double dx  = b.rx - a.rx;
        double dy  = b.ry - a.ry;
        double drdr = dx*dx + dy*dy;
        return drdr < (Math.pow(a.radius+b.radius,2));
    }

    // update velocities upon collision between this particle and that particle
    public void bounceOff(Particle that) {
        double dx  = that.rx - this.rx;
        double dy  = that.ry - this.ry;
        double dvx = that.vx - this.vx;
        double dvy = that.vy - this.vy;
        double dvdr = dx*dvx + dy*dvy;             // dv dot dr
        double dist = this.radius + that.radius;   // distance between particle centers at collison

        // normal force F, and in x and y directions
        double F = 2 * this.mass * that.mass * dvdr / ((this.mass + that.mass) * dist);
        double fx = F * dx / dist;
        double fy = F * dy / dist;

        // update velocities according to normal force
        this.vx += fx / this.mass;
        this.vy += fy / this.mass;
        that.vx -= fx / that.mass;
        that.vy -= fy / that.mass;

        // update collision counts
        this.count++;
        that.count++;
    }

    // update velocity of this particle upon collision with a vertical wall
    public void bounceOffVerticalWall() {
        vx = -vx;
        count++;
    }

    // update velocity of this particle upon collision with a horizontal wall
    public void bounceOffHorizontalWall() {
        vy = -vy;
        count++;
    }

    // return kinetic energy associated with this particle
    public double kineticEnergy() { return 0.5 * mass * (vx*vx + vy*vy); }
}
