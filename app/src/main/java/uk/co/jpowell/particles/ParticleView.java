package uk.co.jpowell.particles;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;


/**
 * TODO: document your custom view class.
 */
public class ParticleView extends View{

    private Paint paint;
    private static int NUM_PARTICLES = 50;

    private int contentWidth;
    private int contentHeight;

    private MinPQ<Event> pq;        // the priority queue
    private double t  = 0.0;        // simulation clock time
    private double hz = 0.5;        // number of redraw events per clock tick
    private Particle[] particles;   // the array of particles


    public ParticleView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public ParticleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public ParticleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context,attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {

        paint = new Paint();
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        setWillNotDraw(false);

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        contentWidth = size.x;
        contentHeight = size.y;

        particles = new Particle[NUM_PARTICLES];
        for (int i = 0; i < NUM_PARTICLES; i++){
            Particle p = new Particle(contentWidth,contentHeight);
            while (overlapWithAny(particles,i,p)) p = new Particle(contentWidth,contentHeight);
            particles[i] = p;
        }

        // initialize PQ with collision events and redraw event
        pq = new MinPQ<Event>();
        for (int i = 0; i < particles.length; i++) {
            predict(particles[i], Double.POSITIVE_INFINITY);
        }
        pq.insert(new Event(0, null, null));        // redraw event

        simulate();
    }

    private boolean overlapWithAny(Particle[] particles, int size, Particle p){
        for (int i = 0; i < size; i++){
            if (p.overlap(particles[i])) return true;
        }
        return false;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);

        for (Particle p : particles) {
            p.draw(canvas);
        }
        pq.insert(new Event(t + 1.0 / hz, null, null));
        simulate();
    }

    // updates priority queue with all new events for particle a
    private void predict(Particle a, double limit) {
        if (a == null) return;

        // particle-particle collisions
        for (int i = 0; i < particles.length; i++) {
            double dt = a.timeToHit(particles[i]);
            if (t + dt <= limit)
                pq.insert(new Event(t + dt, a, particles[i]));
        }

        // particle-wall collisions
        double dtX = a.timeToHitVerticalWall();
        double dtY = a.timeToHitHorizontalWall();
        if (t + dtX <= limit) pq.insert(new Event(t + dtX, a, null));
        if (t + dtY <= limit) pq.insert(new Event(t + dtY, null, a));
    }




    /********************************************************************************
     *  Event based simulation for limit seconds
     ********************************************************************************/
    public void simulate() {

        boolean simulating = true;
        // the main event-driven simulation loop
        while (simulating && !pq.isEmpty()) {

            // get impending event, discard if invalidated
            Event e = pq.delMin();
            if (!e.isValid()) continue;
            Particle a = e.a;
            Particle b = e.b;

            // physical collision, so update positions, and then simulation clock
            for (int i = 0; i < particles.length; i++)
                particles[i].move(e.time - t);
            t = e.time;

            // process event
            if      (a != null && b != null) a.bounceOff(b);              // particle-particle collision
            else if (a != null && b == null) a.bounceOffVerticalWall();   // particle-wall collision
            else if (a == null && b != null) b.bounceOffHorizontalWall(); // particle-wall collision
            else if (a == null && b == null) {
                invalidate();               // Time for a redraw event, so invalidate the view,
                simulating = false;         // and break out the simulation loop
            }

            // update the priority queue with new collisions involving a or b
            predict(a, Double.POSITIVE_INFINITY);
            predict(b, Double.POSITIVE_INFINITY);
        }
    }


    /*************************************************************************
     *  An event during a particle collision simulation. Each event contains
     *  the time at which it will occur (assuming no supervening actions)
     *  and the particles a and b involved.
     *
     *    -  a and b both null:      redraw event
     *    -  a null, b not null:     collision with vertical wall
     *    -  a not null, b null:     collision with horizontal wall
     *    -  a and b both not null:  binary collision between a and b
     *
     *************************************************************************/
    private static class Event implements Comparable<Event> {
        private final double time;         // time that event is scheduled to occur
        private final Particle a, b;       // particles involved in event, possibly null
        private final int countA, countB;  // collision counts at event creation


        // create a new event to occur at time t involving a and b
        public Event(double t, Particle a, Particle b) {
            this.time = t;
            this.a    = a;
            this.b    = b;
            if (a != null) countA = a.count();
            else           countA = -1;
            if (b != null) countB = b.count();
            else           countB = -1;
        }

        // compare times when two events will occur
        public int compareTo(Event that) {
            if      (this.time < that.time) return -1;
            else if (this.time > that.time) return +1;
            else                            return  0;
        }

        // has any collision occurred between when event was created and now?
        public boolean isValid() {
            if (a != null && a.count() != countA) return false;
            if (b != null && b.count() != countB) return false;
            return true;
        }

    }


}
