package edu.ucsd.cse110.cse_110_project_cse_110_team_9.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;

import com.google.android.material.color.MaterialColors;

import edu.ucsd.cse110.cse_110_project_cse_110_team_9.Constants;
import edu.ucsd.cse110.cse_110_project_cse_110_team_9.R;


public class RingView extends View {
    private Paint mPaint;
    private Canvas mCanvas;
    private Constants.scale zoomLevel;
    private int mWidth;
    private int mHeight;


    private void init(Context context) {

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        int strokeWidth = 4;
        mPaint.setStrokeWidth(strokeWidth);
        mPaint.setARGB(100, 106, 108, 110);
    }

    public RingView(Context context) {
        super(context);
        init(context);
    }

    public RingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void setZoomObserver(LiveData<Constants.scale> zoomLevel, LifecycleOwner owner) {
        zoomLevel.observe(owner, this::onScaleChanged);
    }

    public Constants.scale getZoomLevel() {
        return zoomLevel;
    }

    private void onScaleChanged(Constants.scale zoomLevel) {
        this.zoomLevel = zoomLevel;
        invalidate();
        requestLayout();
    }

    public RingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        View parent = (View) getParent();
        int width = parent.getWidth();

        // Log.d("RingView", Integer.toString(width));
        float radius = (float) width / 2 - Constants.OUTER_RING_PADDING;


        //canvas.drawCircle((float)(0.5 * width),(float)(0.5*width),radius, mPaint);

        // canvas.save();
        canvas.drawCircle((float) getWidth() / 2, (float) getHeight() / 2, radius, mPaint);
        // Log.d("Ring", Float.toString(radius));

        if (!isInEditMode()) {
            switch (zoomLevel) {
                case ONE:
                    //
                    break;
                case TEN:
                    canvas.drawCircle((float) getWidth() / 2, (float) getHeight() / 2, (float) radius / 2,
                            mPaint);
                    //  Log.d("Ring10", Float.toString(radius/2));

                    break;
                case FIVE_HUNDRED:
                    canvas.drawCircle((float) getWidth() / 2, (float) getHeight() / 2,
                            (float) 2 * radius / 3, mPaint);
                    canvas.drawCircle((float) getWidth() / 2, (float) getHeight() / 2, (float) radius / 3,
                            mPaint);
                    //  Log.d("Ring500", Float.toString(radius/3));

                    break;
                case FIVE_HUNDRED_PLUS:
                    canvas.drawCircle((float) getWidth() / 2, (float) getHeight() / 2, (float) radius / 4,
                            mPaint);
                    canvas.drawCircle((float) getWidth() / 2, (float) getHeight() / 2,
                            (float) radius / 2,
                            mPaint);

                    canvas.drawCircle((float) getWidth() / 2, (float) getHeight() / 2,
                            (float) 3 * radius / 4,
                            mPaint);
                    break;
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        mHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(mWidth, mHeight);
    }

    public void setZoom(Constants.scale zoom) {
        invalidate();    // force view to be re-drawn
        requestLayout();
    }
}