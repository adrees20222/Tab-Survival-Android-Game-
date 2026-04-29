package com.example.tapsurvival;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.List;

public class GameManager {
    public enum State { MAIN_MENU, PLAYING, PAUSED, GAME_OVER, SHOP, SETTINGS, ABOUT, HIGH_SCORES }
    private volatile State currentState = State.MAIN_MENU;
    private volatile State previousState = State.MAIN_MENU;

    private List<Obstacle> obstacles;
    private List<Collectible> collectibles;
    private List<Particle> particles;
    private int screenWidth, screenHeight;
    private long lastSpawnTime;
    private long lastItemSpawnTime;
    private long spawnInterval = 1500; // ms
    private float obstacleSpeed = 15f;
    
    private int shakeDuration = 0;
    private java.util.Random random = new java.util.Random();
    
    private int score = 0;
    private int bonusScore = 0;
    private int combo = 0;
    private float comboMultiplier = 1.0f;
    private long startTime;
    private int highScore = 0;
    private android.content.SharedPreferences prefs;
    private android.graphics.Bitmap logoBitmap;
    
    private float lastObstacleX = -1;
    private long lastObstacleSpawnTime = 0;
    private int speedUpIndicator = 0;
    private float lastSpeed = 15f;
    
    private int magnetTimer = 0;
    private int ghostTimer = 0;
    private int feverTimer = 0;
    private int sameLaneCount = 0;
    private int starCombo = 0;
    private int gems = 0;
    private List<Icon> allIcons;
    private List<ThemeColor> allThemes;
    private Icon currentIcon;
    private ThemeColor currentTheme;
    private String unlockedIconsStr = "default";
    private String unlockedThemesStr = "red_sq";
    private String unlockedPlayerThemesStr = "default";
    private String unlockedObstacleShapesStr = "square";
    private int shopTab = 0; // 0 for Shapes, 1 for Colors
    private int settingsTab = 0; // 0 for Actors, 1 for Colors
    private float shopScrollY = 0;
    private float settingsScrollY = 0;
    private float lastTouchY = 0;
    private int perfectDodgeIndicator = 0;
    
    private long pauseStartTime = 0;
    private int levelTargetScore = 5000;
    private int currentLevel = 1;
    private int bullets = 0;
    private int superBullets = 0;
    private long lastPlayerTouchTime = 0;
    private int countdown = 0;
    private int levelUpIndicator = 0;
    private float transitionAlpha = 1.0f;
    private State nextState = null;
    private boolean isTransitioning = false;
    private long savedElapsedTime = 0;
    private boolean hasSavedGame = false;
    private String[] cachedSubLines;
    private String lastSubText = "";
    private List<ThemeColor> allSkins;
    private ThemeColor currentSkin;
    private String unlockedSkinsStr = "default";
    
    // Pre-allocated Rects to avoid GC jank
    private RectF tempRect = new RectF();
    private RectF headerRect = new RectF();
    private RectF badgeRect = new RectF();
    private RectF buttonRect = new RectF();
    private android.graphics.Path tempPath = new android.graphics.Path();
    
    // Audio and Haptics
    private android.os.Vibrator vibrator;
    private android.media.SoundPool soundPool;
    private int soundSwitch, soundCollect, soundCrash, soundFever;
    private boolean soundLoaded = false;
    
    // Material 3 Palette
    private static final int COLOR_PRIMARY = Color.parseColor("#D0BCFF");
    private static final int COLOR_ON_PRIMARY = Color.parseColor("#381E72");
    private static final int COLOR_PRIMARY_CONTAINER = Color.parseColor("#4F378B");
    private static final int COLOR_SURFACE = Color.parseColor("#1C1B1F");
    private static final int COLOR_SURFACE_VARIANT = Color.parseColor("#49454F");
    private static final int COLOR_ON_SURFACE = Color.parseColor("#E6E1E5");
    private static final int COLOR_SECONDARY = Color.parseColor("#CCC2DC");
    private static final int COLOR_TERTIARY = Color.parseColor("#EFB8C8");
    private static final int COLOR_OUTLINE = Color.parseColor("#938F99");


    public GameManager(android.content.Context context, int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.obstacles = new ArrayList<>();
        this.collectibles = new ArrayList<>();
        this.particles = new ArrayList<>();
        this.prefs = context.getSharedPreferences("GamePrefs", android.content.Context.MODE_PRIVATE);
        this.highScore = prefs.getInt("highScore", 0);
        this.gems = prefs.getInt("gems", 0);
        this.bullets = prefs.getInt("bullets", 0);
        this.superBullets = prefs.getInt("superBullets", 0);
        this.currentLevel = prefs.getInt("currentLevel", 1);
        this.levelTargetScore = prefs.getInt("levelTargetScore", 5000);
        this.hasSavedGame = prefs.getBoolean("hasSavedGame", false);
        this.unlockedIconsStr = prefs.getString("unlockedIcons", "default,circle,triangle");
        this.unlockedThemesStr = prefs.getString("unlockedThemes", "red_sq");
        this.unlockedPlayerThemesStr = prefs.getString("unlockedPlayerThemes", "default");
        this.unlockedObstacleShapesStr = prefs.getString("unlockedObstacleShapes", "square");
        
        // Initialize Audio
        android.media.AudioAttributes attrs = new android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_GAME)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new android.media.SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(attrs)
                .build();
        
        // Load sounds (Placeholders - user needs to add files to res/raw)
        // soundSwitch = soundPool.load(context, R.raw.switch_lane, 1);
        // soundCollect = soundPool.load(context, R.raw.collect, 1);
        // soundShoot = soundPool.load(context, R.raw.shoot, 1);
        
        soundPool.setOnLoadCompleteListener((sp, id, status) -> soundLoaded = true);
        
        // Initialize Haptics
        vibrator = (android.os.Vibrator) context.getSystemService(android.content.Context.VIBRATOR_SERVICE);

        initCustomization();
        
        try {
            logoBitmap = android.graphics.BitmapFactory.decodeResource(context.getResources(), R.drawable.logo);
            if (logoBitmap != null) {
                float ratio = (float) logoBitmap.getHeight() / logoBitmap.getWidth();
                int targetWidth = screenWidth / 2;
                int targetHeight = (int) (targetWidth * ratio);
                logoBitmap = android.graphics.Bitmap.createScaledBitmap(logoBitmap, targetWidth, targetHeight, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        this.unlockedSkinsStr = prefs.getString("unlockedSkins", "default");
    }

    private List<ThemeColor> obstacleColors;
    private List<Icon> obstacleShapes;
    private List<ThemeColor> playerColors;
    private Icon currentObstacleShape;
    private ThemeColor currentObstacleColor;

    private void initCustomization() {
        allIcons = new ArrayList<>();
        // Player Actors (Settings)
        allIcons.add(new Icon("default", "Square", Icon.Type.SQUARE, "", 0, true));
        allIcons.add(new Icon("circle", "Circle", Icon.Type.CIRCLE, "", 0, true));
        allIcons.add(new Icon("triangle", "Triangle", Icon.Type.TRIANGLE, "", 0, true));
        
        // Emojis unlocked by Level (1 Level per 10k)
        int level = getCurrentLevel();
        allIcons.add(new Icon("girl", "Girl", Icon.Type.EMOJI, "👧", 0, level >= 2));
        allIcons.add(new Icon("boy", "Boy", Icon.Type.EMOJI, "👦", 0, level >= 3));
        allIcons.add(new Icon("rocket", "Rocket", Icon.Type.EMOJI, "🚀", 0, level >= 4));
        allIcons.add(new Icon("alien", "Alien", Icon.Type.EMOJI, "👾", 0, level >= 5));
        allIcons.add(new Icon("robot", "Robot", Icon.Type.EMOJI, "🤖", 0, level >= 6));
        allIcons.add(new Icon("car", "Car", Icon.Type.EMOJI, "🚗", 0, level >= 7));
        allIcons.add(new Icon("gem", "Gem", Icon.Type.EMOJI, "💎", 0, level >= 8));
        allIcons.add(new Icon("crown", "Crown", Icon.Type.EMOJI, "👑", 0, level >= 9));
        allIcons.add(new Icon("cat", "Cat", Icon.Type.EMOJI, "🐱", 0, level >= 10));
        allIcons.add(new Icon("dog", "Dog", Icon.Type.EMOJI, "🐶", 0, level >= 11));
        allIcons.add(new Icon("sun", "Sun", Icon.Type.EMOJI, "☀️", 0, level >= 12));
        allIcons.add(new Icon("lion", "Lion", Icon.Type.EMOJI, "🦁", 0, level >= 13));
        allIcons.add(new Icon("tiger", "Tiger", Icon.Type.EMOJI, "🐯", 0, level >= 14));
        allIcons.add(new Icon("panda", "Panda", Icon.Type.EMOJI, "🐼", 0, level >= 15));
        allIcons.add(new Icon("koala", "Koala", Icon.Type.EMOJI, "🐨", 0, level >= 16));
        allIcons.add(new Icon("frog", "Frog", Icon.Type.EMOJI, "🐸", 0, level >= 17));
        allIcons.add(new Icon("octopus", "Octopus", Icon.Type.EMOJI, "🐙", 0, level >= 18));

        
        playerColors = new ArrayList<>();
        // Player Colors (Gems)
        playerColors.add(new ThemeColor("default", "Cyan", Color.parseColor("#00E5FF"), 0, true));
        playerColors.add(new ThemeColor("ruby", "Ruby", Color.RED, 50, unlockedPlayerThemesStr.contains("ruby")));
        playerColors.add(new ThemeColor("gold", "Gold", Color.parseColor("#FFD600"), 75, unlockedPlayerThemesStr.contains("gold")));
        playerColors.add(new ThemeColor("purple", "Purple", Color.parseColor("#AA00FF"), 60, unlockedPlayerThemesStr.contains("purple")));
        playerColors.add(new ThemeColor("white", "White", Color.WHITE, 100, unlockedPlayerThemesStr.contains("white")));
        playerColors.add(new ThemeColor("neon_green", "Neon", Color.parseColor("#39FF14"), 80, unlockedPlayerThemesStr.contains("neon_green")));

        obstacleShapes = new ArrayList<>();
        // Obstacle Shapes (Shop - Gems)
        obstacleShapes.add(new Icon("square", "Square", Icon.Type.SQUARE, "", 0, true));
        obstacleShapes.add(new Icon("circle", "Circle", Icon.Type.CIRCLE, "", 30, unlockedObstacleShapesStr.contains("circle")));
        obstacleShapes.add(new Icon("triangle", "Triangle", Icon.Type.TRIANGLE, "", 30, unlockedObstacleShapesStr.contains("triangle")));
        obstacleShapes.add(new Icon("hex", "Hexagon", Icon.Type.HEXAGON, "", 50, unlockedObstacleShapesStr.contains("hex")));
        obstacleShapes.add(new Icon("diamond", "Diamond", Icon.Type.DIAMOND, "", 60, unlockedObstacleShapesStr.contains("diamond")));
        obstacleShapes.add(new Icon("heart", "Heart", Icon.Type.HEART, "", 70, unlockedObstacleShapesStr.contains("heart")));
        obstacleShapes.add(new Icon("pentagon", "Pentagon", Icon.Type.PENTAGON, "", 80, unlockedObstacleShapesStr.contains("pentagon")));

        obstacleColors = new ArrayList<>();
        // Obstacle Colors (Shop - Gems)
        obstacleColors.add(new ThemeColor("danger_red", "Danger Red", Color.parseColor("#FF1744"), 0, true));
        obstacleColors.add(new ThemeColor("frost_blue", "Frost Blue", Color.parseColor("#1E88E5"), 40, unlockedThemesStr.contains("frost_blue")));
        obstacleColors.add(new ThemeColor("acid_green", "Acid Green", Color.parseColor("#43A047"), 40, unlockedThemesStr.contains("acid_green")));
        obstacleColors.add(new ThemeColor("void_purple", "Void Purple", Color.parseColor("#AA00FF"), 60, unlockedThemesStr.contains("void_purple")));
        obstacleColors.add(new ThemeColor("sunset", "Sunset", Color.parseColor("#FF5722"), 50, unlockedThemesStr.contains("sunset")));

        allSkins = new ArrayList<>();
        allSkins.add(new ThemeColor("default", "Classic Space", Color.parseColor("#1C1B1F"), 0, true));
        allSkins.add(new ThemeColor("neon", "Neon City", Color.parseColor("#000000"), 100, unlockedSkinsStr.contains("neon")));
        allSkins.add(new ThemeColor("sunset_skin", "Deep Sunset", Color.parseColor("#210002"), 150, unlockedSkinsStr.contains("sunset_skin")));
        allSkins.add(new ThemeColor("matrix", "Digital Rain", Color.parseColor("#001000"), 200, unlockedSkinsStr.contains("matrix")));

        // Load Player
        String iconId = prefs.getString("currentIconId", "default");
        for (Icon i : allIcons) if (i.id.equals(iconId) && i.unlocked) currentIcon = i;        if (currentIcon == null) currentIcon = allIcons.get(0);

        String pColorId = prefs.getString("currentPlayerColorId", "default");
        for (ThemeColor t : playerColors) if (t.id.equals(pColorId)) currentTheme = t;
        if (currentTheme == null) currentTheme = playerColors.get(0);
        String oShapeId = prefs.getString("currentObstacleShapeId", "square");
        for (Icon i : obstacleShapes) if (i.id.equals(oShapeId)) currentObstacleShape = i;
        if (currentObstacleShape == null) currentObstacleShape = obstacleShapes.get(0);

        String oColorId = prefs.getString("currentObstacleColorId", "danger_red");
        for (ThemeColor t : obstacleColors) if (t.id.equals(oColorId)) currentObstacleColor = t;
        if (currentObstacleColor == null) currentObstacleColor = obstacleColors.get(0);
        
        String skinId = prefs.getString("currentSkinId", "default");
        for (ThemeColor t : allSkins) if (t.id.equals(skinId)) currentSkin = t;
        if (currentSkin == null) currentSkin = allSkins.get(0);

        updateObstacleTheme();
    }

    private void updateObstacleTheme() {
        if (currentObstacleColor != null && currentObstacleShape != null) {
            Obstacle.setGlobalTheme(currentObstacleColor, currentObstacleShape.type);
        }
    }

    public void update(Player player) {
        // Handle State Transitions (Faster for better UX)
        if (isTransitioning) {
            transitionAlpha -= 0.15f;
            if (transitionAlpha <= 0) {
                currentState = nextState;
                isTransitioning = false;
                transitionAlpha = 0;
            }
            return;
        } else if (transitionAlpha < 1.0f) {
            transitionAlpha += 0.15f;
            if (transitionAlpha > 1.0f) transitionAlpha = 1.0f;
        }

        if (currentState != State.PLAYING) {
            updateVisuals();
            if (shakeDuration > 0) shakeDuration--;
            return;
        }

        if (countdown > 0) {
            countdown--;
            updateVisuals();
            return;
        }

        // Increase difficulty over time
        long elapsed = System.currentTimeMillis() - startTime;
        score = (int) ((elapsed / 500) * comboMultiplier) + bonusScore; // 2 points per second + bonuses
        
        obstacleSpeed = 15f + (elapsed / 5000f); // Increase speed every 5 seconds
        if (obstacleSpeed > lastSpeed + 1f) {
            speedUpIndicator = 60; // Show message for ~1 second
            lastSpeed = obstacleSpeed;
        }
        spawnInterval = Math.max(600, 1500 - (elapsed / 10000) * 100);

        // Check for Level Up
        if (score >= levelTargetScore) {
            levelUp();
        }

        // Spawning Obstacles with balance check
        if (System.currentTimeMillis() - lastSpawnTime > spawnInterval) {
            if (feverTimer > 0) {
                // In Fever mode, spawn a Star instead of an Obstacle!
                collectibles.add(new Collectible(screenWidth, obstacleSpeed, Collectible.Type.STAR));
            } else {
                Obstacle newObstacle = new Obstacle(screenWidth, obstacleSpeed);
                
                // Track same lane count
                if (newObstacle.x == lastObstacleX) {
                    sameLaneCount++;
                } else {
                    sameLaneCount = 0;
                }

                // Logic to handle lane switching
                boolean laneSwitched = (newObstacle.x != lastObstacleX);
                float timeSinceLast = System.currentTimeMillis() - lastObstacleSpawnTime;
                
                // If lane switched too soon, or we've been in the same lane too long
                if (laneSwitched && timeSinceLast < 600) {
                    // Too soon to switch, stay in same lane
                    newObstacle.x = lastObstacleX;
                    sameLaneCount++;
                } else if (!laneSwitched && sameLaneCount >= 3) {
                    // Too long in same lane, force a switch
                    newObstacle.x = (newObstacle.x == (screenWidth / 4f) - (newObstacle.size / 2f)) ? 
                        (3 * screenWidth / 4f) - (newObstacle.size / 2f) : 
                        (screenWidth / 4f) - (newObstacle.size / 2f);
                    sameLaneCount = 0;
                }

                obstacles.add(newObstacle);
                lastObstacleX = newObstacle.x;
                lastObstacleSpawnTime = System.currentTimeMillis();
            }
            lastSpawnTime = System.currentTimeMillis();
        }

        // Spawning Collectibles
        if (System.currentTimeMillis() - lastItemSpawnTime > 4000 + random.nextInt(4000)) {
            float r = random.nextFloat();
            Collectible.Type type;
            if (r < 0.25f) type = Collectible.Type.STAR;
            else if (r < 0.35f) type = Collectible.Type.SHIELD;
            else if (r < 0.45f) type = Collectible.Type.MAGNET;
            else if (r < 0.55f) type = Collectible.Type.GHOST;
            else type = Collectible.Type.GEM; // Increased Gem rate to 45%
            
            collectibles.add(new Collectible(screenWidth, obstacleSpeed, type));
            
            // Gem Cluster logic (15% chance to spawn 2 extra gems)
            if (type == Collectible.Type.GEM && random.nextFloat() < 0.15f) {
                for (int j = 1; j <= 2; j++) {
                    Collectible extraGem = new Collectible(screenWidth, obstacleSpeed, Collectible.Type.GEM);
                    extraGem.y = -extraGem.size * (j + 1) * 2; // Offset vertically
                    collectibles.add(extraGem);
                }
            }
            lastItemSpawnTime = System.currentTimeMillis();
        }



        // Collectibles Collision and Move
        for (int i = collectibles.size() - 1; i >= 0; i--) {
            Collectible c = collectibles.get(i);
            c.update();
            
            // Magnet logic
            if (magnetTimer > 0 && (c.type == Collectible.Type.STAR || c.type == Collectible.Type.GEM)) {
                float dx = (player.x + player.size/2) - (c.x + c.size/2);
                float dy = (player.y + player.size/2) - (c.y + c.size/2);
                float dist = (float) Math.sqrt(dx*dx + dy*dy);
                if (dist < 500) {
                    c.x += dx / 10f;
                    c.y += dy / 10f;
                }
            }

            if (RectF.intersects(player.getCollisionRect(), c.getCollisionRect())) {
                if (vibrator != null) vibrator.vibrate(30);
                // if (soundLoaded) soundPool.play(soundCollect, 1, 1, 0, 0, 1);
                
                switch (c.type) {
                    case SHIELD: player.setHasShield(true); break;
                    case STAR: 
                        bonusScore += 25 + (int) (elapsed / 1000); 
                        if (feverTimer == 0) { // Only count if not already in Fever
                            starCombo++;
                            if (starCombo >= 5) {
                                feverTimer = 180; // 3 seconds of Fever
                                starCombo = 0;
                                if (vibrator != null) vibrator.vibrate(new long[]{0, 50, 50, 50}, -1);
                                // if (soundLoaded) soundPool.play(soundFever, 1, 1, 0, 0, 1);
                            }
                        }
                        break;
                    case MAGNET: magnetTimer = 300; break;
                    case GHOST: ghostTimer = 180; break;
                    case GEM: gems++; prefs.edit().putInt("gems", gems).apply(); bonusScore += 100; break;
                }
                collectibles.remove(i);
                continue;
            }
            if (c.isOffScreen(screenHeight)) collectibles.remove(i);
        }

        // Timers update
        if (magnetTimer > 0) magnetTimer--;
        if (ghostTimer > 0) ghostTimer--;
        if (feverTimer > 0) feverTimer--;
        if (perfectDodgeIndicator > 0) perfectDodgeIndicator--;
        


        // Move and collision
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle o = obstacles.get(i);
            o.update();

            // Collision Check (Invulnerable in Ghost or Fever mode)
            if (ghostTimer == 0 && feverTimer == 0 && RectF.intersects(player.getCollisionRect(), o.getCollisionRect())) {
                if (player.hasShield) {
                    player.setHasShield(false);
                    obstacles.remove(i);
                    shakeDuration = 10;
                    triggerExplosion(o.x + o.size/2, o.y + o.size/2);
                    if (vibrator != null) vibrator.vibrate(50);
                    // if (soundLoaded) soundPool.play(soundCrash, 1, 1, 0, 0, 1);
                } else {
                    currentState = State.GAME_OVER;
                    combo = 0;
                    comboMultiplier = 1.0f;
                    shakeDuration = 0; // Disabled shake
                    triggerExplosion(player.x + player.size/2, player.y + player.size/2);
                    if (vibrator != null) vibrator.vibrate(new long[]{0, 100, 100, 200}, -1);
                    // if (soundLoaded) soundPool.play(soundCrash, 1, 1, 0, 0, 1);
                    
                    if (score > highScore) {
                        highScore = score;
                        prefs.edit().putInt("highScore", highScore).apply();
                    }
                }
            }

            if (o.isOffScreen(screenHeight)) {
                obstacles.remove(i);
                // Successful dodge!
                combo++;
                comboMultiplier = 1.0f + (combo / 10f); // +0.1x every 10 dodges
            }
        }

        if (shakeDuration > 0) shakeDuration--;

        // Particles and other visual updates should continue even if not playing
        updateVisuals();
    }

    private void updateVisuals() {
        // Move Particles
        for (int i = particles.size() - 1; i >= 0; i--) {
            particles.get(i).update();
            if (particles.get(i).isDead()) particles.remove(i);
        }
    }

    private void triggerExplosion(float x, float y) {
        int color = (currentTheme != null) ? currentTheme.color : Color.parseColor("#00E5FF");
        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(x, y, color));
        }
    }

    public float getShakeX() {
        return shakeDuration > 0 ? (random.nextFloat() - 0.5f) * 20 : 0;
    }

    public float getShakeY() {
        return shakeDuration > 0 ? (random.nextFloat() - 0.5f) * 20 : 0;
    }

    public void draw(Canvas canvas, Paint paint, Player player) {
        boolean useLayer = transitionAlpha < 1.0f || isTransitioning;
        if (useLayer) {
            paint.setAlpha((int) (transitionAlpha * 255));
            canvas.saveLayer(0, 0, screenWidth, screenHeight, paint);
            paint.setAlpha(255);
        }

        // Draw Particles
        for (Particle p : particles) {
            p.draw(canvas, paint);
        }

        // Draw Collectibles
        for (Collectible c : collectibles) {
            c.draw(canvas, paint);
        }

        // Draw Obstacles
        for (Obstacle o : obstacles) {
            o.draw(canvas, paint);
        }

        // Draw Score and Combo ONLY during active play
        if (currentState == State.PLAYING) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(60);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Score: " + score, screenWidth / 2f, 100, paint);
            
            if (combo > 5) {
                paint.setColor(Color.parseColor("#FFD600"));
                paint.setTextSize(40);
                canvas.drawText("COMBO X" + String.format("%.1f", comboMultiplier), screenWidth / 2f, 160, paint);
            }
        }

        // Speed Up Indicator
        if (speedUpIndicator > 0) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(80);
            paint.setAlpha(Math.min(255, speedUpIndicator * 10));
            canvas.drawText("SPEED UP!", screenWidth / 2f, screenHeight / 3f, paint);
            speedUpIndicator--;
        }
        
        // Level Up Indicator
        if (levelUpIndicator > 0) {
            paint.setColor(Color.parseColor("#FFD600"));
            paint.setTextSize(100);
            paint.setAlpha(Math.min(255, levelUpIndicator * 10));
            canvas.drawText("LEVEL UP!", screenWidth / 2f, screenHeight / 2f, paint);
            levelUpIndicator--;
        }
        


        // Countdown Overlay
        if (currentState == State.PLAYING && countdown > 0) {
            paint.setColor(COLOR_SURFACE);
            paint.setAlpha(200);
            canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
            paint.setAlpha(255);
            
            paint.setColor(COLOR_PRIMARY);
            paint.setTextSize(60);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("LEVEL " + getCurrentLevel() + " CHALLENGE", screenWidth / 2f, screenHeight / 2f - 100, paint);
            
            paint.setTextSize(45);
            paint.setColor(Color.WHITE);
            canvas.drawText("Score " + levelTargetScore + " to complete!", screenWidth / 2f, screenHeight / 2f + 20, paint);
        }

        // Draw Power-up Timers (side of screen)
        drawCircularTimer(canvas, paint, "MAG", magnetTimer, 300, 0);
        drawCircularTimer(canvas, paint, "GHO", ghostTimer, 180, 1);
        drawCircularTimer(canvas, paint, "FEV", feverTimer, 180, 2);

        // Perfect Dodge Text
        if (perfectDodgeIndicator > 0) {
            paint.setColor(Color.parseColor("#FFEB3B"));
            paint.setTextSize(60);
            paint.setAlpha(Math.min(255, perfectDodgeIndicator * 15));
            canvas.drawText("PERFECT DODGE!", screenWidth / 2f, screenHeight / 4f, paint);
        }

        paint.setAlpha(255);

        if (currentState == State.MAIN_MENU) {
            drawMainMenu(canvas, paint);
        } else if (currentState == State.GAME_OVER) {
            drawGameOver(canvas, paint);
        } else if (currentState == State.PAUSED) {
            drawPauseMenu(canvas, paint);
        } else if (currentState == State.SHOP) {
            drawShop(canvas, paint);
        } else if (currentState == State.ABOUT) {
            drawAbout(canvas, paint);
        } else if (currentState == State.SETTINGS) {
            drawSettings(canvas, paint);
        } else if (currentState == State.HIGH_SCORES) {
            drawHighScores(canvas, paint);
        }

        // Draw Pause Button during play
        if (currentState == State.PLAYING) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(60);
            canvas.drawText("||", screenWidth - 80, 100, paint);

            // Draw Level Progress
            float progress = (float) score / levelTargetScore;
            drawProgressBar(canvas, paint, "LVL " + getCurrentLevel(), progress, screenWidth / 2f, 200);

            // Draw Bullets UI
            // Draw Bullets UI (Indicator only, no button)
            if (getCurrentLevel() >= 5) {
                paint.setColor(Color.WHITE);
                paint.setTextSize(30);
                paint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText("B: " + bullets, 30, screenHeight - 60, paint);
                if (getCurrentLevel() >= 10) {
                    canvas.drawText("S: " + superBullets, 150, screenHeight - 60, paint);
                }
            }
        }
        
        if (useLayer) {
            canvas.restore();
        }
        paint.setAlpha(255);
    }

    private void drawMainMenu(Canvas canvas, Paint paint) {
        drawOverlay(canvas, paint, "", ""); 
        float centerX = screenWidth / 2f;
        
        paint.setColor(Color.WHITE);
        paint.setTextSize(120);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        // Removed Neon Glow effect
        canvas.drawText("TAP SURVIVAL", centerX, screenHeight * 0.12f, paint);

        // Level Badge below title
        drawLevelBadge(canvas, paint, centerX, screenHeight * 0.18f);

        float startY = screenHeight * 0.28f;
        float spacing = 135;

        drawButton(canvas, paint, centerX, startY, "NEW GAME");
        drawButton(canvas, paint, centerX, startY + spacing, "SHOP");
        drawButton(canvas, paint, centerX, startY + spacing * 2, "HIGH SCORE");
        drawButton(canvas, paint, centerX, startY + spacing * 3, "SETTINGS");
        drawButton(canvas, paint, centerX, startY + spacing * 4, "ABOUT");
    }


    private void drawLevelBadge(Canvas canvas, Paint paint, float x, float y) {
        int level = getCurrentLevel();
        float progress = getLevelProgress();
        
        String levelText = "LEVEL " + level;
        paint.setTextSize(35);
        float tw = paint.measureText(levelText);
        
        badgeRect.set(x - tw/2f - 40, y - 40, x + tw/2f + 40, y + 40);
        paint.setColor(COLOR_PRIMARY_CONTAINER);
        canvas.drawRoundRect(badgeRect, 40, 40, paint);
        
        // Progress bar inside badge
        paint.setColor(COLOR_PRIMARY);
        paint.setAlpha(100);
        canvas.drawRoundRect(badgeRect.left, badgeRect.top, badgeRect.left + badgeRect.width() * progress, badgeRect.bottom, 40, 40, paint);
        
        paint.setAlpha(255);
        paint.setColor(COLOR_ON_SURFACE);
        canvas.drawText(levelText, x, y + 12, paint);
    }

    private void drawGameOver(Canvas canvas, Paint paint) {
        drawOverlay(canvas, paint, "GAME OVER", "Final Score: " + score + "\nHigh Score: " + highScore + "\n\nTap to Menu");
    }

    private void drawPauseMenu(Canvas canvas, Paint paint) {
        drawOverlay(canvas, paint, "PAUSED", "");
        drawButton(canvas, paint, screenWidth / 2f, screenHeight / 2f, "CONTINUE");
        drawButton(canvas, paint, screenWidth / 2f, screenHeight / 2f + 120, "MAIN MENU");
    }

    private void drawShop(Canvas canvas, Paint paint) {
        drawOverlay(canvas, paint, "OBSTACLE SHOP", "Customize the blocks you avoid!");
        float centerX = screenWidth / 2f;
        
        // Gems are now drawn in the header via drawOverlay
        paint.setFakeBoldText(false);

        drawTabButton(canvas, paint, screenWidth * 0.15f, screenHeight * 0.28f, "SHAPES", shopTab == 0, 0.22f);
        drawTabButton(canvas, paint, screenWidth * 0.38f, screenHeight * 0.28f, "COLORS", shopTab == 1, 0.22f);
        drawTabButton(canvas, paint, screenWidth * 0.61f, screenHeight * 0.28f, "SKINS", shopTab == 2, 0.22f);
        drawTabButton(canvas, paint, screenWidth * 0.85f, screenHeight * 0.28f, "BULLETS", shopTab == 3, 0.22f);

        float startY = screenHeight * 0.42f + shopScrollY;
        float spacing = 150;
        
        canvas.save();
        canvas.clipRect(0, screenHeight * 0.34f, screenWidth, screenHeight * 0.80f); 
        if (shopTab == 0) {
            for (int i = 0; i < obstacleShapes.size(); i++) {
                Icon shape = obstacleShapes.get(i);
                drawItemCard(canvas, paint, centerX, startY + i * 180, shape.name, shape.price, shape.unlocked, shape == currentObstacleShape, null, shape);
            }
        } else if (shopTab == 1) {
            for (int i = 0; i < obstacleColors.size(); i++) {
                ThemeColor color = obstacleColors.get(i);
                drawItemCard(canvas, paint, centerX, startY + i * 180, color.name, color.price, color.unlocked, color == currentObstacleColor, color, null);
            }
        } else if (shopTab == 2) {
            for (int i = 0; i < allSkins.size(); i++) {
                ThemeColor skin = allSkins.get(i);
                drawItemCard(canvas, paint, centerX, startY + i * 180, skin.name, skin.price, skin.unlocked, skin == currentSkin, skin, null);
            }
        } else {
            // Bullets Tab
            if (getCurrentLevel() < 5) {
                paint.setColor(COLOR_ON_SURFACE);
                paint.setTextSize(40);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("Bullets Unlock at Level 5", centerX, screenHeight * 0.5f, paint);
            } else {
                drawItemCard(canvas, paint, centerX, startY, "1 Bullet", 10, true, false, null, null);
                paint.setColor(COLOR_ON_SURFACE);
                paint.setTextSize(32);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("You have: " + bullets, centerX, startY + 80, paint);

                if (getCurrentLevel() >= 10) {
                    drawItemCard(canvas, paint, centerX, startY + 220, "Screen Clear", 15, true, false, null, null);
                    canvas.drawText("You have: " + superBullets, centerX, startY + 300, paint);
                } else {
                    drawItemCard(canvas, paint, centerX, startY + 220, "Locked (Lvl 10)", 0, false, false, null, null);
                }
            }
        }
        canvas.restore();
        drawButton(canvas, paint, centerX, screenHeight * 0.92f, "BACK");
    }

    private void drawItemCard(Canvas canvas, Paint paint, float x, float y, String name, int price, boolean unlocked, boolean active, ThemeColor colorPreview, Icon iconPreview) {
        float width = 650;
        float height = 140;
        tempRect.set(x - width/2f, y - height/2f, x + width/2f, y + height/2f);
        
        // Elevation shadow
        paint.setColor(Color.BLACK);
        paint.setAlpha(30);
        canvas.drawRoundRect(tempRect.left + 4, tempRect.top + 6, tempRect.right + 4, tempRect.bottom + 6, 24, 24, paint);
        
        // Background
        paint.setAlpha(255);
        paint.setColor(active ? COLOR_PRIMARY : COLOR_SURFACE_VARIANT);
        canvas.drawRoundRect(tempRect, 24, 24, paint);
        
        // Outline
        if (!active) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setColor(COLOR_OUTLINE);
            canvas.drawRoundRect(tempRect, 24, 24, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        // Icon/Color Preview
        float iconSize = 60;
        float iconX = tempRect.left + 60;
        if (colorPreview != null) {
            paint.setColor(colorPreview.color);
            canvas.drawCircle(iconX, y, iconSize/2, paint);
        } else if (iconPreview != null) {
            paint.setColor(COLOR_ON_SURFACE);
            if (iconPreview.type == Icon.Type.EMOJI) {
                paint.setTextSize(iconSize);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(iconPreview.emoji, iconX, y + iconSize/3, paint);
            } else {
                float s = iconSize / 2;
                if (iconPreview.type == Icon.Type.SQUARE) canvas.drawRect(iconX - s, y - s, iconX + s, y + s, paint);
                else if (iconPreview.type == Icon.Type.CIRCLE) canvas.drawCircle(iconX, y, s, paint);
                else if (iconPreview.type == Icon.Type.TRIANGLE || iconPreview.type == Icon.Type.HEXAGON || 
                         iconPreview.type == Icon.Type.DIAMOND || iconPreview.type == Icon.Type.HEART || 
                         iconPreview.type == Icon.Type.PENTAGON) {
                    tempPath.reset();
                    if (iconPreview.type == Icon.Type.TRIANGLE) {
                        tempPath.moveTo(iconX, y - s);
                        tempPath.lineTo(iconX - s, y + s);
                        tempPath.lineTo(iconX + s, y + s);
                    } else if (iconPreview.type == Icon.Type.DIAMOND) {
                        tempPath.moveTo(iconX, y - s);
                        tempPath.lineTo(iconX + s, y);
                        tempPath.lineTo(iconX, y + s);
                        tempPath.lineTo(iconX - s, y);
                    } else if (iconPreview.type == Icon.Type.HEXAGON) {
                        for (int i = 0; i < 6; i++) {
                            float angle = (float) (i * Math.PI / 3);
                            float px = iconX + s * (float) Math.cos(angle);
                            float py = y + s * (float) Math.sin(angle);
                            if (i == 0) tempPath.moveTo(px, py); else tempPath.lineTo(px, py);
                        }
                    } else if (iconPreview.type == Icon.Type.PENTAGON) {
                        for (int i = 0; i < 5; i++) {
                            float angle = (float) (i * 2 * Math.PI / 5 - Math.PI / 2);
                            float px = iconX + s * (float) Math.cos(angle);
                            float py = y + s * (float) Math.sin(angle);
                            if (i == 0) tempPath.moveTo(px, py); else tempPath.lineTo(px, py);
                        }
                    } else if (iconPreview.type == Icon.Type.HEART) {
                        tempPath.moveTo(iconX, y + s * 0.7f);
                        tempPath.cubicTo(iconX - s, y - s * 0.3f, iconX - s * 0.5f, y - s * 1.2f, iconX, y - s * 0.4f);
                        tempPath.cubicTo(iconX + s * 0.5f, y - s * 1.2f, iconX + s, y - s * 0.3f, iconX, y + s * 0.7f);
                    }
                    tempPath.close();
                    canvas.drawPath(tempPath, paint);
                } else {
                    canvas.drawRect(iconX - s, y - s, iconX + s, y + s, paint);
                }
            }
        } else {
            // Bullet / Item icons
            paint.setTextSize(40);
            paint.setTextAlign(Paint.Align.CENTER);
            String icon = name.contains("Bullet") ? "🔫" : (name.contains("Clear") ? "🧨" : "📦");
            canvas.drawText(icon, iconX, y + 15, paint);
        }

        // Text Info
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(active ? COLOR_ON_PRIMARY : COLOR_ON_SURFACE);
        paint.setTextSize(38);
        paint.setFakeBoldText(true);
        canvas.drawText(name, iconX + 100, y + 10, paint);
        paint.setFakeBoldText(false);

        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(32);
        String status = unlocked ? (active ? "ACTIVE" : "SELECT") : "💎 " + price;
        canvas.drawText(status, tempRect.right - 40, y + 10, paint);
    }


    private void drawAbout(Canvas canvas, Paint paint) {
        drawOverlay(canvas, paint, "ABOUT", "Tap Survival: Reflex Challenge");
        float centerX = screenWidth / 2f;
        paint.setColor(COLOR_ON_SURFACE);
        paint.setTextSize(40);
        paint.setTextAlign(Paint.Align.CENTER);
        
        String aboutText = "Developed by:\nMuhammad Adrees\n+923077377945\n\nAvoid blocks, collect gems,\nsurvive as long as you can!\n\nDesigned for relaxation\nand focus.";
        String[] lines = aboutText.split("\n");
        for (int i = 0; i < lines.length; i++) {
            canvas.drawText(lines[i], centerX, screenHeight * 0.35f + (i * 55), paint);
        }
        
        drawButton(canvas, paint, centerX, screenHeight * 0.92f, "BACK");
    }




    private void drawSettings(Canvas canvas, Paint paint) {
        drawOverlay(canvas, paint, "CHARACTER SELECT", "Customize your look!");
        float centerX = screenWidth / 2f;
        
        drawTabButton(canvas, paint, screenWidth * 0.3f, screenHeight * 0.27f, "ACTORS", settingsTab == 0);
        drawTabButton(canvas, paint, screenWidth * 0.7f, screenHeight * 0.27f, "COLORS", settingsTab == 1);

        float startY = screenHeight * 0.42f + settingsScrollY;
        float spacing = 150;
        
        canvas.save();
        canvas.clipRect(0, screenHeight * 0.34f, screenWidth, screenHeight * 0.80f);
        if (settingsTab == 0) {
            for (int i = 0; i < allIcons.size(); i++) {
                Icon icon = allIcons.get(i);
                int price = (i < 3) ? 0 : (i - 1) * 10; // Placeholder price for logic
                drawItemCard(canvas, paint, centerX, startY + i * 180, icon.name, price, icon.unlocked, icon == currentIcon, null, icon);
            }
        } else {
            for (int i = 0; i < playerColors.size(); i++) {
                ThemeColor color = playerColors.get(i);
                drawItemCard(canvas, paint, centerX, startY + i * 180, color.name, color.price, color.unlocked, color == currentTheme, color, null);
            }
        }
        canvas.restore();
        
        drawButton(canvas, paint, centerX, screenHeight * 0.92f, "BACK");
    }

    private void drawHighScores(Canvas canvas, Paint paint) {
        drawOverlay(canvas, paint, "HIGH SCORES", "Your best performance!");
        float centerX = screenWidth / 2f;
        
        paint.setColor(COLOR_TERTIARY);
        paint.setTextSize(70);
        paint.setFakeBoldText(true);
        canvas.drawText("BEST SCORE", centerX, screenHeight * 0.40f, paint);
        
        paint.setColor(COLOR_ON_SURFACE);
        paint.setTextSize(140);
        canvas.drawText(String.valueOf(highScore), centerX, screenHeight * 0.55f, paint);
        paint.setFakeBoldText(false);
        
        drawButton(canvas, paint, centerX, screenHeight * 0.92f, "BACK");
    }

    private float getPulse() {
        return (float) (Math.sin(System.currentTimeMillis() / 400.0) * 0.05 + 1.0);
    }

    private void drawButton(Canvas canvas, Paint paint, float x, float y, String text, int color, boolean locked, boolean active, int previewColor) {
        float width = 600;
        float height = 100;
        buttonRect.set(x - width/2f, y - height/2f, x + width/2f, y + height/2f);
        
        // Background
        paint.setAlpha(255);
        if (locked) {
            paint.setColor(COLOR_SURFACE_VARIANT);
        } else if (active) {
            paint.setColor(COLOR_PRIMARY);
        } else {
            paint.setColor(COLOR_PRIMARY_CONTAINER);
        }
        canvas.drawRoundRect(buttonRect, height/2f, height/2f, paint);
        
        // Outline
        if (!active && !locked) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setColor(COLOR_OUTLINE);
            canvas.drawRoundRect(buttonRect, height/2f, height/2f, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        // Text
        paint.setColor(active ? COLOR_ON_PRIMARY : COLOR_ON_SURFACE);
        paint.setTextSize(40);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        canvas.drawText(text, x, y + 15, paint);
        paint.setFakeBoldText(false);
    }
    private void drawButton(Canvas canvas, Paint paint, float x, float y, String text) {
        drawButton(canvas, paint, x, y, text, 0, false, false, 0);
    }



    public void handleTouch(float tx, float ty, Player player) {
        float centerX = screenWidth / 2f;
        if (currentState == State.MAIN_MENU) {
            float startY = screenHeight * 0.28f;
            float spacing = 150;
            if (isInside(tx, ty, centerX, startY)) startGame();
            else if (isInside(tx, ty, centerX, startY + spacing)) switchState(State.SHOP);
            else if (isInside(tx, ty, centerX, startY + spacing * 2)) switchState(State.HIGH_SCORES);
            else if (isInside(tx, ty, centerX, startY + spacing * 3)) switchState(State.SETTINGS);
            else if (isInside(tx, ty, centerX, startY + spacing * 4)) switchState(State.ABOUT);
        } else if (currentState == State.PLAYING) {
            if (tx > screenWidth - 150 && ty < 150) {
                pauseGame();
            } else if (player.getCollisionRect().contains(tx, ty)) {
                long now = System.currentTimeMillis();
                if (now - lastPlayerTouchTime < 300) {
                    shoot(player);
                    lastPlayerTouchTime = 0; // Reset to prevent triple-tap firing twice
                } else {
                    lastPlayerTouchTime = now;
                }
            } else {
                player.toggleLane();
                onPlayerToggleLane(player);
            }
        } else if (currentState == State.PAUSED) {
            if (isInside(tx, ty, screenWidth/2f, screenHeight/2f)) resumeGame();
            else if (isInside(tx, ty, screenWidth/2f, screenHeight/2f + 120)) {
                switchState(State.MAIN_MENU);
                reset();
            }
        } else if (currentState == State.GAME_OVER) {
            switchState(State.MAIN_MENU);
            reset();
        } else if (currentState == State.SHOP) {
            lastTouchY = ty;
            // Tab Selection (centered at 0.28f)
            if (ty > screenHeight * 0.24f && ty < screenHeight * 0.32f) {
                if (tx < screenWidth * 0.25f) shopTab = 0;
                else if (tx < screenWidth * 0.50f) shopTab = 1;
                else if (tx < screenWidth * 0.75f) shopTab = 2;
                else shopTab = 3;
                shopScrollY = 0;
            }
            
            // Buy Bullets Tab Logic
            if (shopTab == 3 && getCurrentLevel() >= 5) {
                float listStartY = screenHeight * 0.42f + shopScrollY;
                float spacing = 150;
                
                // Buy 1 Bullet
                if (isInside(tx, ty, screenWidth/2f, listStartY, 300)) {
                    if (gems >= 10) {
                        gems -= 10;
                        bullets++;
                        prefs.edit().putInt("gems", gems).putInt("bullets", bullets).apply();
                        if (vibrator != null) vibrator.vibrate(50);
                    }
                }
                // Buy Screen Clear (startY + 220)
                else if (getCurrentLevel() >= 10 && isInside(tx, ty, screenWidth/2f, listStartY + 220, 300)) {
                    if (gems >= 15) {
                        gems -= 15;
                        superBullets++;
                        prefs.edit().putInt("gems", gems).putInt("superBullets", superBullets).apply();
                        if (vibrator != null) vibrator.vibrate(new long[]{0, 50, 50, 50}, -1);
                    }
                }
            }

            float listStartY = screenHeight * 0.42f + shopScrollY;
            float spacing = 180;

            if (shopTab == 0) {
                for (int i = 0; i < obstacleShapes.size(); i++) {
                    if (isInside(tx, ty, screenWidth/2f, listStartY + i * spacing, 300)) {
                        Icon shape = obstacleShapes.get(i);
                        if (shape.unlocked) {
                            currentObstacleShape = shape;
                            updateObstacleTheme();
                            prefs.edit().putString("currentObstacleShapeId", shape.id).apply();
                        } else if (gems >= shape.price) {
                            gems -= shape.price;
                            shape.unlocked = true;
                            unlockedObstacleShapesStr += "," + shape.id;
                            prefs.edit().putInt("gems", gems).putString("unlockedObstacleShapes", unlockedObstacleShapesStr).apply();
                        }
                    }
                }
            } else if (shopTab == 1) {
                for (int i = 0; i < obstacleColors.size(); i++) {
                    if (isInside(tx, ty, screenWidth/2f, listStartY + i * spacing, 300)) {
                        ThemeColor color = obstacleColors.get(i);
                        if (color.unlocked) {
                            currentObstacleColor = color;
                            updateObstacleTheme();
                            prefs.edit().putString("currentObstacleColorId", color.id).apply();
                        } else if (gems >= color.price) {
                            gems -= color.price;
                            color.unlocked = true;
                            unlockedThemesStr += "," + color.id;
                            prefs.edit().putInt("gems", gems).putString("unlockedThemes", unlockedThemesStr).apply();
                        }
                    }
                }
            } else {
                for (int i = 0; i < allSkins.size(); i++) {
                    if (isInside(tx, ty, screenWidth/2f, listStartY + i * spacing, 300)) {
                        ThemeColor skin = allSkins.get(i);
                        if (skin.unlocked) {
                            currentSkin = skin;
                            prefs.edit().putString("currentSkinId", skin.id).apply();
                        } else if (gems >= skin.price) {
                            gems -= skin.price;
                            skin.unlocked = true;
                            unlockedSkinsStr += "," + skin.id;
                            prefs.edit().putInt("gems", gems).putString("unlockedSkins", unlockedSkinsStr).apply();
                        }
                    }
                }
            }
            if (isInside(tx, ty, screenWidth/2f, screenHeight * 0.92f, 300)) switchState(State.MAIN_MENU);

        } else if (currentState == State.SETTINGS) {
            lastTouchY = ty;
            // Tab Selection (centered at 0.27f)
            if (ty > screenHeight * 0.24f && ty < screenHeight * 0.30f) {

                if (tx < screenWidth / 2f) settingsTab = 0;
                else settingsTab = 1;
                settingsScrollY = 0;
            }

            float listStartY = screenHeight * 0.42f + settingsScrollY;
            float spacing = 180;

            if (settingsTab == 0) {
                for (int i = 0; i < allIcons.size(); i++) {
                    if (isInside(tx, ty, screenWidth/2f, listStartY + i * spacing, 300)) {
                        Icon icon = allIcons.get(i);
                        if (icon.unlocked) {
                            currentIcon = icon;
                            player.setIcon(icon);
                            prefs.edit().putString("currentIconId", icon.id).apply();
                        }
                    }
                }
            } else {
                for (int i = 0; i < playerColors.size(); i++) {
                    if (isInside(tx, ty, screenWidth/2f, listStartY + i * spacing, 300)) {
                        ThemeColor color = playerColors.get(i);
                        if (color.unlocked) {
                            currentTheme = color;
                            player.setTheme(color);
                            prefs.edit().putString("currentPlayerColorId", color.id).apply();
                        } else if (gems >= color.price) {
                            gems -= color.price;
                            color.unlocked = true;
                            unlockedPlayerThemesStr += "," + color.id;
                            prefs.edit().putInt("gems", gems).putString("unlockedPlayerThemes", unlockedPlayerThemesStr).apply();
                        }
                    }
                }
            }
            if (isInside(tx, ty, screenWidth/2f, screenHeight * 0.92f, 300)) switchState(State.MAIN_MENU);

        } else if (currentState == State.ABOUT || currentState == State.HIGH_SCORES) {
            if (isInside(tx, ty, screenWidth/2f, screenHeight * 0.90f)) switchState(State.MAIN_MENU);
        }

    }

    private boolean isInside(float tx, float ty, float bx, float by, float width) {
        return tx > bx - width && tx < bx + width && ty > by - 60 && ty < by + 60;
    }

    private boolean isInside(float tx, float ty, float bx, float by) {
        return isInside(tx, ty, bx, by, 300);
    }

    private void drawTabButton(Canvas canvas, Paint paint, float x, float y, String text, boolean active, float widthPercent) {
        float width = screenWidth * widthPercent;
        float height = 80;
        tempRect.set(x - width/2f, y - height/2f, x + width/2f, y + height/2f);
        
        if (active) {
            paint.setColor(COLOR_PRIMARY);
            canvas.drawRoundRect(tempRect, 40, 40, paint);
        } else {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setColor(COLOR_OUTLINE);
            canvas.drawRoundRect(tempRect, 40, 40, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        
        paint.setColor(active ? COLOR_ON_PRIMARY : COLOR_ON_SURFACE);
        paint.setTextSize(30);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, x, y + 12, paint);
    }

    private void drawTabButton(Canvas canvas, Paint paint, float x, float y, String text, boolean active) {
        drawTabButton(canvas, paint, x, y, text, active, 0.35f);
    }

    private void drawCircularTimer(Canvas canvas, Paint paint, String label, int current, int max, int index) {
        if (current <= 0) return;
        float size = 80;
        float x = 70;
        float y = 280 + (index * 130);
        
        tempRect.set(x - size/2, y - size/2, x + size/2, y + size/2);
        
        // Background track
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);
        paint.setColor(COLOR_SURFACE_VARIANT);
        canvas.drawCircle(x, y, size/2, paint);
        
        // Progress arc
        paint.setColor(label.equals("MAG") ? Color.parseColor("#E91E63") : COLOR_PRIMARY);
        float angle = 360f * (current / (float) max);
        canvas.drawArc(tempRect, -90, angle, false, paint);
        
        // Icon in center
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(40);
        paint.setTextAlign(Paint.Align.CENTER);
        String icon = label.equals("MAG") ? "🧲" : (label.equals("GST") ? "👻" : "🔥");
        canvas.drawText(icon, x, y + 15, paint);
        
        // Timer Text OUTSIDE (to the right)
        paint.setColor(Color.WHITE);
        paint.setTextSize(32);
        paint.setTextAlign(Paint.Align.LEFT);
        String timeStr = (current / 60) + "s"; // Assuming 60fps
        canvas.drawText(timeStr, x + size/2 + 20, y + 10, paint);
    }

    private void drawOverlay(Canvas canvas, Paint paint, String title, String sub) {
        // Glassmorphism background
        paint.setColor(Color.parseColor("#0F0D13"));
        paint.setAlpha(235);
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        
        // Subtle gradient mask for "blur" feel
        android.graphics.RadialGradient glass = new android.graphics.RadialGradient(screenWidth/2f, screenHeight/2f, screenWidth,
                Color.TRANSPARENT, Color.parseColor("#1AFFFFFF"), android.graphics.Shader.TileMode.CLAMP);
        paint.setShader(glass);
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        paint.setShader(null);
        paint.setAlpha(255);

        if (!title.isEmpty()) {
            android.graphics.LinearGradient gradient = new android.graphics.LinearGradient(0, 0, 0, screenHeight * 0.15f,
                    Color.parseColor("#6750A4"), Color.parseColor("#381E72"), android.graphics.Shader.TileMode.CLAMP);
            paint.setShader(gradient);
            headerRect.set(0, 0, screenWidth, screenHeight * 0.15f);
            canvas.drawRect(headerRect, paint);
            paint.setShader(null);
            
            paint.setColor(Color.WHITE);
            paint.setFakeBoldText(true);
            
            if (title.equals("OBSTACLE SHOP")) {
                // Title on the left (80% space)
                paint.setTextSize(65);
                paint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(title, 40, screenHeight * 0.08f, paint);
                
                // Gems on the right (20% space)
                paint.setTextSize(40);
                paint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText("💎 " + gems, screenWidth - 40, screenHeight * 0.08f, paint);
            } else {
                // Regular centered title for other screens
                paint.setTextSize(75);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(title, screenWidth / 2f, screenHeight * 0.08f, paint);
            }
            paint.setFakeBoldText(false);
        }

        if (!sub.isEmpty()) {
            paint.setTextSize(38);
            paint.setColor(COLOR_SECONDARY);
            paint.setTextAlign(Paint.Align.CENTER);
            
            if (!sub.equals(lastSubText)) {
                cachedSubLines = sub.split("\n");
                lastSubText = sub;
            }
            
            if (cachedSubLines != null) {
                for (int i = 0; i < cachedSubLines.length; i++) {
                    canvas.drawText(cachedSubLines[i], screenWidth / 2f, screenHeight * 0.20f + (i * 55), paint);
                }
            }
        }
    }

    public void startGame() {
        switchState(State.PLAYING);
        countdown = 120; // 2 second countdown
        
        // Random Challenge Score: 4000 to 6500 for all games
        levelTargetScore = 4000 + random.nextInt(2501);
        
        startTime = System.currentTimeMillis();
        lastSpawnTime = System.currentTimeMillis();
        lastItemSpawnTime = System.currentTimeMillis();
        score = 0;
        bonusScore = 0;
        lastObstacleX = -1;
        magnetTimer = 0;
        ghostTimer = 0;
        feverTimer = 0;
        starCombo = 0;
        sameLaneCount = 0;
        hasSavedGame = false;
        prefs.edit().putBoolean("hasSavedGame", false).apply();
        obstacles.clear();
        collectibles.clear();
    }

    private void continueGame() {
        if (hasSavedGame) {
            loadGameState();
            resumeGame();
        } else {
            currentState = State.PLAYING;
            resumeGame();
        }
    }

    public void reset() {
        clearSavedGame();
        currentState = State.MAIN_MENU;
        score = 0;
        bonusScore = 0;
        obstacles.clear();
        collectibles.clear();
        magnetTimer = 0;
        ghostTimer = 0;
        feverTimer = 0;
        sameLaneCount = 0;
        perfectDodgeIndicator = 0;
    }

    public void onPlayerToggleLane(Player player) {
        // Haptic on lane switch
        if (vibrator != null) vibrator.vibrate(20);
        // if (soundLoaded) soundPool.play(soundSwitch, 1, 1, 0, 0, 1);

        // Check for Perfect Dodge
        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle o = obstacles.get(i);
            if (o == null) continue;
            
            float distY = Math.abs(o.y - player.y);
            // Must be very close vertically and the obstacle must be in the lane the player just LEFT
            // (If isLeftLane is true now, they just left the Right lane)
            float laneX = player.isLeftLane() ? (3 * screenWidth / 4f) - (o.size / 2f) : (screenWidth / 4f) - (o.size / 2f);
            
            if (distY < 300 && Math.abs(o.x - laneX) < 10) { 
                perfectDodgeIndicator = 40;
                bonusScore += 50;
                if (vibrator != null) vibrator.vibrate(40);
                break;
            }
        }
    }

    public boolean isStartScreen() { return currentState == State.MAIN_MENU; }
    public boolean isGameOver() { return currentState == State.GAME_OVER; }
    public boolean isPlaying() { return currentState == State.PLAYING; }
    public boolean isPaused() { return currentState == State.PAUSED; }
    public float getObstacleSpeed() { return obstacleSpeed; }
    public boolean isGhostModeActive() { return ghostTimer > 0; }
    public boolean isFeverModeActive() { return feverTimer > 0; }
    public List<Obstacle> getObstacles() { return obstacles; }
    public boolean isShopScreen() { return currentState == State.SHOP; }
    public boolean isSettingsScreen() { return currentState == State.SETTINGS; }
    public State getCurrentState() { return currentState; }

    
    public int getCurrentLevel() {
        return currentLevel;
    }
    
    public float getLevelProgress() {
        return (float) score / levelTargetScore;
    }



    public void setLastTouchY(float y) {
        this.lastTouchY = y;
    }

    public void handleScroll(float currentY) {
        if (lastTouchY != 0) {
            float delta = (currentY - lastTouchY) * 2.0f;
            float visibleArea = screenHeight * 0.48f; // screenHeight * (0.84 - 0.36)
            if (currentState == State.SHOP) {
                shopScrollY += delta;
                int listSize;
                if (shopTab == 0) listSize = obstacleShapes.size();
                else if (shopTab == 1) listSize = obstacleColors.size();
                else if (shopTab == 2) listSize = allSkins.size();
                else listSize = (getCurrentLevel() >= 10 ? 3 : 2); // Bullets tab spacing
                
                float maxScroll = Math.max(0, (listSize * 180) - visibleArea + 180);
                if (shopScrollY > 0) shopScrollY = 0;
                if (shopScrollY < -maxScroll) shopScrollY = -maxScroll;
            } else if (currentState == State.SETTINGS) {
                settingsScrollY += delta;
                int listSize = (settingsTab == 0 ? allIcons.size() : playerColors.size());
                float maxScroll = Math.max(0, (listSize * 180) - visibleArea + 180);
                if (settingsScrollY > 0) settingsScrollY = 0;
                if (settingsScrollY < -maxScroll) settingsScrollY = -maxScroll;
            }

        }
        lastTouchY = currentY;
    }

    public Icon getActiveIcon() { return currentIcon; }
    public ThemeColor getActiveTheme() { return currentTheme; }
    
    public void drawWatermark(Canvas canvas) {
        if (logoBitmap != null) {
            Paint p = new Paint();
            p.setAlpha(30); // Faint watermark
            canvas.drawBitmap(logoBitmap, (screenWidth - logoBitmap.getWidth()) / 2f, screenHeight * 0.02f, p);
        }
    }

    private void switchState(State state) {
        if (state == currentState) return;
        nextState = state;
        isTransitioning = true;
        transitionAlpha = 1.0f;
    }

    public void pauseGame() {
        switchState(State.PAUSED);
        pauseStartTime = System.currentTimeMillis();
        saveGameState();
    }

    public void resumeGame() {
        switchState(State.PLAYING);
        if (pauseStartTime > 0) {
            startTime += (System.currentTimeMillis() - pauseStartTime);
            pauseStartTime = 0;
        } else if (hasSavedGame) {
            // If resuming from a completely closed app
            startTime = System.currentTimeMillis() - savedElapsedTime;
            hasSavedGame = false;
            prefs.edit().putBoolean("hasSavedGame", false).apply();
        }
    }

    private void saveGameState() {
        if (score <= 0 && !isPaused()) return;
        
        long elapsed = System.currentTimeMillis() - startTime;
        if (isPaused() && pauseStartTime > 0) {
            elapsed = pauseStartTime - startTime;
        }

        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("savedScore", score);
        editor.putInt("savedBonusScore", bonusScore);
        editor.putLong("savedElapsedTime", elapsed);
        editor.putFloat("savedObstacleSpeed", obstacleSpeed);
        editor.putInt("savedCurrentLevel", currentLevel);
        editor.putInt("savedLevelTargetScore", levelTargetScore);
        editor.putInt("savedBullets", bullets);
        editor.putInt("savedSuperBullets", superBullets);
        editor.putBoolean("hasSavedGame", true);
        editor.apply();
        hasSavedGame = true;
    }

    private void loadGameState() {
        score = prefs.getInt("savedScore", 0);
        bonusScore = prefs.getInt("savedBonusScore", 0);
        savedElapsedTime = prefs.getLong("savedElapsedTime", 0);
        obstacleSpeed = prefs.getFloat("savedObstacleSpeed", 15f);
        currentLevel = prefs.getInt("savedCurrentLevel", 1);
        levelTargetScore = prefs.getInt("savedLevelTargetScore", 5000);
        bullets = prefs.getInt("savedBullets", 0);
        superBullets = prefs.getInt("savedSuperBullets", 0);
    }

    private void clearSavedGame() {
        hasSavedGame = false;
        prefs.edit().remove("savedScore").remove("savedElapsedTime").putBoolean("hasSavedGame", false).apply();
    }

    private void levelUp() {
        levelUpIndicator = 100;
        currentLevel++;
        // Random challenge for next level: 4000 to 6500
        levelTargetScore += 4000 + random.nextInt(2501); 
        prefs.edit().putInt("currentLevel", currentLevel).putInt("levelTargetScore", levelTargetScore).apply();
        if (vibrator != null) vibrator.vibrate(new long[]{0, 100, 50, 100}, -1);
    }

    private void drawProgressBar(Canvas canvas, Paint paint, String label, float progress, float x, float y) {
        float width = 450;
        float height = 24;
        tempRect.set(x - width/2f, y - height/2f, x + width/2f, y + height/2f);
        
        // Track
        paint.setColor(COLOR_SURFACE_VARIANT);
        canvas.drawRoundRect(tempRect, height/2, height/2, paint);
        
        // Progress
        paint.setColor(COLOR_PRIMARY);
        canvas.drawRoundRect(tempRect.left, tempRect.top, tempRect.left + (width * Math.min(1.0f, progress)), tempRect.bottom, height/2, height/2, paint);
        
        paint.setColor(Color.WHITE);
        paint.setTextSize(26);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(label, x, y - 25, paint);
    }

    // drawBulletButton removed as shooting is now double-tap based

    private void shoot(Player player) {
        if (superBullets > 0) {
            superBullets--;
            prefs.edit().putInt("superBullets", superBullets).apply();
            
            // Screen Clear Effect
            for (Obstacle o : obstacles) {
                triggerExplosion(o.x + o.size/2, o.y + o.size/2);
            }
            obstacles.clear();
            if (vibrator != null) vibrator.vibrate(new long[]{0, 50, 50, 50, 50, 50}, -1);
            return;
        }

        if (bullets <= 0) return;
        
        bullets--;
        prefs.edit().putInt("bullets", bullets).apply();
        // if (soundLoaded) soundPool.play(soundShoot, 1, 1, 0, 0, 1);
        
        // Find closest obstacle in front of player
        Obstacle target = null;
        float minDist = Float.MAX_VALUE;
        for (Obstacle o : obstacles) {
            float distY = player.y - o.y;
            if (distY > 0 && distY < minDist) {
                // Must be in same or adjacent lane for "shooting" logic to feel fair
                if (Math.abs(o.x - player.x) < screenWidth / 2f) {
                    minDist = distY;
                    target = o;
                }
            }
        }
        
        if (target != null) {
            triggerExplosion(target.x + target.size/2, target.y + target.size/2);
            obstacles.remove(target);
            if (vibrator != null) vibrator.vibrate(50);
        } else {
            // Missed shoot (still consume bullet but no effect)
            if (vibrator != null) vibrator.vibrate(20);
        }
    }
    
    public int getBackgroundSurfaceColor() {
        return currentSkin != null ? currentSkin.color : Color.parseColor("#1C1B1F");
    }
}

