package com.teinvdlugt.android.fractals;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

public class FractalView extends View {

    private double range = 3.5;
    private int resolution = 512;
    private int precision = 1000;
    private ProgressBar progressBar;
    private boolean[][] pixels = new boolean[resolution][resolution];

    Paint paint = new Paint();

    public void recalculate() {
        final int finalResolution = resolution;
        final int finalPrecision = precision;
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int y = 0; y < pixels.length; y++) {
                    for (int x = 0; x < pixels[y].length; x++) {
                        double cReal = -range / 2 + range / finalResolution * x;
                        double cImg = -range / 2 + range / finalResolution * y;
                        double zReal = cReal, zImg = cImg;

                        int i = 0;
                        while (zReal * zReal + zImg * zImg <= 4 && i < finalPrecision) {
                            double zRealNew = zReal * zReal - zImg * zImg + cReal;
                            zImg = 2 * zReal * zImg + cImg;
                            zReal = zRealNew;
                            i++;
                        }
                        pixels[y][x] = i == finalPrecision;
                    }
                    //Log.d("coffee", "row " + y + " " + pixels[y][40]);

                    final int finalY = y;
                    progressBar.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress(finalY);
                        }
                    });
                }

                postInvalidate();
                progressBar.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress(0);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        paint.setColor(Color.BLACK);

        float pixelsPerBlockX = getWidth() / (float) resolution;
        float pixelsPerBlockY = getHeight() / (float) resolution;
        float pixelsPerBlock = pixelsPerBlockX < pixelsPerBlockY ? pixelsPerBlockX : pixelsPerBlockY;

        for (int y = 0; y < pixels.length; y++) {
            for (int x = 0; x < pixels[y].length; x++) {
                if (pixels[y][x]) {
                    canvas.drawRect(x * pixelsPerBlock, y * pixelsPerBlock,
                            (x + 1) * pixelsPerBlock, (y + 1) * pixelsPerBlock, paint);
                }
            }
        }

        // Draw y axis
        canvas.drawLine(resolution / 2 * pixelsPerBlock, 0, resolution / 2 * pixelsPerBlock, getHeight(), paint);
        // Draw x axis
        canvas.drawLine(0, resolution / 2 * pixelsPerBlock, getWidth(), resolution / 2 * pixelsPerBlock, paint);
    }


    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public void setProgressBar(ProgressBar progressBar) {
        this.progressBar = progressBar;
        progressBar.setMax(resolution);
    }

    public int getResolution() {
        return resolution;
    }

    public void setResolution(int resolution) {
        this.resolution = resolution;
        progressBar.setMax(resolution);
        pixels = new boolean[resolution][resolution];
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
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
