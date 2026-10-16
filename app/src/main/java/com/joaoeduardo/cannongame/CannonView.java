package com.joaoeduardo.cannongame;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
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

    private static final int    TARGET_PIECES      = 7;
    private static final int    MISS_PENALTY       = 2;
    private static final int    HIT_REWARD         = 3;
    private static final double INITIAL_GAME_TIME  = 25.0;

    private static final float CANNON_BASE_RADIUS_PERCENT   = 0.032f;
    private static final float CANNON_BARREL_LENGTH_PERCENT = 0.220f;
    private static final float CANNON_BARREL_WIDTH_PERCENT  = 0.016f;

    private static final float CANNONBALL_RADIUS_PERCENT = 0.018f;
    private static final float CANNONBALL_SPEED_PERCENT  = 0.650f;

    private static final float BLOCKER_DISTANCE_PERCENT    = 0.350f;
    private static final float BLOCKER_WIDTH_PERCENT       = 0.016f;
    private static final float BLOCKER_HALF_LENGTH_PERCENT = 0.140f;
    private static final float BLOCKER_SPEED_PERCENT       = 0.350f;

    private static final float TARGET_DISTANCE_PERCENT = 0.910f;
    private static final float TARGET_WIDTH_PERCENT    = 0.026f;
    private static final float TARGET_GAP_PERCENT      = 0.040f;

    private static final int[] TARGET_COLORS = {
        Color.parseColor("#EF4444"),
        Color.parseColor("#F97316"),
        Color.parseColor("#FACC15"),
        Color.parseColor("#22C55E"),
        Color.parseColor("#06B6D4"),
        Color.parseColor("#3B82F6"),
        Color.parseColor("#A855F7")
    };

    private static final String PREFS_NAME = "cannon_prefs";
    private static final String KEY_BEST   = "best_shots";

    private float screenWidth;
    private float screenHeight;

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

    private float cannonX;
    private float cannonY;
    private float barrelEndX;
    private float barrelEndY;

    private boolean cannonballOnScreen;
    private float   cannonballX;
    private float   cannonballY;
    private float   cannonballVelocityX;
    private float   cannonballVelocityY;

    private RectF blockerRect;
    private float blockerVelocity;

    private RectF[]   targetRects;
    private boolean[] targetHit;
    private int       targetsRemaining;

    private double timeLeft;
    private int    shotsFired;
    private double totalElapsedTime;
    private int    bestShots;

    private boolean gameOver;
    private boolean dialogIsDisplayed;
    private boolean surfaceReady;

    private SoundPool soundPool;
    private int       cannonSoundId;
    private int       blockerSoundId;
    private int       targetSoundId;

    private final Paint backgroundPaint  = new Paint();
    private final Paint cannonBasePaint  = new Paint();
    private final Paint cannonPivotPaint = new Paint();
    private final Paint cannonBarrelPaint= new Paint();
    private final Paint cannonballPaint  = new Paint();
    private final Paint cannonballGlow   = new Paint();
    private final Paint blockerPaint     = new Paint();
    private final Paint blockerGlowPaint = new Paint();
    private final Paint hudPaint         = new Paint();
    private final Paint hudSecondaryPaint= new Paint();
    private final Paint timeBarBgPaint   = new Paint();
    private final Paint timeBarPaint     = new Paint();
    private final Paint[] targetPaints   = new Paint[TARGET_PIECES];

    private CannonThread cannonThread;

    public CannonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        initPaints();
        initSound();
        loadBestScore();
    }

    private void initPaints() {
        backgroundPaint.setColor(Color.parseColor("#0F172A"));

        cannonBasePaint.setColor(Color.parseColor("#475569"));
        cannonBasePaint.setAntiAlias(true);

        cannonPivotPaint.setColor(Color.parseColor("#38BDF8"));
        cannonPivotPaint.setAntiAlias(true);

        cannonBarrelPaint.setColor(Color.parseColor("#94A3B8"));
        cannonBarrelPaint.setAntiAlias(true);
        cannonBarrelPaint.setStrokeCap(Paint.Cap.ROUND);

        cannonballPaint.setColor(Color.parseColor("#F59E0B"));
        cannonballPaint.setAntiAlias(true);

        cannonballGlow.setColor(Color.parseColor("#FEF08A"));
        cannonballGlow.setAntiAlias(true);

        blockerPaint.setColor(Color.parseColor("#E11D48"));
        blockerPaint.setAntiAlias(true);

        blockerGlowPaint.setColor(Color.parseColor("#FB7185"));
        blockerGlowPaint.setStyle(Paint.Style.STROKE);
        blockerGlowPaint.setAntiAlias(true);

        hudPaint.setColor(Color.WHITE);
        hudPaint.setAntiAlias(true);
        hudPaint.setFakeBoldText(true);
        hudPaint.setTextAlign(Paint.Align.LEFT);

        hudSecondaryPaint.setColor(Color.parseColor("#94A3B8"));
        hudSecondaryPaint.setAntiAlias(true);
        hudSecondaryPaint.setTextAlign(Paint.Align.RIGHT);

        timeBarBgPaint.setColor(Color.parseColor("#1E293B"));

        for (int i = 0; i < TARGET_PIECES; i++) {
            targetPaints[i] = new Paint();
            targetPaints[i].setColor(TARGET_COLORS[i]);
            targetPaints[i].setAntiAlias(true);
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
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        bestShots = prefs.getInt(KEY_BEST, Integer.MAX_VALUE);
    }

    private void saveBestScore() {
        if (shotsFired < bestShots) {
            bestShots = shotsFired;
            getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_BEST, bestShots).apply();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
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

    private void computeMeasurements() {
        float min = Math.min(screenWidth, screenHeight);

        cannonBaseRadius   = CANNON_BASE_RADIUS_PERCENT   * min;
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

        float textSize = screenWidth / 30f;
        hudPaint.setTextSize(textSize);
        hudSecondaryPaint.setTextSize(textSize * 0.85f);
        blockerGlowPaint.setStrokeWidth(screenWidth * 0.003f);
    }

    private void startNewGame() {
        timeLeft         = INITIAL_GAME_TIME;
        shotsFired       = 0;
        totalElapsedTime = 0;
        gameOver         = false;
        targetsRemaining = TARGET_PIECES;

        cannonX    = 0f;
        cannonY    = screenHeight / 2f;
        barrelEndX = cannonBarrelLength;
        barrelEndY = screenHeight / 2f;

        cannonballOnScreen = false;

        float blockerCx = BLOCKER_DISTANCE_PERCENT * screenWidth;
        blockerRect = new RectF(
            blockerCx - blockerWidth / 2f,
            screenHeight / 2f - blockerHalfLength,
            blockerCx + blockerWidth / 2f,
            screenHeight / 2f + blockerHalfLength
        );
        blockerVelocity = blockerSpeed;

        targetRects = new RectF[TARGET_PIECES];
        targetHit   = new boolean[TARGET_PIECES];
        float targetCx = TARGET_DISTANCE_PERCENT * screenWidth;
        float targetMarginY = targetSectionHeight * 0.08f;

        for (int i = 0; i < TARGET_PIECES; i++) {
            float top    = targetFirstY + i * targetSectionHeight + targetMarginY;
            float bottom = targetFirstY + (i + 1) * targetSectionHeight - targetMarginY;
            targetRects[i] = new RectF(
                targetCx - targetWidth / 2f,
                top,
                targetCx + targetWidth / 2f,
                bottom
            );
        }
    }

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

        float dy = (float) (blockerVelocity * elapsedTime);
        blockerRect.offset(0, dy);
        if (blockerRect.top < 0 && blockerVelocity < 0) {
            blockerVelocity = -blockerVelocity;
            blockerRect.offset(0, -2 * dy);
        } else if (blockerRect.bottom > screenHeight && blockerVelocity > 0) {
            blockerVelocity = -blockerVelocity;
            blockerRect.offset(0, -2 * dy);
        }

        if (cannonballOnScreen) {
            cannonballX += (float) (cannonballVelocityX * elapsedTime);
            cannonballY += (float) (cannonballVelocityY * elapsedTime);

            if (cannonballY - cannonballRadius < 0 ||
                cannonballY + cannonballRadius > screenHeight) {
                cannonballOnScreen = false;
                applyPenalty();
            } else if (cannonballX - cannonballRadius > screenWidth) {
                cannonballOnScreen = false;
                applyPenalty();
            } else if (circleHitsRect(blockerRect)) {
                cannonballVelocityX = -Math.abs(cannonballVelocityX);
                soundPool.play(blockerSoundId, 1f, 1f, 0, 0, 1f);
                applyPenalty();
            } else {
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

    private void drawGameElements(Canvas canvas) {
        canvas.drawRect(0, 0, screenWidth, screenHeight, backgroundPaint);

        float targetRadius = targetWidth * 0.4f;
        for (int i = 0; i < TARGET_PIECES; i++) {
            if (!targetHit[i]) {
                canvas.drawRoundRect(targetRects[i], targetRadius, targetRadius, targetPaints[i]);
            }
        }

        float blockerRadius = blockerWidth * 0.5f;
        canvas.drawRoundRect(blockerRect, blockerRadius, blockerRadius, blockerPaint);
        canvas.drawRoundRect(blockerRect, blockerRadius, blockerRadius, blockerGlowPaint);

        cannonBarrelPaint.setStrokeWidth(cannonBarrelWidth);
        canvas.drawLine(cannonX, cannonY, barrelEndX, barrelEndY, cannonBarrelPaint);

        canvas.drawCircle(cannonX, cannonY, cannonBaseRadius, cannonBasePaint);
        canvas.drawCircle(cannonX, cannonY, cannonBaseRadius * 0.45f, cannonPivotPaint);

        if (cannonballOnScreen) {
            canvas.drawCircle(cannonballX, cannonballY, cannonballRadius * 1.3f, cannonballGlow);
            canvas.drawCircle(cannonballX, cannonballY, cannonballRadius, cannonballPaint);
        }

        drawHUD(canvas);
        drawTimeBar(canvas);
    }

    private void drawHUD(Canvas canvas) {
        float margin = screenWidth * 0.025f;
        float y      = hudPaint.getTextSize() + margin * 0.8f;

        canvas.drawText(String.format("Time: %.1f s", timeLeft), margin, y, hudPaint);
        canvas.drawText(
            String.format("Shots: %d  |  Remaining: %d", shotsFired, targetsRemaining),
            margin,
            y + hudPaint.getTextSize() * 1.25f,
            hudPaint
        );

        if (bestShots != Integer.MAX_VALUE) {
            canvas.drawText(
                String.format("Record: %d shots", bestShots),
                screenWidth - margin,
                y,
                hudSecondaryPaint
            );
        }
    }

    private void drawTimeBar(Canvas canvas) {
        float ratio    = (float) Math.min(1.0, timeLeft / INITIAL_GAME_TIME);
        float barH     = screenHeight * 0.016f;
        float barWidth = ratio * screenWidth;

        canvas.drawRect(0, screenHeight - barH, screenWidth, screenHeight, timeBarBgPaint);

        int r = (int) (Math.max(0f, Math.min(1f, 2f * (1f - ratio))) * 255);
        int g = (int) (Math.max(0f, Math.min(1f, 2f * ratio)) * 255);
        timeBarPaint.setColor(Color.rgb(r, g, 40));

        canvas.drawRect(0, screenHeight - barH, barWidth, screenHeight, timeBarPaint);
    }

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

    private Activity getActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

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
        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing()) {
            activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
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
    }

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
                    if (canvas == null) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ignored) {}
                        continue;
                    }

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
