package com.teinvdlugt.android.fractals;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FractalView2 extends View {

    private int[] bitmap;
    private int bitmapWidth, bitmapHeight;
    private List<double[]> wanted = new ArrayList<>();
    private Paint paint;
    private boolean calculating = false;
    private CalculatingTask calculatingTask;

    protected double startReal = -2, startImg = 2, rangeReal = 4, rangeImg = 4, escapeValue = 2;
    private double precision = 100;
    private double maxColorIterations = 400;
    private double colorDistribution = 30;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(Bitmap.createScaledBitmap(
                Bitmap.createBitmap(bitmap, bitmapWidth, bitmapHeight, Bitmap.Config.RGB_565),
                getWidth(), getHeight(), false), 0, 0, paint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculating = false;

        if (oldw == 0 && oldh == 0) {
            // Pretend to be coming from a square, because standard values
            // (startReal -2, rangeReal 4, startImg 2, rangeImg 4) 'presume'
            // a square layout
            oldw = oldh = Math.min(w, h);
        }

        applyDimensions(w, h, oldw, oldh);
        startOver();
    }

    private void applyDimensions(int w, int h, int oldw, int oldh) {
        double rangeRealDiff = (w - oldw) / (double) oldw * rangeReal;
        double rangeImgDiff = (h - oldh) / (double) oldh * rangeImg;
        startReal = startReal - rangeRealDiff / 2.0;
        startImg = startImg + rangeImgDiff / 2.0;
        rangeReal = rangeReal + rangeRealDiff;
        rangeImg = rangeImg + rangeImgDiff;
        bitmapWidth = w / 10;
        bitmapHeight = h / 10;
        bitmap = new int[bitmapWidth * bitmapHeight];
        /*widthResolution = (int) ((double) w / oldw * widthResolution);
        heightResolution = (int) ((double) h / oldh * heightResolution);*/
    }

    public void startOver() {
        wanted.clear();
        double yFactor = rangeImg / bitmapHeight;
        double xFactor = rangeReal / bitmapWidth;
        for (int y = 0; y < bitmapHeight; y++) {
            for (int x = 0; x < bitmapWidth; x++) {
                wanted.add(new double[]{startReal + x * xFactor,
                        startImg - y * yFactor});
            }
        }
        calculateWanted();
    }

    /*private double spareX = 0;
    private double spareY = 0;*/

    private void move(double x, double y) {
        if (x == 0 && y == 0) return;
        startReal += x;
        startImg -= y;

        double xBmpPixelsDouble = x / rangeReal * bitmapWidth;
        double yBmpPixelsDouble = y / rangeImg * bitmapHeight;
        int xBmpPixels = (int) Math.round(xBmpPixelsDouble);
        int yBmpPixels = (int) Math.round(yBmpPixelsDouble);
        /*spareX += xBmpPixelsDouble - xBmpPixels;
        spareY += yBmpPixelsDouble - yBmpPixels;
        int takeFromSpareX = (int) spareX;
        int takeFromSpareY = (int) spareY;
        spareX -= takeFromSpareX;
        spareY -= takeFromSpareY;
        xBmpPixels += takeFromSpareX;
        yBmpPixels += takeFromSpareY;*/

        int[] copy = Arrays.copyOf(bitmap, bitmap.length);

        for (int xpx = 0; xpx < bitmapWidth; xpx++) {
            for (int ypx = 0; ypx < bitmapHeight; ypx++) {
                int origX = xpx + xBmpPixels;
                int origY = ypx + yBmpPixels;
                int color;
                if (origX < 0 || origY < 0 || origX >= bitmapWidth || origY >= bitmapHeight) {
                    color = Color.BLACK;
                    double img = startImg - (double) ypx / bitmapHeight * rangeImg;
                    wanted.add(new double[]{
                            startReal + (double) xpx / bitmapWidth * rangeReal,
                            img});
                } else {
                    color = copy[bitmapWidth * origY + origX];
                }
                bitmap[bitmapWidth * ypx + xpx] = color;
            }
        }
    }

    private void zoom(double factor, double x, double y) {
        /*if (factor == 1) move(x, y);

        // int xBmpPixels = (int) (x / rangeReal * bitmap.getWidth());
        // int yBmpPixels = (int) (y / rangeImg * bitmap.getHeight());

        Bitmap copy = bitmap.copy(Bitmap.Config.RGB_565, false);

        for (int xpx = 0; xpx < bitmap.getWidth(); xpx++) {
            for (int ypx = 0; ypx < bitmap.getHeight(); ypx++) {
                int origX = (int) Math.round(xpx / factor);
                int origY = (int) Math.round(ypx / factor);
                // TODO: 7-2-2016 Move according to xBmpPixels and yBmpPixels
                int color;
                if (origX < 0 || origY < 0 || origX > copy.getWidth() || origY > copy.getHeight()) {
                    color = Color.BLACK;
                    wanted.add(new double[]{
                            startReal + xpx / bitmap.getWidth() * rangeReal,
                            startImg + ypx / bitmap.getHeight() * rangeImg});
                } else {
                    color = copy.getPixel(origX, origY);
                }
                bitmap.setPixel(xpx, ypx, color);
            }
        }*/
    }

    /**
     * Set {@code calculating} member variable to false to cancel instead of calling task.cancel(boolean);
     */
    private class CalculatingTask extends AsyncTask<Void, int[], Void> {
        @Override
        protected void onPreExecute() {
            calculating = true;
        }

        @Override
        protected Void doInBackground(Void... params) {
            List<int[]> batch = new ArrayList<>();

            while (!wanted.isEmpty() && calculating) {
                double[] pos = wanted.remove(0);

                final double cReal = pos[0];
                final double cImg = pos[1];
                double zReal = 0, zImg = 0;

                if (cImg < startImg - rangeImg || cImg > startImg || cReal > rangeImg + startImg || cReal < startReal) {
                    // Complex number is off-screen, so there's no use calculating its value
                    continue;
                }

                int iterations = 0;
                while (zReal * zReal + zImg * zImg <= escapeValue * escapeValue && iterations < precision) {
                    double zRealNew = zReal * zReal - zImg * zImg + cReal;
                    zImg = 2 * zReal * zImg + cImg;
                    zReal = zRealNew;
                    iterations++;
                }

                int xpx = (int) Math.round((cReal - startReal) / rangeReal * bitmapWidth);
                int ypx = (int) Math.round((startImg - cImg) / rangeImg * bitmapHeight);
                int color = iterations == precision ? Color.BLACK : resolveColor(iterations);
                // TODO useColor ? resolveColor(iterations) : Color.WHITE;
                batch.add(new int[]{xpx, ypx, color});

                if (batch.size() > 20 && calculating) {
                    int[][] array = new int[batch.size()][3];
                    for (int i = 0; i < batch.size(); i++) {
                        array[i] = batch.get(i);
                    }
                    publishProgress(array);
                    batch.clear();
                }
            }

            calculating = false;
            return null;
        }

        @Override
        protected void onProgressUpdate(int[]... values) {
            for (int[] pixel : values) {
                try {
                    bitmap[bitmapWidth * pixel[1] + pixel[0]] = pixel[2];
                } catch (IndexOutOfBoundsException ignored) {}
            }
            invalidate();
        }

        protected int resolveColor(int iterations) {
            // See FractalView.resolveColor() for comments
            double value = 1 - Math.pow(1 - iterations / maxColorIterations, colorDistribution);
            if (value >= 1.) return Color.WHITE;
            double valuePerUnitColor = .5 / 255;
            int blue = (int) Math.max(255 - value / valuePerUnitColor, 0);
            int red = (int) Math.max(255 - Math.abs(.5 - value) / valuePerUnitColor, 0);
            int green = (int) Math.max(255 - (1. - value) / valuePerUnitColor, 0);
            return Color.rgb(red, green, blue);
        }
    }

    private void calculateWanted() {
        if (!calculating) {
            calculatingTask = new CalculatingTask();
            calculatingTask.execute();
        }
    }

    private float prevXDrag1 = -1, prevYDrag1 = -1;
    private float prevXDrag2 = -1, prevYDrag2 = -1;
    private int pointerId1 = -1, pointerId2 = -1;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                prevXDrag1 = event.getX();
                prevYDrag1 = event.getY();
                pointerId1 = event.getPointerId(0);
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (pointerId2 != -1)
                    return false; // This was probably the third (or higher) pointer
                pointerId2 = event.getPointerId(event.getActionIndex());
                int index = event.getActionIndex();
                prevXDrag2 = event.getX(index);
                prevYDrag2 = event.getY(index);
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 1) {
                    // Move
                    move((prevXDrag1 - event.getX()) / getWidth() * rangeReal,
                            (prevYDrag1 - event.getY()) / getHeight() * rangeImg);
                    prevXDrag1 = event.getX();
                    prevYDrag1 = event.getY();
                } else {
                    // Zoom and move
                    int indexCurrent = event.getActionIndex();
                    int index1 = event.findPointerIndex(pointerId1);
                    int index2 = event.findPointerIndex(pointerId2);
                    if ((indexCurrent != index1 && indexCurrent != index2) || pointerId1 == -1 ||
                            pointerId2 == -1 || prevXDrag1 == -1 || prevYDrag1 == -1 ||
                            prevXDrag2 == -1 || prevYDrag2 == -1) {
                        // This event is for a third (or higher) pointer,
                        // or something went terribly wrong
                        return false;
                    }

                    double xDist1 = prevXDrag2 - prevXDrag1;
                    double yDist1 = prevYDrag2 - prevYDrag1;
                    double dist1Sqr = xDist1 * xDist1 + yDist1 * yDist1;

                    double centerPointX1 = (prevXDrag1 + prevXDrag2) / 2d;
                    double centerPointY1 = (prevYDrag1 + prevYDrag2) / 2d;
                    double centerPointReal1 = startReal + centerPointX1 / getWidth() * rangeReal;
                    double centerPointImg1 = startImg + centerPointY1 / getHeight() * rangeImg;

                    prevXDrag1 = event.getX(index1);
                    prevYDrag1 = event.getY(index1);
                    prevXDrag2 = event.getX(index2);
                    prevYDrag2 = event.getY(index2);

                    double xDist2 = prevXDrag2 - prevXDrag1;
                    double yDist2 = prevYDrag2 - prevYDrag1;
                    double dist2Sqr = xDist2 * xDist2 + yDist2 * yDist2;
                    double factor = Math.sqrt(dist1Sqr / dist2Sqr);

                    double centerPointX2 = (prevXDrag1 + prevXDrag2) / 2d;
                    double centerPointY2 = (prevYDrag1 + prevYDrag2) / 2d;
                    double centerPointReal2 = startReal + centerPointX2 / getWidth() * rangeReal;
                    double centerPointImg2 = startImg + centerPointY2 / getHeight() * rangeImg;

                    zoom(factor, centerPointReal2 - centerPointReal1, centerPointImg2 - centerPointImg1);
                }

                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                prevXDrag1 = prevXDrag2 = prevYDrag1 = prevYDrag2 =
                        pointerId2 = pointerId1 = -1;
                calculateWanted();
                return false;
            case MotionEvent.ACTION_POINTER_UP:
                int pointerIndex = event.getActionIndex();
                if (pointerIndex == event.findPointerIndex(pointerId1)) {
                    // Transfer the data of pointer 2 to pointer 1
                    pointerId1 = pointerId2;
                    prevXDrag1 = prevXDrag2;
                    prevYDrag1 = prevYDrag2;

                    // Get rid of pointer 2
                    pointerId2 = -1;
                    prevXDrag2 = prevYDrag2 = -1;
                } else if (pointerIndex == event.findPointerIndex(pointerId2)) {
                    pointerId2 = -1;
                    prevXDrag2 = prevYDrag2 = -1;
                } else return false;
                return true;
            default:
                return false;
        }
    }

    public FractalView2(Context context) {
        super(context);
        paint = new Paint();
    }

    public FractalView2(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FractalView2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
