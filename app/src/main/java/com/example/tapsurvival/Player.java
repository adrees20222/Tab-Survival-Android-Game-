package com.example.tapsurvival;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.List;

public class Player {
    public float x, y;
    public int size;
    private int screenWidth;
    private boolean isLeftLane = true;
    private float targetX;
    private float speed = 50f; // Speed of switching lanes
    public boolean hasShield = false;
    private Icon currentIcon;
    private ThemeColor currentTheme;

    private List<float[]> trailPositions = new ArrayList<>();
    private static final int MAX_TRAIL = 10;
    
    private Path trianglePath = new Path();
    private Path hexagonPath = new Path();
    private Path diamondPath = new Path();
    private Path heartPath = new Path();
    private Path pentagonPath = new Path();

    public Player(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.size = screenWidth / 6;
        this.y = screenHeight - (size * 2);
        this.currentIcon = new Icon("default", "Square", Icon.Type.SQUARE, "", 0, true);
        reset();
    }

    public void reset() {
        isLeftLane = true;
        targetX = (screenWidth / 4f) - (size / 2f);
        x = targetX;
        hasShield = false;
        trailPositions.clear();
        // Pre-allocate trail positions
        for (int i = 0; i < MAX_TRAIL; i++) {
            trailPositions.add(new float[]{x, y});
        }
    }

    public void setHasShield(boolean hasShield) {
        this.hasShield = hasShield;
    }

    public void setIcon(Icon icon) {
        this.currentIcon = icon;
    }

    public void setTheme(ThemeColor theme) {
        this.currentTheme = theme;
    }

    public void toggleLane() {
        isLeftLane = !isLeftLane;
        if (isLeftLane) {
            targetX = (screenWidth / 4f) - (size / 2f);
        } else {
            targetX = (3 * screenWidth / 4f) - (size / 2f);
        }
    }

    public boolean isLeftLane() {
        return isLeftLane;
    }

    public void update() {
        // Smooth transition between lanes
        if (x < targetX) {
            x = Math.min(x + speed, targetX);
        } else if (x > targetX) {
            x = Math.max(x - speed, targetX);
        }

        // Update Trail (reuse old array)
        float[] last = trailPositions.remove(trailPositions.size() - 1);
        last[0] = x;
        last[1] = y;
        trailPositions.add(0, last);
    }

    public void draw(Canvas canvas, Paint paint, boolean isGhost, boolean isFever) {
        update();
        
        int themeColor = currentTheme != null ? currentTheme.color : Color.parseColor("#00E5FF");
        if (isFever) themeColor = Color.parseColor("#FFD600");

        // Neon Glow Pass
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(themeColor);
        paint.setAlpha(isGhost ? 40 : 80);
        float glowSize = size * 1.15f;
        drawShape(canvas, paint, x - (glowSize - size)/2f, y - (glowSize - size)/2f, glowSize);

        // Draw Trail
        for (int i = 0; i < trailPositions.size(); i++) {
            float[] pos = trailPositions.get(i);
            int alpha = (int) (150 * (1.0f - (float) i / MAX_TRAIL));
            if (isGhost) alpha /= 2;
            
            paint.setColor(themeColor);
            paint.setAlpha(alpha);
            float trailSize = size * (1.0f - (float) i / MAX_TRAIL * 0.5f);
            drawShape(canvas, paint, pos[0] + (size - trailSize)/2f, pos[1] + (size - trailSize)/2f, trailSize);
        }

        // Draw Shield Ring
        if (hasShield) {
            paint.setColor(Color.parseColor("#76FF03"));
            paint.setAlpha(isGhost ? 100 : 255);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(10);
            canvas.drawCircle(x + size/2f, y + size/2f, size * 0.8f, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        paint.setColor(themeColor);
        paint.setAlpha(isGhost ? 150 : 255);
        drawShape(canvas, paint, x, y, size);
        paint.setAlpha(255);
    }

    private void drawShape(Canvas canvas, Paint paint, float x, float y, float size) {
        if (currentIcon == null) {
            canvas.drawRect(x, y, x + size, y + size, paint);
            return;
        }

        switch (currentIcon.type) {
            case SQUARE:
                canvas.drawRect(x, y, x + size, y + size, paint);
                break;
            case CIRCLE:
                canvas.drawCircle(x + size/2f, y + size/2f, size/2f, paint);
                break;
            case TRIANGLE:
                trianglePath.reset();
                trianglePath.moveTo(x + size/2f, y);
                trianglePath.lineTo(x, y + size);
                trianglePath.lineTo(x + size, y + size);
                trianglePath.close();
                canvas.drawPath(trianglePath, paint);
                break;
            case HEXAGON:
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
                break;
            case DIAMOND:
                diamondPath.reset();
                diamondPath.moveTo(x + size/2f, y);
                diamondPath.lineTo(x + size, y + size/2f);
                diamondPath.lineTo(x + size/2f, y + size);
                diamondPath.lineTo(x, y + size/2f);
                diamondPath.close();
                canvas.drawPath(diamondPath, paint);
                break;
            case HEART:
                heartPath.reset();
                float hcx = x + size/2f;
                float hcy = y + size/2f;
                heartPath.moveTo(hcx, hcy + size/4f);
                heartPath.cubicTo(x, y, x, y + size/2f, hcx, y + size);
                heartPath.cubicTo(x + size, y + size/2f, x + size, y, hcx, hcy + size/4f);
                canvas.drawPath(heartPath, paint);
                break;
            case PENTAGON:
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
                break;
            case EMOJI:
                // Don't use the theme color for emojis if you want them to be full color
                int oldColor = paint.getColor();
                paint.setTextSize(size * 0.9f);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(currentIcon.emoji, x + size/2f, y + size * 0.85f, paint);
                paint.setColor(oldColor);
                break;
        }
    }

    public RectF getCollisionRect() {
        return new RectF(x, y, x + size, y + size);
    }
}
