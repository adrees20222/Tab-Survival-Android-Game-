package com.example.tapsurvival;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Path;
import java.util.Random;

public class Obstacle {
    public float x, y;
    public int size;
    public float speed;
    private int screenWidth;
    private static ThemeColor activeTheme;
    private static Icon.Type activeShape = Icon.Type.SQUARE;
    private boolean isShifter = false;
    private boolean hasShifted = false;
    private float targetX;

    private static Path trianglePath = new Path();
    private static Path hexagonPath = new Path();
    private static Path diamondPath = new Path();
    private static Path heartPath = new Path();
    private static Path pentagonPath = new Path();

    public static void setGlobalTheme(ThemeColor theme, Icon.Type shape) {
        activeTheme = theme;
        activeShape = shape;
        updatePaths();
    }

    private static void updatePaths() {
        // We'll update path geometry in draw to account for size/position
    }

    public Obstacle(int screenWidth, float speed) {
        this.screenWidth = screenWidth;
        this.size = screenWidth / 6;
        this.speed = speed;
        
        Random random = new Random();
        boolean leftLane = random.nextBoolean();
        
        if (leftLane) {
            x = (screenWidth / 4f) - (size / 2f);
        } else {
            x = (3 * screenWidth / 4f) - (size / 2f);
        }
        
        // 20% chance to be a shifter if game speed is high enough
        if (speed > 18 && random.nextFloat() < 0.25f) {
            isShifter = true;
            targetX = leftLane ? (3 * screenWidth / 4f) - (size / 2f) : (screenWidth / 4f) - (size / 2f);
        }
        
        y = -size; // Start just above screen
    }

    public void update() {
        y += speed;
        
        if (isShifter && !hasShifted && y > screenWidth * 0.4f) { // Shift higher up (relative to width is actually okay if it's calibrated)
            float moveSpeed = speed * 0.8f;
            if (x < targetX) x = Math.min(x + moveSpeed, targetX);
            else if (x > targetX) x = Math.max(x - moveSpeed, targetX);
            
            if (Math.abs(x - targetX) < 1.0f) {
                x = targetX;
                hasShifted = true;
            }
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        paint.setColor(activeTheme != null ? activeTheme.color : Color.parseColor("#FF1744"));
        
        // Visual cue for shifter (white border)
        if (isShifter && !hasShifted) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setColor(Color.WHITE);
            drawShapeInternal(canvas, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(activeTheme != null ? activeTheme.color : Color.parseColor("#FF1744"));
        }

        drawShapeInternal(canvas, paint);
    }

    private void drawShapeInternal(Canvas canvas, Paint paint) {
        if (activeShape == Icon.Type.CIRCLE) {
            canvas.drawCircle(x + size/2f, y + size/2f, size/2f, paint);
        } else if (activeShape == Icon.Type.TRIANGLE) {
            trianglePath.reset();
            trianglePath.moveTo(x + size/2f, y);
            trianglePath.lineTo(x, y + size);
            trianglePath.lineTo(x + size, y + size);
            trianglePath.close();
            canvas.drawPath(trianglePath, paint);
        } else if (activeShape == Icon.Type.HEXAGON) {
            hexagonPath.reset();
            float centerX = x + size/2f;
            float centerY = y + size/2f;
            float radius = size/2f;
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(60 * i - 30);
                float px = (float) (centerX + radius * Math.cos(angle));
                float py = (float) (centerY + radius * Math.sin(angle));
                if (i == 0) hexagonPath.moveTo(px, py);
                else hexagonPath.lineTo(px, py);
            }
            hexagonPath.close();
            canvas.drawPath(hexagonPath, paint);
        } else if (activeShape == Icon.Type.DIAMOND) {
            diamondPath.reset();
            diamondPath.moveTo(x + size/2f, y);
            diamondPath.lineTo(x + size, y + size/2f);
            diamondPath.lineTo(x + size/2f, y + size);
            diamondPath.lineTo(x, y + size/2f);
            diamondPath.close();
            canvas.drawPath(diamondPath, paint);
        } else if (activeShape == Icon.Type.HEART) {
            heartPath.reset();
            float cx = x + size/2f;
            float cy = y + size/2f;
            heartPath.moveTo(cx, cy + size/4f);
            heartPath.cubicTo(x, y, x, y + size/2f, cx, y + size);
            heartPath.cubicTo(x + size, y + size/2f, x + size, y, cx, cy + size/4f);
            canvas.drawPath(heartPath, paint);
        } else if (activeShape == Icon.Type.PENTAGON) {
            pentagonPath.reset();
            float pcenterX = x + size/2f;
            float pcenterY = y + size/2f;
            float pradius = size/2f;
            for (int i = 0; i < 5; i++) {
                double angle = Math.toRadians(72 * i - 90);
                float px = (float) (pcenterX + pradius * Math.cos(angle));
                float py = (float) (pcenterY + pradius * Math.sin(angle));
                if (i == 0) pentagonPath.moveTo(px, py);
                else pentagonPath.lineTo(px, py);
            }
            pentagonPath.close();
            canvas.drawPath(pentagonPath, paint);
        } else {
            canvas.drawRect(x, y, x + size, y + size, paint);
        }
    }

    public boolean isOffScreen(int screenHeight) {
        return y > screenHeight;
    }

    public RectF getCollisionRect() {
        return new RectF(x, y, x + size, y + size);
    }
}
