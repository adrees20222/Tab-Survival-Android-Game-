package com.example.tapsurvival;

import android.graphics.Canvas;
import android.graphics.Paint;
import java.util.Random;

public class Particle {
    public float x, y;
    private float vx, vy;
    private int size;
    private int color;
    private float alpha = 255;
    private float fadeSpeed;

    public Particle(float x, float y, int color) {
        this.x = x;
        this.y = y;
        this.color = color;
        
        Random r = new Random();
        this.vx = (r.nextFloat() - 0.5f) * 20;
        this.vy = (r.nextFloat() - 0.5f) * 20;
        this.size = r.nextInt(15) + 5;
        this.fadeSpeed = r.nextFloat() * 5 + 2;
    }

    public void update() {
        x += vx;
        y += vy;
        alpha -= fadeSpeed;
    }

    public void draw(Canvas canvas, Paint paint) {
        if (alpha <= 0) return;
        paint.setColor(color);
        paint.setAlpha((int) alpha);
        canvas.drawRect(x, y, x + size, y + size, paint);
        paint.setAlpha(255); // Reset alpha for next draws
    }

    public boolean isDead() {
        return alpha <= 0;
    }
}
