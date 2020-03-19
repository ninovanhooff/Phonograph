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

import androidx.annotation.Nullable;

import static java.lang.Math.log10;

public class LevelsView extends View {
    private static final int[] GRADIENT_COLORS = { Color.RED, Color.YELLOW, Color.GREEN};
    /** Strongest attenuation on the scale */
    private static final float DB_FLOOR = -48;
    private static final long PEAK_HOLD_MILLIS = 800L;

    private float currentDb = DB_FLOOR;
    private float peakDb = currentDb;

    private Rect viewBounds = new Rect();
    /** Textbounds of a dB label */
    private Rect textBounds = new Rect();

    private Paint barPaint;
    private Paint textPaint;

    /** A timer which fires when the peak level should not be maintained any more */
    private Timer peakTimer;
    private int barWidth;

    private float oneDp = AndroidUtils.dpToPx(1);

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

    public void setAmplitude(int amp){
        setCurrentDb(convertAmpToDb(amp));
    }

    public void setCurrentDb(float db) {
        db = -Math.abs(db);
        db = Math.min(0, Math.max(DB_FLOOR, db));
        // smooth out short silences while keeping quick response to loudness
        if (db > currentDb){
            currentDb = db;
        } else {
            currentDb = (currentDb + db) / 2;
        }
        if (currentDb > peakDb) {
            peakDb = currentDb;
            if (peakTimer != null) {
                peakTimer.cancel();
            }
            peakTimer = new Timer();
            peakTimer.schedule(
                    new TimerTask() {
                                   @Override
                                   public void run() {
                                       peakDb = currentDb;
                                   }
                               }
                    , PEAK_HOLD_MILLIS);
        }
        invalidate();
    }

    private void initialize() {
        setFocusable(false);
        barPaint = new Paint();
        textPaint = new TextPaint();
        textPaint.setTextSize(AndroidUtils.dpToPx(12));
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE); //todo styled color
        int vPad = (int) AndroidUtils.dpToPx(12);
        int hPad = (int) AndroidUtils.dpToPx(12);
        setPadding(hPad, vPad, hPad, vPad); // todo define in xml or something

        // stereo
        barWidth = 40;
    }

    private float convertAmpToDb(int amp){
        amp = Math.abs(amp);
        return (float) (20 * log10(amp / 32767.0d));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // left level bar
        canvas.drawRect(getPaddingLeft(),dbYCoordinate(currentDb),getPaddingLeft() + barWidth,viewBounds.bottom - getPaddingBottom(), barPaint);

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
    }

    private float dbYCoordinate(float db) {
        return getPaddingTop() + db / DB_FLOOR * (viewBounds.height() - getPaddingTop() - getPaddingBottom());
    }

    // called when the dimensions of the View change
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        viewBounds.set(0, 0, w, h);
        LinearGradient gradient = new LinearGradient(0, 0, w, h, GRADIENT_COLORS, null, Shader.TileMode.CLAMP);
        barPaint.setShader(gradient);
    }
}
