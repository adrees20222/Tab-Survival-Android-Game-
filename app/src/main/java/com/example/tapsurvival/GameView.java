package com.example.tapsurvival;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements Runnable {

    private Thread gameThread;
    private boolean isPlaying;
    private SurfaceHolder surfaceHolder;
    private Paint paint;
    private Canvas canvas;

    private Player player;
    private GameManager gameManager;
    private Background background;

    private int screenWidth, screenHeight;


    public GameView(Context context) {
        super(context);
        surfaceHolder = getHolder();
        paint = new Paint();
    }

    @Override
    public void run() {
        while (isPlaying) {
            update();
            draw();
            control();
        }
    }

    private void update() {
        if (player == null) {
            // Initialize game objects when screen size is known
            screenWidth = getWidth();
            screenHeight = getHeight();
            if (screenWidth > 0) {
                player = new Player(screenWidth, screenHeight);
                gameManager = new GameManager(getContext(), screenWidth, screenHeight);
                background = new Background(screenWidth, screenHeight);
                
                // Set initial skin
                player.setIcon(gameManager.getActiveIcon());
                player.setTheme(gameManager.getActiveTheme());
            }
            return;
        }


        gameManager.update(player);
        if (background != null) {
            background.setSkinColor(gameManager.getBackgroundSurfaceColor());
        }
    }


    private void draw() {
        if (surfaceHolder.getSurface().isValid()) {
            canvas = surfaceHolder.lockCanvas();
            
            // Apply Screenshake
            if (gameManager != null) {
                canvas.translate(gameManager.getShakeX(), gameManager.getShakeY());
            }

            // Clear screen with Material 3 Surface color (or custom skin color)
            canvas.drawColor(gameManager.getBackgroundSurfaceColor()); 


            if (player != null && gameManager != null) {
                if (background != null) {
                    background.draw(canvas, paint, gameManager.getObstacleSpeed()); 
                }
                
                
                gameManager.draw(canvas, paint, player);

                if (gameManager.isPlaying() || gameManager.isPaused()) {
                    player.draw(canvas, paint, gameManager.isGhostModeActive(), gameManager.isFeverModeActive());
                    drawDangerVignette(canvas);
                }
            }


            surfaceHolder.unlockCanvasAndPost(canvas);

        }
    }

    private void drawDangerVignette(Canvas canvas) {
        float intensity = 0;
        if (gameManager != null) {
            for (Obstacle o : gameManager.getObstacles()) {
                // Only consider obstacles in the same lane for danger vignette
                if (Math.abs(o.x - player.x) < 50) {
                    float distY = player.y - o.y; // Distance from above
                    if (distY > 0 && distY < 800) {
                        intensity = Math.max(intensity, 1.0f - (distY / 800f));
                    }
                }
            }
        }
        
        if (intensity > 0) {
            int color = Color.argb((int)(120 * intensity), 255, 0, 0);
            android.graphics.RadialGradient gradient = new android.graphics.RadialGradient(
                screenWidth/2f, screenHeight/2f, screenHeight/1.2f,
                new int[]{Color.TRANSPARENT, color},
                null, android.graphics.Shader.TileMode.CLAMP);
            Paint vPaint = new Paint();
            vPaint.setShader(gradient);
            canvas.drawRect(0, 0, screenWidth, screenHeight, vPaint);
        }
    }

    private void control() {
        try {
            // ~60 FPS
            Thread.sleep(16);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        isPlaying = false;
        if (gameManager != null && gameManager.isPlaying()) {
            gameManager.pauseGame();
        }
        try {
            if (gameThread != null) gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        isPlaying = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float tx = event.getX();
        float ty = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (gameManager != null) {
                    gameManager.setLastTouchY(ty); // Need to add this setter to GameManager
                    gameManager.handleTouch(tx, ty, player);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (gameManager != null && (gameManager.isShopScreen() || gameManager.isSettingsScreen())) {
                    gameManager.handleScroll(ty);
                }
                break;
        }
        return true;
    }
}
