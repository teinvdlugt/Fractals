package com.teinvdlugt.android.fractals;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;

public class FractalView extends View {

    /**
     * The lowest real value shown
     */
    protected double startReal = -2;
    /**
     * The highest imaginary value shown
     */
    protected double startImg = 2;
    protected double range = 4;
    protected int resolution = 512;
    protected int precision = 400;
    protected ProgressBar progressBar;
    protected Bitmap bitmap;
    protected Bitmap scaledBitmap;
    protected Paint axisPaint;
    protected Paint zoomPaint;
    private boolean calculating = false;

    public void recalculate() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                calculating = true;
                final int[] colors = new int[resolution * resolution];

                for (int y = 0; y < resolution; y++) {
                    for (int x = 0; x < resolution; x++) {
                        double cReal = absoluteRealValue(x);
                        double cImg = absoluteImaginaryValue(y);
                        double zReal = cReal, zImg = cImg;

                        int i = 0;
                        while (zReal * zReal + zImg * zImg <= 4 && i < precision) {
                            double zRealNew = zReal * zReal - zImg * zImg + cReal;
                            zImg = 2 * zReal * zImg + cImg;
                            zReal = zRealNew;
                            i++;
                        }

                        colors[resolution * y + x] = i == precision ? Color.BLACK : Color.WHITE;
                    }

                    final int finalY = y;
                    if (progressBar != null) {
                        progressBar.post(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setProgress(finalY);
                            }
                        });
                    }
                }

                bitmap = Bitmap.createBitmap(colors, resolution, resolution, Bitmap.Config.RGB_565);
                scaledBitmap = null;
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (progressBar != null) progressBar.setProgress(0);
                        invalidate();
                        requestLayout();
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Define square size (the view isn't allowed to be a rectangle)
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        int size = Math.min(width, height);

        // Draw bitmap
        if (bitmap != null) {
            if (scaledBitmap == null) {
                scaledBitmap = Bitmap.createScaledBitmap(bitmap, size, size, false);
            }
            canvas.drawBitmap(scaledBitmap, 0f, 0f, null);
        }

        // Draw axes
        float xAxis = (float) (size * (-startReal / range));
        float yAxis = (float) (size * (startImg / range));
        canvas.drawLine(xAxis, 0, xAxis, size, axisPaint);
        canvas.drawLine(0, yAxis, size, yAxis, axisPaint);

        // Draw zoom indication
        if (zoomStartX != -1 && zoomStartY != -1 && zoomEndX != -1 && zoomEndY != -1) {
            float left = Math.min(zoomStartX, zoomEndX);
            float right = Math.max(zoomStartX, zoomEndX);
            float top = Math.min(zoomStartY, zoomEndY);
            float bottom = Math.max(zoomStartY, zoomEndY);
            canvas.drawRect(left, top, right, bottom, zoomPaint);
        }
    }

    protected float zoomStartX = -1, zoomStartY = -1;
    protected float zoomEndX = -1, zoomEndY = -1;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            zoomStartX = event.getX();
            zoomStartY = event.getY();
            zoomEndX = -1;
            zoomEndY = -1;
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            zoomEndX = event.getX();
            zoomEndY = event.getY();
            invalidate();
            requestLayout();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            zoomIn();
            zoomStartX = zoomStartY = zoomEndX = zoomEndY = -1;
            recalculate();
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void zoomIn() {
        double startReal = absoluteRealValue(zoomStartX);
        double endReal = absoluteRealValue(zoomEndX);
        double startImg = absoluteImaginaryValue(zoomStartY);
        double endImg = absoluteImaginaryValue(zoomEndY);

        double xRange = Math.abs(startReal - endReal);
        double yRange = Math.abs(startImg - endImg);

        this.range = Math.max(xRange, yRange);
        this.startReal = Math.min(startReal, endReal);
        this.startImg = Math.max(startImg, endImg);
    }

    protected void init() {
        axisPaint = new Paint();
        zoomPaint = new Paint();
        axisPaint.setColor(Color.BLACK);
        zoomPaint.setARGB(128, 50, 50, 200);
    }

    /**
     * The real value in the complex field represented by a column of virtual pixels.
     *
     * @param column The column of the 'virtual' pixels (defined in {@code resolution})
     * @return The real value in the complex field
     */
    protected double absoluteRealValue(int column) {
        // TODO: 18-8-2015 resolution needs to be finalized when calculating process is running
        return startReal + range / resolution * column;
    }

    /**
     * The imaginary value in the complex field represented by a row of virtual pixels.
     *
     * @param row The row of the 'virtual' pixels (defined in {@code resolution})
     * @return The imaginary value in the complex field
     */
    protected double absoluteImaginaryValue(int row) {
        return startImg - range / resolution * row;
    }

    /**
     * The real value in the complex field represented by a device pixel in {@code scaledBitmap}.
     *
     * @param x The x position of the device pixel from which to retrieve the real value
     * @return The real value in the complex field
     */
    protected double absoluteRealValue(float x) {
        if (scaledBitmap != null) {
            return startReal + x / scaledBitmap.getWidth() * range;
        }
        return -1;
    }

    /**
     * The imaginary value in the complex field represented by a device pixel in {@code scaledBitmap}.
     *
     * @param y The y position of the device pixel from which to retrieve the imaginary value
     * @return The imaginary value in the complex field
     */
    protected double absoluteImaginaryValue(float y) {
        if (scaledBitmap != null) {
            return startImg - y / scaledBitmap.getHeight() * range;
        }
        return -1;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    /**
     * The {@code ProgressBar} to which the {@code FractalView} will report its progress.
     * If you want to detach the {@code ProgressBar} from the {@code FractalView}, pass null.
     *
     * @param progressBar Null if you don't want any {@code ProgressBar} to be linked to the
     *                    {@code FractalView}.
     */
    public void setProgressBar(@Nullable ProgressBar progressBar) {
        this.progressBar = progressBar;
        if (progressBar != null) {
            progressBar.setMax(resolution);
        }
    }

    public int getResolution() {
        return resolution;
    }

    public void setResolution(int resolution) {
        if (!calculating) {
            this.resolution = resolution;
            progressBar.setMax(resolution);
        }
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        if (!calculating) this.precision = precision;
    }

    public FractalView(Context context) {
        super(context);
        init();
    }

    public FractalView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FractalView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
}
