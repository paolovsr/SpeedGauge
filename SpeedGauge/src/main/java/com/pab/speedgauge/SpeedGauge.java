package com.pab.speedgauge;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class SpeedGauge extends View {
    int maxValue, currentValue, stepValue, divider, scaledMax, scaledValue, scaledStep;
    float radius, innerRadius, progressThickness, xCenter, yCenter, unit;
    Paint textPaint, progressPaint, progressBackPaint, bigTicPaint, needlePaint, bigTextPaint; //How to draw
    //Canvas something; //What to draw
    RadialGradient needleGradient;
    int textColor, progressColor, needleColor, progressBackColor;
    RectF progressArc;
    String unitOfMeasurement;
    ValueAnimator cursorAnimator;
    Path needle = new Path();
    Path rotatedNeedle = new Path();
    Matrix mMatrix = new Matrix();
    RectF bounds = new RectF();

    float[] stepXs, stepYs;



    public SpeedGauge(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SpeedGauge, 0, 0);
        try{
            maxValue = a.getInteger(R.styleable.SpeedGauge_maxValue, 100);
            currentValue = a.getInteger(R.styleable.SpeedGauge_value, 0);
            stepValue = a.getInteger(R.styleable.SpeedGauge_stepValue, 10);
            divider = a.getInteger(R.styleable.SpeedGauge_valDivider, 1);
            textColor = a.getColor(R.styleable.SpeedGauge_textColor, 0xFF000000);
            progressColor = a.getColor(R.styleable.SpeedGauge_progressColor, 0xFF707070);
            progressBackColor = a.getColor(R.styleable.SpeedGauge_progressBackColor, 0xFF000000);
            needleColor = a.getColor(R.styleable.SpeedGauge_needleColor, 0xFFAA0000);
            unitOfMeasurement = a.getString(R.styleable.SpeedGauge_udm);
            if (unitOfMeasurement == null){
                unitOfMeasurement = "  ";
            }
            if(divider==0)
                divider=1;
            scaledMax = maxValue/divider;
            scaledStep = stepValue/divider;
            scaledValue =currentValue/divider;
        } finally{
            a.recycle(); //TypedArray objects are a shared resource and must be recycled after use.
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Try for a width based on our minimum
        int minw = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
        int w = resolveSizeAndState(minw, widthMeasureSpec, 1);

        // Whatever the width ends up being, ask for a height that would let the pie
        // get as big as it can
        int minh = MeasureSpec.getSize(w) + getPaddingBottom() + getPaddingTop();
        int h = resolveSizeAndState(minh, heightMeasureSpec, 0);

        setMeasuredDimension(w, h);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH){
        // Account for padding
        float xpad = (float)(getPaddingLeft() + getPaddingRight());
        float ypad = (float)(getPaddingTop() + getPaddingBottom());
        float ww = (float)w - xpad;
        float hh = (float)h - ypad;

        // Figure out how big we can make the circle.
        float diameter = Math.min(ww, hh);
        radius = diameter/2;
        unit=radius/100;

        innerRadius = radius - 2*radius/5;
        progressThickness = radius/5;
        float progressRadius= radius-progressThickness/2;
        xCenter = getPaddingLeft() + ww/2;
        yCenter = getPaddingTop() + hh/2;
        progressArc = new RectF(xCenter-progressRadius, yCenter-progressRadius, xCenter+progressRadius, yCenter+progressRadius);

        float upperBase = innerRadius/50;
        float lowerBase = innerRadius/10;
        needle.moveTo(xCenter, yCenter);
        needle.lineTo(xCenter-lowerBase, yCenter);
        needle.lineTo(xCenter-upperBase, yCenter-innerRadius);
        needle.lineTo(xCenter+upperBase, yCenter-innerRadius);
        needle.lineTo(xCenter+lowerBase, yCenter);
        needle.lineTo(xCenter, yCenter);

        init();
    }

    private void init(){
        //Creating objects ahead of time is an important optimization

        needleGradient = new RadialGradient(xCenter, yCenter, innerRadius, Color.TRANSPARENT, needleColor, Shader.TileMode.CLAMP);

        bigTicPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bigTicPaint.setColor(textColor);
        bigTicPaint.setStrokeWidth(3*unit);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setStrokeWidth(2*unit);
        textPaint.setTextSize(15*unit);

        bigTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bigTextPaint.setColor(textColor);
        bigTextPaint.setStrokeWidth(3*unit);
        bigTextPaint.setTextSize(30*unit);

        needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        needlePaint.setColor(needleColor);
        needlePaint.setShader(needleGradient);
        //needlePaint.setStrokeWidth(2*unit);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setColor(progressColor);
        progressPaint.setStrokeWidth(progressThickness);

        progressBackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressBackPaint.setStyle(Paint.Style.STROKE);
        progressBackPaint.setColor(progressBackColor);
        progressBackPaint.setStrokeWidth(progressThickness);


        //Draw the steps: perform the calculations only once and store the results
        int startAngle = 210;
        int deltaAngle = 240 * stepValue / maxValue;
        stepXs = new float[(int)(scaledMax/scaledStep)+1];
        stepYs = new float[stepXs.length];
        for (int i = 0; i < stepXs.length; i ++) {
            stepXs[i] = (float) (xCenter - textPaint.measureText(String.valueOf(i*scaledStep)) / 2 + ((radius - 1.5 * progressThickness) * Math.cos(Math.toRadians(startAngle))));
            stepYs[i] = (float) (yCenter - (textPaint.descent() + textPaint.ascent()) / 2 - ((radius - 1.5 * progressThickness) * Math.sin(Math.toRadians(startAngle))));
            startAngle -= deltaAngle;
        }

    }


    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);

        if(progressArc!=null) {
            //Draw the quadrant
            float startAngle = 150;
            canvas.drawArc(progressArc, startAngle, 240, false, progressBackPaint);

            //Draw progress.
            startAngle = 150;
            canvas.drawArc(progressArc, startAngle, (float) (currentValue * 240.0 / maxValue), false, progressPaint);

            startAngle = 210;
            int deltaAngle = 240 * stepValue / maxValue;
            //Draw the steps.
            for (int i = 0; i < stepXs.length; i ++) {
                canvas.drawText(String.valueOf(i*scaledStep),stepXs[i], stepYs[i], textPaint);
            }


            String displayedValue;
            displayedValue = String.valueOf(currentValue);
           /* if (divider==1)
                displayedValue = String.valueOf(currentValue);
            else
                displayedValue = String.format("%.2f", (float)(currentValue)/divider);*/

                canvas.drawText(displayedValue,
                    xCenter - bigTextPaint.measureText(displayedValue) / 2,
                    (float) (yCenter - radius * Math.cos(Math.toRadians(210)) + (bigTextPaint.ascent() + bigTextPaint.descent())),
                    bigTextPaint);

            //Draw the unit of measurement
            canvas.drawText(unitOfMeasurement, xCenter - textPaint.measureText(unitOfMeasurement) / 2, yCenter - 2*textPaint.ascent(), textPaint);

            //Draw needle
            rotatedNeedle.set(needle);
            rotatedNeedle.computeBounds(bounds, true);
            mMatrix.postRotate((float) (-120 + currentValue * 240.0 / maxValue), bounds.centerX(), bounds.bottom);
            rotatedNeedle.transform(mMatrix);
            mMatrix.reset();
            canvas.drawPath(rotatedNeedle, needlePaint);
        }
    }
/*
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //Log.d("SpeedGauge", "Touch");
        if(progressArc.contains(event.getX(), event.getY())) {
            cursorAnimator = ObjectAnimator.ofInt(com.omsi.italmixdiagnostic.SpeedGauge.this, "value", 0, maxValue, 0);
            cursorAnimator.setDuration(2000);
            cursorAnimator.start();
            return super.onTouchEvent(event);
        }else{
            return false;
        }
    }
*/
    /** Getters and Setters allowing dynamic behaviour */
    public void setValue(int val){
        //used for the animation.
        currentValue = val;
        invalidate();//Forces the redraw of layout.
        requestLayout(); //Needed if a property changes might affect the size and shape of view.
    }

    public void moveToValue(int value){
        cursorAnimator = ObjectAnimator.ofInt(SpeedGauge.this, "value",  currentValue, value);

        cursorAnimator.setDuration(200*Math.abs(value-currentValue)/maxValue);
        cursorAnimator.start();
        //currentValue = value;
        invalidate();
    }

    public void setMaxValue(int maxVal){
        this.maxValue = maxVal;
        invalidate();
    }

    public void setStepValue(int stepValue){
        this.stepValue = stepValue;
        invalidate();
    }
    public void setTextColor(int color){
        textColor = color;
        invalidate();
    }
    public void setProgressColor(int color){
        progressColor = color;
        invalidate();
    }
    public void setProgressBackColor(int color){
        progressBackColor = color;
        invalidate();
    }
    public void setNeedleColor(int color){
        needleColor= color;
        invalidate();
    }
    public void setUnitOfMeasurement(String unit){
        unitOfMeasurement = unit;
        invalidate();
    }
}
