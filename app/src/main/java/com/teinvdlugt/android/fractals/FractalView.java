package com.teinvdlugt.android.fractals;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class FractalView extends View {

    private double range = 2.7;
    private int widthPixels = 1024;
    private boolean[][] pixels = new boolean[widthPixels][widthPixels];

    Paint paint = new Paint();

    public void recalculate() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int y = 0; y < pixels.length; y++) {
                    for (int x = 0; x < pixels[y].length; x++) {
                        double cReal = -range / 2 + range / widthPixels * x;
                        double cImg = -range / 2 + range / widthPixels * y;
                        double zReal = cReal, zImg = cImg;

                        int i = 0;
                        int max = 1000;
                        while (zReal * zReal + zImg * zImg <= 4 && i < max) {
                            double zRealNew = zReal * zReal - zImg * zImg + cReal;
                            zImg = 2 * zReal * zImg + cImg;
                            zReal = zRealNew;
                            i++;
                        }
                        pixels[y][x] = i == max;
                    }
                    Log.d("coffee", "row " + y + " " + pixels[y][40]);
                    if (y % widthPixels / 8 == 0) postInvalidate();
                }

                postInvalidate();
            }
        }).start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        paint.setColor(Color.BLACK);

        float pixelsPerBlock = getWidth() / (float) widthPixels;
        for (int y = 0; y < pixels.length; y++) {
            for (int x = 0; x < pixels[y].length; x++) {
                if (pixels[y][x]) {
                    canvas.drawRect(x * pixelsPerBlock, y * pixelsPerBlock,
                            (x + 1) * pixelsPerBlock, (y + 1) * pixelsPerBlock, paint);
                }
            }
        }

        // Draw y axis
        canvas.drawLine(widthPixels / 2 * pixelsPerBlock, 0, widthPixels / 2 * pixelsPerBlock, getHeight(), paint);
        // Draw x axis
        canvas.drawLine(0, widthPixels / 2 * pixelsPerBlock, getWidth(), widthPixels / 2 * pixelsPerBlock, paint);
    }

    public FractalView(Context context) {
        super(context);
    }

    public FractalView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FractalView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
