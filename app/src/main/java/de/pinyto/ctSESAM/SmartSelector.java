package de.pinyto.ctSESAM;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import java.util.HashMap;

/**
 * TODO: document your custom view class.
 */
public class SmartSelector extends View {
    private int tileHeight = 60;
    private int contentWidth;
    private int contentHeight;
    private Rect wholeCanvasRect;
    private Paint backgroundPaint, tilePaint;
    private int[][] colorMatrix;
    private int minLength = 4;
    private int maxLength = 32;
    private int digit_count = 10;
    private int lower_count = 36;
    private int upper_count = 36;
    private int extra_count = 24;
    private int selectedComplexity = -1;
    private int selectedLength = -1;
    OnStrengthSelectedEventListener StrengthSelectedListener;

    public SmartSelector(Context context) {
        super(context);
        init(null, 0);
    }

    public SmartSelector(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public SmartSelector(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.SmartSelector, defStyle, 0);
        a.recycle();
        // Initialize drawing environment
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(0xff000000);
        backgroundPaint.setStyle(Paint.Style.FILL);
        tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tilePaint.setColor(0xff0000aa);
        tilePaint.setStyle(Paint.Style.FILL);
        calculateColorMatrix();
    }

    public interface OnStrengthSelectedEventListener {
        void onStrengthSelected(int length, int complexity);
    }

    public void setOnStrengthSelectedEventListener(OnStrengthSelectedEventListener eventListener) {
        StrengthSelectedListener = eventListener;
    }

    private void calculatePadding() {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        contentWidth = getWidth() - paddingLeft - paddingRight;
        contentHeight = getHeight() - paddingTop - paddingBottom;
        wholeCanvasRect = new Rect(0, 0, contentWidth, contentHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        calculatePadding();
    }

    private void calculateColorMatrix() {
        int[] complexity = new int[]{
                digit_count,
                lower_count,
                upper_count,
                digit_count + lower_count,
                lower_count + upper_count,
                lower_count + upper_count + digit_count,
                lower_count + upper_count + digit_count + extra_count};
        colorMatrix = new int[maxLength-minLength+1][complexity.length];
        for (int i = 0; i < maxLength-minLength+1; i++) {
            for (int j=0; j < complexity.length; j++) {
                double s = 20;
                double tianhe2_years = (Math.pow(complexity[j], (i+minLength))*0.4/3120000)/
                        (60*60*24*365);
                double strength_red = 1-s/(s+Math.log(tianhe2_years+1)/Math.log(50));
                double strength_green = 1-s/(s+Math.log(tianhe2_years+1)/Math.log(1.2));
                int redValue = (int) Math.round(215*(1-strength_red));
                int greenValue = (int) Math.round(190*strength_green);
                colorMatrix[i][j] = Color.argb(0xff, redValue, greenValue, 0);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(wholeCanvasRect, backgroundPaint);
        float tileWidth = (float) (contentWidth-1) / (maxLength-minLength+1);
        for (int i = 0; i < maxLength-minLength+1; i++) {
            for (int j = 0; j < colorMatrix[0].length; j++) {
                if (i == selectedLength && j==selectedComplexity) {
                    tilePaint.setColor(0xffffffff);
                } else {
                    tilePaint.setColor(colorMatrix[i][colorMatrix[0].length - 1 - j]);
                }
                canvas.drawRect(1+i*tileWidth, 1+j*tileHeight,
                        1+i*tileWidth+tileWidth-1, 1+j*tileHeight+tileHeight-1,
                        tilePaint);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int minw = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
        int w = resolveSizeAndState(minw, widthMeasureSpec, 1);
        int minh = getPaddingBottom() + getPaddingTop() + 7 * tileHeight + 1;
        int h = resolveSizeAndState(minh, heightMeasureSpec, 1);
        setMeasuredDimension(w, h);
        calculatePadding();
        postInvalidate();
    }

    private void selectTile(float x, float y) {
        float tileWidth = (float) (contentWidth-1) / (maxLength-minLength+1);
        selectedLength = (int) (x / tileWidth);
        selectedComplexity = (int) (y / tileHeight);
        if (StrengthSelectedListener != null) {
            StrengthSelectedListener.onStrengthSelected(minLength+selectedLength,
                    colorMatrix[0].length-1-selectedComplexity);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch(action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_MOVE:
                //event.getPointerId(0);
                float x = event.getX(0);
                float y = event.getY(0);
                selectTile(x, y);
                postInvalidate();
                performClick();
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
