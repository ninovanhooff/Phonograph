package com.ninovanhooff.phonograph.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.ninovanhooff.phonograph.util.AndroidUtils;

import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import static java.lang.Math.log10;

public class LevelsView extends View {
    /** Determines how slowly the meter level will decay after receiving a lower amplitude than the
     * last observed. Higher values cause slower decay. */
    public static int DEFAULT_HOLD_FACTOR = 50;
    private static final int[] GRADIENT_COLORS = { Color.RED, Color.YELLOW, Color.GREEN}; //todo refine colors
    /** Strongest attenuation on the scale, considered silent. */
    private static final float DB_FLOOR = -48f;
    private static final long PEAK_HOLD_MILLIS = 1500L;
    /** Limit above which clipping is assumed. The assumption here is that measurements are not
     * accurate, so even levels below 0dB can be considered to clip */ 
    private static final float CLIP_LIMIT = -3f;
    /** For how long the clipping status should be shown after the signal stops clipping */
    private static final long CLIP_HOLD_MILLIS = 3000L;

    // per-channel properties
    private float currentDb = DB_FLOOR;
    private float peakDb = currentDb;
    private boolean isClipping = false;
    /** Fires when the peak level should not be maintained any more */
    private Timer peakTimer;
    /** Fires when the clipping status should not be maintained anymore */
    private Timer clipTimer;

    private Rect viewBounds = new Rect();
    /** Bounds of a dB label */
    private Rect textBounds = new Rect();

    private Paint barPaint;
    private Paint textPaint;
    @ColorInt
    private int textColor = Color.WHITE;  //todo styled color

    private int barWidth;

    private float oneDp = AndroidUtils.dpToPx(1);

    /** Fires when no new level is set for some time */
    private Timer decayTimer;
    private int holdFactor = LevelsView.DEFAULT_HOLD_FACTOR;

    public LevelsView(Context context) {
        this(context, null, 0);
    }

    public LevelsView(Context context, @Nullable AttributeSet attrs){
        this(context, attrs, 0);
    }

    public LevelsView(Context context, @Nullable AttributeSet attrs, int deStyleAttr){
        super(context, attrs, deStyleAttr);
        initialize();
    }

    public void setAmplitude(int amp) {
        setAmplitude(amp, true);
    }

    public void setAmplitude(int amp, boolean invalidate){
        setCurrentDb(convertAmpToDb(amp), invalidate);
    }

    public void setCurrentDb(float db) {
        setCurrentDb(db, true);
    }

    public void setCurrentDb(float db, boolean invalidate) {
        db = -Math.abs(db);
        db = Math.min(0, Math.max(DB_FLOOR, db));
        // smooth out short silences while keeping quick response to loudness
        if (db > currentDb){
            currentDb = db;
        } else {
            currentDb = (currentDb * holdFactor + db) / (holdFactor + 1);
        }
        if (currentDb > CLIP_LIMIT){
            isClipping = true;
        }
        
        if (currentDb > CLIP_LIMIT){
            isClipping = true;
            resetClipTimer();
        }
        
        if (currentDb > peakDb) {
            peakDb = currentDb;
            resetPeakTimer();
        }

        if (invalidate){
            postInvalidate();
        }
    }


    /** Determines how slowly the meter level will decay after receiving a lower amplitude than the
     * last observed. Higher values cause slower decay. See {@link LevelsView#DEFAULT_HOLD_FACTOR}*/
    @SuppressWarnings("unused") // API
    public void setHoldFactor(int holdFactor) {
        this.holdFactor = holdFactor;
    }

    private void initialize() {
        setFocusable(false);
        barPaint = new Paint();
        textPaint = new TextPaint();
        textPaint.setTextSize(AndroidUtils.dpToPx(12));
        textPaint.setAntiAlias(true);
        textPaint.setColor(textColor);
        int vPad = (int) AndroidUtils.dpToPx(12);
        int hPad = (int) AndroidUtils.dpToPx(12);
        setPadding(hPad, vPad, hPad, vPad); // todo define in xml or something

        // stereo
        barWidth = 40;
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // fill bar area with clip color if clipping
        if (isClipping){
            textPaint.setColor(ColorUtils.setAlphaComponent(GRADIENT_COLORS[0], 127));
            canvas.drawRect(getPaddingLeft(),dbYCoordinate(0f),getPaddingLeft() + barWidth,viewBounds.bottom - getPaddingBottom(), textPaint);
            textPaint.setColor(textColor);
        }

        // left level bar
        canvas.drawRect(getPaddingLeft(),Math.round(dbYCoordinate(currentDb)),getPaddingLeft() + barWidth,viewBounds.bottom - getPaddingBottom(), barPaint);

        //peak level bar
        canvas.drawRect(getPaddingLeft(),dbYCoordinate(peakDb) - oneDp * 2,getPaddingLeft() + barWidth + oneDp * 2,dbYCoordinate(peakDb), barPaint);

        float stepHeight;
        for (int i = 0; i <= -DB_FLOOR; i += 6){
            String text = Integer.toString(i);
            textPaint.getTextBounds(text, 0, text.length(), textBounds);

            stepHeight = dbYCoordinate(-i);
            canvas.drawRect(
                    getPaddingLeft() + barWidth + oneDp,
                    stepHeight - oneDp * .5f,
                    viewBounds.width() / 2 - textBounds.width() / 2 - oneDp,
                    stepHeight + oneDp * .5f,
                    textPaint);
            canvas.drawText(
                    text,
                    viewBounds.width() / 2 - textBounds.width() / 2,
                    stepHeight - textBounds.exactCenterY(),
                    textPaint);
        }

        if (currentDb > DB_FLOOR){
            resetDecayTimer();
        }
    }

    // called when the dimensions of the View change
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        viewBounds.set(0, 0, w, h);
        LinearGradient gradient = new LinearGradient(0, 0, w, h, GRADIENT_COLORS, null, Shader.TileMode.CLAMP); //todo positions
        barPaint.setShader(gradient);
    }

    private float convertAmpToDb(int amp){
        amp = Math.abs(amp);
        return (float) (20 * log10(amp / 32767.0d));
    }

    private float dbYCoordinate(float db) {
        return getPaddingTop() + db / DB_FLOOR * (viewBounds.height() - getPaddingTop() - getPaddingBottom());
    }

    private void resetClipTimer() {
        if (clipTimer != null) {
            clipTimer.cancel();
        }
        clipTimer = new Timer();
        clipTimer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        isClipping = false;
                        postInvalidate();
                    }
                }
                , CLIP_HOLD_MILLIS);
    }

    private void resetPeakTimer() {
        if (peakTimer != null) {
            peakTimer.cancel();
        }
        peakTimer = new Timer();
        peakTimer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        peakDb = currentDb;
                        postInvalidate();
                    }
                }
                , PEAK_HOLD_MILLIS);
    }

    private void resetDecayTimer() {
        if (decayTimer != null){
            decayTimer.cancel();
        }
        decayTimer = new Timer();
        decayTimer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        if (currentDb == DB_FLOOR){
                            return;
                        }
                        if (Math.abs(currentDb - DB_FLOOR) < 1f){
                            currentDb = DB_FLOOR;
                            peakDb = DB_FLOOR;
                            return;
                        }
                        setCurrentDb(DB_FLOOR); // simulate a silent input, which causes decay
                    }
                }
                , 12L); // fire before the next frame @60fps
    }
}
