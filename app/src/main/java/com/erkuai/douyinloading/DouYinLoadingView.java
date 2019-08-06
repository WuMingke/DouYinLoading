package com.erkuai.douyinloading;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

/**
 * Created by Administrator on 2019/8/6.
 */

public class DouYinLoadingView extends View {

    private final float mRadius = dp2px(6);
    private final float mGap = dp2px(0.8f);
    private static final float RTL_SCALE = 0.7f;
    private static final float LTR_SCALE = 1.3f;
    private static final int LEFT_COLOR = 0XFFFF4040;
    private static final int RIGHT_COLOR = 0XFF00EEEE;
    private static final int MIX_COLOR = Color.BLACK;
    private static final int DURATION = 350;
    private static final int PAUSE_DURATION = 80;
    private static final float SCALE_START_FRACTION = 0.2f;
    private static final float SCALE_END_FRACTION = 0.8f;

    private float leftRadius;
    private float rightRadius;
    private float gap;//小球间隔
    private float rtlScale;//从右到左缩放
    private float ltrScale;//从左到右缩放
    private int leftColor;
    private int rightColor;
    private int mixColor;
    private int duration;//小球一次移动时长
    private int pauseDuration;//小球移动一次后停顿时长
    private float scaleStartFraction; //小球一次移动期间，进度在[0,scaleStartFraction]期间根据rtlScale、ltrScale逐渐缩放，取值为[0,0.5]
    private float scaleEndFraction;//小球一次移动期间，进度在[scaleEndFraction,1]期间逐渐恢复初始大小,取值为[0.5,1]

    private Paint leftPaint, rightPaint, mixPaint;
    private Path ltrPath, rtlPath, mixPath;
    private float distance;//小球一次移动距离(即两球圆点之间距离）

    private ValueAnimator animator;
    private float fraction;
    private boolean isAnimationCanceled;
    private boolean isLtr = true;

    public DouYinLoadingView(Context context) {
        this(context, null);
    }

    public DouYinLoadingView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DouYinLoadingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DouYinLoadingView);
        leftRadius = typedArray.getDimension(R.styleable.DouYinLoadingView_leftRadius, mRadius);
        rightRadius = typedArray.getDimension(R.styleable.DouYinLoadingView_rightRadius, mRadius);
        gap = typedArray.getDimension(R.styleable.DouYinLoadingView_gap, mGap);
        rtlScale = typedArray.getFloat(R.styleable.DouYinLoadingView_rtlScale, RTL_SCALE);
        ltrScale = typedArray.getFloat(R.styleable.DouYinLoadingView_ltrScale, LTR_SCALE);
        leftColor = typedArray.getColor(R.styleable.DouYinLoadingView_leftColor, LEFT_COLOR);
        rightColor = typedArray.getColor(R.styleable.DouYinLoadingView_rightColor, RIGHT_COLOR);
        mixColor = typedArray.getColor(R.styleable.DouYinLoadingView_mixColor, MIX_COLOR);
        duration = typedArray.getInt(R.styleable.DouYinLoadingView_duration, DURATION);
        pauseDuration = typedArray.getInt(R.styleable.DouYinLoadingView_pauseDuration, PAUSE_DURATION);
        scaleStartFraction = typedArray.getFloat(R.styleable.DouYinLoadingView_scaleStartFraction, SCALE_START_FRACTION);
        scaleEndFraction = typedArray.getFloat(R.styleable.DouYinLoadingView_scaleEndFraction, SCALE_END_FRACTION);
        typedArray.recycle();

        checkAttr();
        distance = gap + leftRadius + rightRadius;

        initDraw();
        initAnim();

    }

    private void initAnim() {
        fraction = 0.0f;
        stop();
        animator = ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.setDuration(duration);
        if (pauseDuration > 0) {
            animator.setStartDelay(pauseDuration);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
        } else {
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.RESTART);
            animator.setInterpolator(new LinearInterpolator());
        }
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                fraction = animation.getAnimatedFraction();
                invalidate();
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                isLtr = !isLtr;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isAnimationCanceled) {
                    animation.start();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isAnimationCanceled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                isLtr = !isLtr;
            }
        });

        animator.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        float max = Math.max(rtlScale, ltrScale);
        max = Math.max(max, 1);

        if (widthMode != MeasureSpec.EXACTLY) {
            widthSize = (int) (gap + (leftRadius * 2 + rightRadius * 2) * max + dp2px(1));
        }

        if (heightMode != MeasureSpec.EXACTLY) {
            heightSize = (int) (Math.max(leftRadius, rightRadius) * 2 * max + dp2px(1));
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerY = getMeasuredHeight() / 2.0f;

        float LtRInitRadius, RTLInitRadius;
        Paint ltrPaint, rtlPaint;

        //确定当前【从左往右】移动的是哪颗小球
        if (isLtr) {
            LtRInitRadius = leftRadius;
            RTLInitRadius = rightRadius;
            ltrPaint = leftPaint;
            rtlPaint = rightPaint;
        } else {
            LtRInitRadius = rightRadius;
            RTLInitRadius = leftRadius;
            ltrPaint = rightPaint;
            rtlPaint = leftPaint;
        }

        float ltrX = getMeasuredWidth() / 2.0f - distance / 2.0f;
        ltrX = ltrX + (distance * fraction);

        float rtlX = getMeasuredWidth() / 2.0f + distance / 2.0f;
        rtlX = rtlX - (distance * fraction);

        float ltrBallRadius, rtlBallRadius;
        if (fraction <= scaleStartFraction) { //动画进度[0,scaleStartFraction]时，球大小由1倍逐渐缩放至ltrScale/rtlScale倍
            float scaleFraction = 1.0f / scaleStartFraction * fraction; //百分比转换 [0,scaleStartFraction]] -> [0,1]
            ltrBallRadius = LtRInitRadius * (1 + (ltrScale - 1) * scaleFraction);
            rtlBallRadius = RTLInitRadius * (1 + (rtlScale - 1) * scaleFraction);
        } else if (fraction >= scaleEndFraction) { //动画进度[scaleEndFraction,1]，球大小由ltrScale/rtlScale倍逐渐恢复至1倍
            float scaleFraction = (fraction - 1) / (scaleEndFraction - 1); //百分比转换，[scaleEndFraction,1] -> [1,0]
            ltrBallRadius = LtRInitRadius * (1 + (ltrScale - 1) * scaleFraction);
            rtlBallRadius = RTLInitRadius * (1 + (rtlScale - 1) * scaleFraction);
        } else { //动画进度[scaleStartFraction,scaleEndFraction]，球保持缩放后的大小
            ltrBallRadius = LtRInitRadius * ltrScale;
            rtlBallRadius = RTLInitRadius * rtlScale;
        }
        ltrPath.reset();
        ltrPath.addCircle(ltrX, centerY, ltrBallRadius, Path.Direction.CW);
        rtlPath.reset();
        rtlPath.addCircle(rtlX, centerY, rtlBallRadius, Path.Direction.CW);
        mixPath.op(ltrPath, rtlPath, Path.Op.INTERSECT);

        canvas.drawPath(ltrPath, ltrPaint);
        canvas.drawPath(rtlPath, rtlPaint);
        canvas.drawPath(mixPath, mixPaint);
    }

    public void stop() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    private void initDraw() {
        leftPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        leftPaint.setColor(leftColor);
        rightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rightPaint.setColor(rightColor);
        mixPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mixPaint.setColor(mixColor);

        ltrPath = new Path();
        rtlPath = new Path();
        mixPath = new Path();
    }

    private float dp2px(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void checkAttr() {
        leftRadius = rightRadius > 0 ? leftRadius : mRadius;
        rightRadius = rightRadius > 0 ? rightRadius : mRadius;
        gap = gap > 0 ? gap : mGap;
        rtlScale = rtlScale >= 0 ? rtlScale : RTL_SCALE;
        ltrScale = ltrScale >= 0 ? ltrScale : LTR_SCALE;
        duration = duration > 0 ? duration : DURATION;
        pauseDuration = pauseDuration >= 0 ? pauseDuration : PAUSE_DURATION;
        if (scaleStartFraction < 0 || scaleStartFraction > 0.5f) {
            scaleStartFraction = SCALE_START_FRACTION;
        }
        if (scaleEndFraction < 0.5 || scaleEndFraction > 1) {
            scaleEndFraction = SCALE_END_FRACTION;
        }
    }
}
