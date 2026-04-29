package com.example.tapsurvival;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Background {
    private class Star {
        float x, y, speed, size;
        int alpha;
        int color;
        private int baseColor;

        Star(int screenWidth, int screenHeight, Random random) {
            x = random.nextInt(screenWidth);
            y = random.nextInt(screenHeight);
            speed = 2 + random.nextFloat() * 5;
            size = 1 + random.nextFloat() * 5;
            alpha = 100 + random.nextInt(155);
            
            // Random space-like colors
            // M3 Palette stars for a soothing space feel
            int r = random.nextInt(4);
            if (r == 0) color = Color.parseColor("#D0BCFF"); // Primary (Lavender)
            else if (r == 1) color = Color.parseColor("#EFB8C8"); // Tertiary (Pinkish)
            else if (r == 2) color = Color.parseColor("#CCC2DC"); // Secondary (Muted Purple)
            else color = Color.WHITE;
            baseColor = color;

        }

        void update(float speedMultiplier, int screenHeight, int screenWidth, Random random) {
            y += speed * speedMultiplier;
            if (y > screenHeight) {
                y = -10;
                x = random.nextInt(screenWidth);
            }
        }
    }

    private List<Star> stars;
    private int screenWidth, screenHeight;
    private Random random = new Random();
    private float gridOffset = 0;
    private int activeSkinColor = Color.parseColor("#1C1B1F");

    public Background(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.stars = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            stars.add(new Star(screenWidth, screenHeight, random));
        }
    }

    public void setSkinColor(int color) {
        this.activeSkinColor = color;
    }

    public void draw(Canvas canvas, Paint paint, float gameSpeed) {
        float speedMultiplier = gameSpeed / 10f;
        
        // Draw Stars
        for (Star s : stars) {
            s.update(speedMultiplier, screenHeight, screenWidth, random);
            
            // Adjust star color based on skin for subtle harmony
            if (activeSkinColor == Color.BLACK) { // Neon/Matrix
                paint.setColor(s.baseColor);
            } else {
                paint.setColor(s.baseColor);
            }
            
            paint.setAlpha(s.alpha);
            canvas.drawCircle(s.x, s.y, s.size, paint);
        }

        // Draw Scrolling Neon Grid
        paint.setStrokeWidth(2);
        if (activeSkinColor == Color.BLACK) {
            paint.setColor(Color.parseColor("#39FF14")); // Matrix Green if black
        } else {
            paint.setColor(Color.parseColor("#313033"));
        }
        paint.setAlpha(60);
        
        // Vertical Lines
        float lane1 = screenWidth / 4f;
        float lane2 = 3 * screenWidth / 4f;
        canvas.drawLine(lane1, 0, lane1, screenHeight, paint);
        canvas.drawLine(lane2, 0, lane2, screenHeight, paint);
        canvas.drawLine(screenWidth / 2f, 0, screenWidth / 2f, screenHeight, paint);

        // Horizontal Moving Lines
        gridOffset += speedMultiplier * 10;
        if (gridOffset > 200) gridOffset = 0;
        
        for (float y = gridOffset; y < screenHeight; y += 200) {
            paint.setAlpha((int) (40 * (y / screenHeight))); // Fade out near top
            canvas.drawLine(0, y, screenWidth, y, paint);
        }

        paint.setAlpha(255);
        paint.setStyle(Paint.Style.FILL);
    }
}
