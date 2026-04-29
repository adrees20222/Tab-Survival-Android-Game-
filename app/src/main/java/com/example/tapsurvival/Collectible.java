package com.example.tapsurvival;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import java.util.Random;

public class Collectible {
    public enum Type { SHIELD, STAR, MAGNET, GHOST, GEM }
    public Type type;
    public float x, y;
    public int size;
    public float speed;
    private int screenWidth;

    public Collectible(int screenWidth, float speed, Type type) {
        this.screenWidth = screenWidth;
        this.size = screenWidth / 10;
        this.speed = speed;
        this.type = type;

        Random random = new Random();
        boolean leftLane = random.nextBoolean();
        if (leftLane) {
            x = (screenWidth / 4f) - (size / 2f);
        } else {
            x = (3 * screenWidth / 4f) - (size / 2f);
        }
        y = -size;
    }

    public void update() {
        y += speed;
    }

    public void draw(Canvas canvas, Paint paint) {
        switch (type) {
            case SHIELD:
                paint.setColor(Color.parseColor("#76FF03")); // Lime Green
                canvas.drawCircle(x + size / 2f, y + size / 2f, size / 2f, paint);
                paint.setColor(Color.BLACK);
                paint.setTextSize(size * 0.8f);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("S", x + size / 2f, y + size / 2f + (size / 4f), paint);
                break;
            case STAR:
                paint.setColor(Color.parseColor("#FFD600")); // Gold Star
                drawDiamond(canvas, paint, x + size / 2f, y + size / 2f, size / 2f);
                break;
            case MAGNET:
                paint.setColor(Color.parseColor("#E91E63")); // Pink Magnet
                canvas.drawRect(x + size * 0.2f, y + size * 0.2f, x + size * 0.8f, y + size * 0.8f, paint);
                paint.setColor(Color.WHITE);
                canvas.drawRect(x + size * 0.4f, y + size * 0.1f, x + size * 0.6f, y + size * 0.4f, paint);
                break;
            case GHOST:
                paint.setColor(Color.parseColor("#BBDEFB")); // Light Blue Ghost
                paint.setAlpha(180);
                canvas.drawCircle(x + size / 2f, y + size / 2f, size / 2f, paint);
                paint.setAlpha(255);
                break;
            case GEM:
                paint.setColor(Color.parseColor("#AA00FF")); // Purple Gem
                drawDiamond(canvas, paint, x + size / 2f, y + size / 2f, size / 2f);
                break;
        }
    }

    private void drawDiamond(Canvas canvas, Paint paint, float cx, float cy, float radius) {
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate(45);
        canvas.drawRect(-radius * 0.8f, -radius * 0.8f, radius * 0.8f, radius * 0.8f, paint);
        canvas.restore();
    }

    public RectF getCollisionRect() {
        return new RectF(x, y, x + size, y + size);
    }

    public boolean isOffScreen(int screenHeight) {
        return y > screenHeight;
    }
}
