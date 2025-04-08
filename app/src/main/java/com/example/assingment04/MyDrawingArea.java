package com.example.assingment04;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MyDrawingArea extends View {

    private Path path = new Path();
    private Bitmap bmp;
    private Canvas bmpCanvas;
    private GestureDetector gestureDetector;
    private Paint paint;


    // Constructors
    public MyDrawingArea(Context context) {
        super(context);
        init(context);
    }

    public MyDrawingArea(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MyDrawingArea(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public MyDrawingArea(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    // Initialize gesture detector and paint
    private void init(Context context) {
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                invalidate();
                return true;
            }
        });


        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bmpCanvas = new Canvas(bmp);

        // Set the initial background color of the bitmap
        bmpCanvas.drawColor(Color.LTGRAY);  // You can change this to any color
    }

    public Bitmap getBitmap() {



        return bmp;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // Draw the background color for each onDraw call to ensure it's consistent
        bmpCanvas.drawColor(Color.LTGRAY);  // Same color as above

        // Draw the current path on top of the background color
        bmpCanvas.drawPath(path, paint);

        // Render the bitmap to the view's canvas
        canvas.drawBitmap(bmp, 0, 0, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int action = event.getAction();

        if (gestureDetector.onTouchEvent(event)) {
            return true;
        }

        //handle touch event, either press or drag
        if (action == MotionEvent.ACTION_DOWN) {
            path.moveTo(x, y); //move cursor
        } else if (action == MotionEvent.ACTION_MOVE) {
            path.lineTo(x, y); //draw line
        }

        invalidate();
        return true;
    }

    public void clearDrawing() {
        path.reset();  //clear path
        bmp.eraseColor(Color.TRANSPARENT);  //clear canvas
        invalidate();
    }
}
