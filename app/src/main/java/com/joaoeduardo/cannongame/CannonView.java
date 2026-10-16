package com.joaoeduardo.cannongame;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CannonView extends SurfaceView implements SurfaceHolder.Callback {

    // ── Game rules ──────────────────────────────────────────────────────────────
    private static final int    TARGET_PIECES      = 7;
    private static final int    MISS_PENALTY       = 2;
    private static final int    HIT_REWARD         = 3;
    private static final double INITIAL_GAME_TIME  = 10.0;

    // ── Layout constants (fraction of screen) ───────────────────────────────────
    private static final float CANNON_BASE_RADIUS_PERCENT  = 0.030f;
    private static final float CANNON_BARREL_LENGTH_PERCENT = 0.250f;
    private static final float CANNON_BARREL_WIDTH_PERCENT  = 0.015f;

    private static final float CANNONBALL_RADIUS_PERCENT = 0.020f;
    private static final float CANNONBALL_SPEED_PERCENT  = 0.400f; // screen-widths/s

    private static final float BLOCKER_DISTANCE_PERCENT    = 0.320f;
    private static final float BLOCKER_WIDTH_PERCENT       = 0.014f;
    private static final float BLOCKER_HALF_LENGTH_PERCENT = 0.380f;
    private static final float BLOCKER_SPEED_PERCENT       = 0.400f; // screen-heights/s

    private static final float TARGET_DISTANCE_PERCENT = 0.900f;
    private static final float TARGET_WIDTH_PERCENT    = 0.028f;
    private static final float TARGET_GAP_PERCENT      = 0.020f; // top/bottom gap each side

    // ── Colors ──────────────────────────────────────────────────────────────────
    private static final int[] TARGET_COLORS = {
        Color.YELLOW, Color.CYAN, Color.RED, Color.GREEN,
        Color.MAGENTA, Color.WHITE, Color.BLUE
    };

    // ── Prefs key for high-score ─────────────────────────────────────────────────
    private static final String PREFS_NAME  = "cannon_prefs";
    private static final String KEY_BEST    = "best_shots";

    // ── Screen ───────────────────────────────────────────────────────────────────
    private float screenWidth;
    private float screenHeight;

    // ── Derived measurements ─────────────────────────────────────────────────────
    private float cannonBaseRadius;
    private float cannonBarrelLength;
    private float cannonBarrelWidth;
    private float cannonballRadius;
    private float cannonballSpeed;
    private float blockerWidth;
    private float blockerHalfLength;
    private float blockerSpeed;
    private float targetWidth;
    private float targetSectionHeight;
    private float targetFirstY;

    // ── Cannon state ─────────────────────────────────────────────────────────────
    private float cannonX;
    private float cannonY;
    private float barrelEndX;
    private float barrelEndY;

    // ── Cannonball state ─────────────────────────────────────────────────────────
    private boolean cannonballOnScreen;
    private float   cannonballX;
    private float   cannonballY;
    private float   cannonballVelocityX;
    private float   cannonballVelocityY;

    // ── Blocker state ────────────────────────────────────────────────────────────
    private RectF blockerRect;
    private float blockerVelocity;

    // ── Target state ─────────────────────────────────────────────────────────────
    private RectF[]    targetRects;
    private boolean[]  targetHit;
    private int        targetsRemaining;

    // ── Score / time ─────────────────────────────────────────────────────────────
    private double timeLeft;
    private int    shotsFired;
    private double totalElapsedTime;
    private int    bestShots;

    // ── Flags ────────────────────────────────────────────────────────────────────
    private boolean gameOver;
    private boolean dialogIsDisplayed;
    private boolean surfaceReady;

    // ── Sound ────────────────────────────────────────────────────────────────────
    private SoundPool soundPool;
    private int       cannonSoundId;
    private int       blockerSoundId;
    private int       targetSoundId;

    // ── Paint objects ────────────────────────────────────────────────────────────
    private final Paint backgroundPaint = new Paint();
    private final Paint cannonPaint     = new Paint();
    private final Paint cannonballPaint = new Paint();
    private final Paint blockerPaint    = new Paint();
    private final Paint hudPaint        = new Paint();
    private final Paint timeBarPaint    = new Paint();
    private final Paint[] targetPaints  = new Paint[TARGET_PIECES];

    // ── Game loop thread ─────────────────────────────────────────────────────────
    private CannonThread cannonThread;

    // ────────────────────────────────────────────────────────────────────────────
    public CannonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        initPaints();
        initSound();
        loadBestScore();
    }

    // ── Initialization ───────────────────────────────────────────────────────────

    private void initPaints() {
        backgroundPaint.setColor(Color.WHITE);

        cannonPaint.setColor(Color.BLACK);
        cannonPaint.setAntiAlias(true);

        cannonballPaint.setColor(Color.BLACK);
        cannonballPaint.setAntiAlias(true);

        blockerPaint.setColor(Color.parseColor("#555555"));

        hudPaint.setColor(Color.BLACK);
        hudPaint.setAntiAlias(true);
        hudPaint.setTextAlign(Paint.Align.LEFT);

        for (int i = 0; i < TARGET_PIECES; i++) {
            targetPaints[i] = new Paint();
            targetPaints[i].setColor(TARGET_COLORS[i]);
        }
    }

    private void initSound() {
        AudioAttributes attrs = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();

        soundPool = new SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build();

        cannonSoundId  = soundPool.load(getContext(), R.raw.cannon_fire, 1);
        blockerSoundId = soundPool.load(getContext(), R.raw.blocker_hit, 1);
        targetSoundId  = soundPool.load(getContext(), R.raw.target_hit,  1);
    }

    private void loadBestScore() {
        SharedPreferences prefs = getContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        bestShots = prefs.getInt(KEY_BEST, Integer.MAX_VALUE);
    }

    private void saveBestScore() {
        if (shotsFired < bestShots) {
            bestShots = shotsFired;
            getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_BEST, bestShots).apply();
        }
    }

    // ── SurfaceHolder.Callback ───────────────────────────────────────────────────

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Dimensions not available yet; wait for surfaceChanged.
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        surfaceReady = true;
        screenWidth  = width;
        screenHeight = height;

        computeMeasurements();
        startNewGame();

        if (cannonThread == null || !cannonThread.isAlive()) {
            cannonThread = new CannonThread(holder);
            cannonThread.setRunning(true);
            cannonThread.start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        stopThread();
    }

    // ── Measurements ─────────────────────────────────────────────────────────────

    private void computeMeasurements() {
        float min = Math.min(screenWidth, screenHeight);

        cannonBaseRadius   = CANNON_BASE_RADIUS_PERCENT  * min;
        cannonBarrelLength = CANNON_BARREL_LENGTH_PERCENT * screenWidth;
        cannonBarrelWidth  = CANNON_BARREL_WIDTH_PERCENT  * screenWidth;

        cannonballRadius = CANNONBALL_RADIUS_PERCENT * min;
        cannonballSpeed  = CANNONBALL_SPEED_PERCENT  * screenWidth;

        blockerWidth      = BLOCKER_WIDTH_PERCENT       * screenWidth;
        blockerHalfLength = BLOCKER_HALF_LENGTH_PERCENT * screenHeight;
        blockerSpeed      = BLOCKER_SPEED_PERCENT       * screenHeight;

        targetWidth        = TARGET_WIDTH_PERCENT * screenWidth;
        float gapY         = TARGET_GAP_PERCENT   * screenHeight;
        float totalTargetH = screenHeight - 2 * gapY;
        targetSectionHeight = totalTargetH / TARGET_PIECES;
        targetFirstY        = gapY;

        float textSize = screenWidth / 28f;
        hudPaint.setTextSize(textSize);
    }

    // ── Game lifecycle ───────────────────────────────────────────────────────────

    private void startNewGame() {
        timeLeft         = INITIAL_GAME_TIME;
        shotsFired       = 0;
        totalElapsedTime = 0;
        gameOver         = false;
        targetsRemaining = TARGET_PIECES;

        // Cannon – base at left-center
        cannonX    = 0f;
        cannonY    = screenHeight / 2f;
        barrelEndX = cannonBarrelLength;
        barrelEndY = screenHeight / 2f;

        // Cannonball – off screen initially
        cannonballOnScreen = false;

        // Blocker – centered vertically at its designated X position
        float blockerCx = BLOCKER_DISTANCE_PERCENT * screenWidth;
        blockerRect = new RectF(
            blockerCx - blockerWidth / 2f,
            screenHeight / 2f - blockerHalfLength,
            blockerCx + blockerWidth / 2f,
            screenHeight / 2f + blockerHalfLength
        );
        blockerVelocity = blockerSpeed;

        // Targets
        targetRects = new RectF[TARGET_PIECES];
        targetHit   = new boolean[TARGET_PIECES];
        float targetCx = TARGET_DISTANCE_PERCENT * screenWidth;
        for (int i = 0; i < TARGET_PIECES; i++) {
            float top    = targetFirstY + i * targetSectionHeight;
            float bottom = top + targetSectionHeight;
            targetRects[i] = new RectF(
                targetCx - targetWidth / 2f,
                top,
                targetCx + targetWidth / 2f,
                bottom
            );
        }
    }

    // ── Update logic (called from game thread) ───────────────────────────────────

    private void updatePositions(double elapsedTime) {
        if (gameOver) return;

        timeLeft         -= elapsedTime;
        totalElapsedTime += elapsedTime;

        if (timeLeft <= 0) {
            timeLeft = 0;
            gameOver = true;
            showGameOverDialog(false);
            return;
        }

        // Blocker movement
        float dy = (float) (blockerVelocity * elapsedTime);
        blockerRect.offset(0, dy);
        if (blockerRect.top < 0 && blockerVelocity < 0) {
            blockerVelocity = -blockerVelocity;
            blockerRect.offset(0, -2 * dy);
        } else if (blockerRect.bottom > screenHeight && blockerVelocity > 0) {
            blockerVelocity = -blockerVelocity;
            blockerRect.offset(0, -2 * dy);
        }

        // Cannonball movement
        if (cannonballOnScreen) {
            cannonballX += (float) (cannonballVelocityX * elapsedTime);
            cannonballY += (float) (cannonballVelocityY * elapsedTime);

            // Off top or bottom
            if (cannonballY - cannonballRadius < 0 ||
                cannonballY + cannonballRadius > screenHeight) {
                cannonballOnScreen = false;
                applyPenalty();
            }
            // Off right side – ball passed all targets
            else if (cannonballX - cannonballRadius > screenWidth) {
                cannonballOnScreen = false;
                applyPenalty();
            }
            // Hit blocker
            else if (circleHitsRect(blockerRect)) {
                cannonballVelocityX = -Math.abs(cannonballVelocityX);
                soundPool.play(blockerSoundId, 1f, 1f, 0, 0, 1f);
                applyPenalty();
            }
            // Hit a target
            else {
                for (int i = 0; i < TARGET_PIECES; i++) {
                    if (!targetHit[i] && circleHitsRect(targetRects[i])) {
                        targetHit[i] = true;
                        targetsRemaining--;
                        cannonballOnScreen = false;
                        timeLeft += HIT_REWARD;
                        soundPool.play(targetSoundId, 1f, 1f, 0, 0, 1f);
                        if (targetsRemaining == 0) {
                            gameOver = true;
                            saveBestScore();
                            showGameOverDialog(true);
                        }
                        break;
                    }
                }
            }
        }
    }

    private void applyPenalty() {
        timeLeft -= MISS_PENALTY;
        if (timeLeft < 0) timeLeft = 0;
    }

    private boolean circleHitsRect(RectF rect) {
        return cannonballX + cannonballRadius >= rect.left  &&
               cannonballX - cannonballRadius <= rect.right &&
               cannonballY + cannonballRadius >= rect.top   &&
               cannonballY - cannonballRadius <= rect.bottom;
    }

    // ── Drawing (called from game thread) ────────────────────────────────────────

    private void drawGameElements(Canvas canvas) {
        // Background
        canvas.drawRect(0, 0, screenWidth, screenHeight, backgroundPaint);

        // Targets
        for (int i = 0; i < TARGET_PIECES; i++) {
            if (!targetHit[i]) {
                canvas.drawRect(targetRects[i], targetPaints[i]);
            }
        }

        // Blocker
        canvas.drawRect(blockerRect, blockerPaint);

        // Cannon base
        canvas.drawCircle(cannonX, cannonY, cannonBaseRadius, cannonPaint);

        // Cannon barrel
        cannonPaint.setStrokeWidth(cannonBarrelWidth);
        canvas.drawLine(cannonX, cannonY, barrelEndX, barrelEndY, cannonPaint);

        // Cannonball
        if (cannonballOnScreen) {
            canvas.drawCircle(cannonballX, cannonballY, cannonballRadius, cannonballPaint);
        }

        // HUD
        drawHUD(canvas);

        // Time bar (extra feature)
        drawTimeBar(canvas);
    }

    private void drawHUD(Canvas canvas) {
        float margin = screenWidth * 0.02f;
        float y      = hudPaint.getTextSize() + margin;

        canvas.drawText(
            String.format("Time: %.1f s", timeLeft),
            margin, y, hudPaint
        );
        canvas.drawText(
            String.format("Shots: %d", shotsFired),
            margin, y + hudPaint.getTextSize() + margin * 0.5f, hudPaint
        );

        if (bestShots != Integer.MAX_VALUE) {
            String best = String.format("Best: %d shots", bestShots);
            hudPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(best, screenWidth - margin, y, hudPaint);
            hudPaint.setTextAlign(Paint.Align.LEFT);
        }
    }

    // ── Extra feature: color-coded time bar at bottom of screen ─────────────────
    private void drawTimeBar(Canvas canvas) {
        float ratio    = (float) Math.min(1.0, timeLeft / INITIAL_GAME_TIME);
        float barH     = screenHeight * 0.018f;
        float barWidth = ratio * screenWidth;

        int r = (int) ((1f - ratio) * 255);
        int g = (int) (ratio        * 255);
        timeBarPaint.setColor(Color.rgb(r, g, 0));
        canvas.drawRect(0, screenHeight - barH, barWidth, screenHeight, timeBarPaint);
    }

    // ── Touch handling ───────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return true;

        if (gameOver) return true;

        float touchX = event.getX();
        float touchY = event.getY();

        double angle = Math.atan2(touchY - cannonY, touchX - cannonX);
        barrelEndX = cannonX + (float) (cannonBarrelLength * Math.cos(angle));
        barrelEndY = cannonY + (float) (cannonBarrelLength * Math.sin(angle));

        if (!cannonballOnScreen) {
            cannonballOnScreen  = true;
            cannonballX         = barrelEndX;
            cannonballY         = barrelEndY;
            cannonballVelocityX = (float) (cannonballSpeed * Math.cos(angle));
            cannonballVelocityY = (float) (cannonballSpeed * Math.sin(angle));
            shotsFired++;
            soundPool.play(cannonSoundId, 1f, 1f, 0, 0, 1f);
        }

        return true;
    }

    // ── Game-over dialog ─────────────────────────────────────────────────────────

    private void showGameOverDialog(final boolean playerWon) {
        if (dialogIsDisplayed) return;
        dialogIsDisplayed = true;

        String title   = getContext().getString(playerWon ? R.string.you_won : R.string.you_lost);
        String message = getContext().getString(
            playerWon ? R.string.win_message : R.string.lose_message,
            shotsFired, (int) totalElapsedTime
        );
        if (playerWon && bestShots != Integer.MAX_VALUE) {
            message += "\n" + getContext().getString(R.string.best_shots, bestShots);
        }

        final String finalMessage = message;
        ((Activity) getContext()).runOnUiThread(() -> new AlertDialog.Builder(getContext())
            .setTitle(title)
            .setMessage(finalMessage)
            .setCancelable(false)
            .setPositiveButton(R.string.play_again, (dialog, which) -> {
                dialogIsDisplayed = false;
                startNewGame();
            })
            .show()
        );
    }

    // ── Public lifecycle controls ────────────────────────────────────────────────

    public void stopGame() {
        stopThread();
    }

    public void resumeGame() {
        if (surfaceReady && (cannonThread == null || !cannonThread.isAlive())) {
            cannonThread = new CannonThread(getHolder());
            cannonThread.setRunning(true);
            cannonThread.start();
        }
    }

    public void releaseResources() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    // ── Thread helper ────────────────────────────────────────────────────────────

    private void stopThread() {
        if (cannonThread == null) return;
        cannonThread.setRunning(false);
        boolean retry = true;
        while (retry) {
            try {
                cannonThread.join();
                retry = false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        cannonThread = null;
    }

    // ── Inner class: Game loop ───────────────────────────────────────────────────

    private class CannonThread extends Thread {

        private final SurfaceHolder surfaceHolder;
        private volatile boolean running;

        CannonThread(SurfaceHolder holder) {
            surfaceHolder = holder;
            setName("CannonThread");
        }

        void setRunning(boolean running) {
            this.running = running;
        }

        @Override
        public void run() {
            long previousTimeMs = System.currentTimeMillis();

            while (running) {
                Canvas canvas = null;
                try {
                    canvas = surfaceHolder.lockCanvas(null);
                    if (canvas == null) continue;

                    synchronized (surfaceHolder) {
                        long nowMs      = System.currentTimeMillis();
                        double elapsed  = (nowMs - previousTimeMs) / 1000.0;
                        previousTimeMs  = nowMs;

                        updatePositions(elapsed);
                        drawGameElements(canvas);
                    }
                } finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
    }
}
