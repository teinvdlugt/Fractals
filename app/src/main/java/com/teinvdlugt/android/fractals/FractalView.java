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

import java.util.Arrays;

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
    /**
     * The bitmap will be updated whilst calculating with for example a new resolution.
     * {@code updateRows} is the number of rows to include in one update. The higher the
     * value the higher the performance, but the used memory will increase a bit.
     */
    protected int updateRows = 10;
    protected double escapeValue = 2;
    protected Bitmap bitmap;
    protected Bitmap scaledBitmap;
    protected Paint axisPaint;
    protected Paint zoomPaint;
    private CalculatingTask calculatingTask;

    private class CalculatingTask extends AsyncTask<Void, Void, Void> {
        double startReal = -1, startImg = -1, range = -1, escapeValue = -1;
        int resolution = -1, precision = -1, updateRows = -1;
        Bitmap backup;

        @Override
        protected void onPreExecute() {
            // Copy these values so that changes to the FractalView values will
            // not affect calculating process.
            if (startReal == -1) startReal = FractalView.this.startReal;
            if (startImg == -1) startImg = FractalView.this.startImg;
            if (range == -1) range = FractalView.this.range;
            if (escapeValue == -1) escapeValue = FractalView.this.escapeValue;
            if (resolution == -1) resolution = FractalView.this.resolution;
            if (precision == -1) precision = FractalView.this.precision;
            if (updateRows == -1) updateRows = FractalView.this.updateRows;
        }

        @Override
        protected Void doInBackground(Void... params) {
            long time = System.nanoTime();
            if (bitmap != null) {
                backup = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
                if (bitmap.getWidth() != resolution) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, resolution, resolution, false);
                }
            } else {
                bitmap = Bitmap.createBitmap(resolution, resolution, Bitmap.Config.RGB_565);
            }
            int[] progressLine = new int[resolution];
            Arrays.fill(progressLine, Color.RED);

            int[] colors = new int[resolution * updateRows];
            for (int y = 0; y < resolution; y++) {
                if (isCancelled()) return null;
                for (int x = 0; x < resolution; x++) {
                    double cReal = absoluteRealValue(x);
                    double cImg = absoluteImaginaryValue(y);
                    double zReal = cReal, zImg = cImg;

                    int iterations = 0;
                    while (zReal * zReal + zImg * zImg <= escapeValue * escapeValue && iterations < precision) {
                        double zRealNew = zReal * zReal - zImg * zImg + cReal;
                        zImg = 2 * zReal * zImg + cImg;
                        zReal = zRealNew;
                        iterations++;
                    }

                    colors[(y % updateRows) * resolution + x] = iterations == precision ? Color.BLACK : Color.WHITE;
                }

                if (isCancelled()) return null;

                if ((y + 1) % updateRows == 0) {
                    bitmap.setPixels(colors, 0, resolution, 0, y - updateRows + 1, resolution, updateRows);
                    if (y + 1 != bitmap.getHeight()) {
                        bitmap.setPixels(progressLine, 0, resolution, 0, y + 1, resolution, 1);
                    }

                    if (scaledBitmap != null) {
                        scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledBitmap.getWidth(), scaledBitmap.getHeight(), false);
                        publishProgress();
                    }
                } else if (y == resolution - 1) {
                    bitmap.setPixels(colors, 0, resolution, 0, resolution - resolution % updateRows, resolution, resolution % updateRows);
                    if (scaledBitmap != null) {
                        scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledBitmap.getWidth(), scaledBitmap.getHeight(), false);
                        publishProgress();
                    }
                }
            }

            if (scaledBitmap != null) {
                scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledBitmap.getWidth(), scaledBitmap.getWidth(), false);
            }

            Log.d("processing time", "Time: " + (System.nanoTime() - time));

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            invalidate();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            FractalView.this.startReal = startReal;
            FractalView.this.startImg = startImg;
            FractalView.this.range = range;
            FractalView.this.escapeValue = escapeValue;
            FractalView.this.resolution = resolution;
            FractalView.this.precision = precision;
            FractalView.this.updateRows = updateRows;

            invalidate();
            requestLayout();

            resetCalculatingTask();
        }

        @Override
        protected void onCancelled() {
            if (backup != null)
                bitmap = Bitmap.createBitmap(backup, 0, 0, backup.getWidth(), backup.getHeight());
            if (scaledBitmap != null && backup != null)
                scaledBitmap = Bitmap.createScaledBitmap(backup, scaledBitmap.getWidth(), scaledBitmap.getHeight(), false);
            invalidate();
            requestLayout();

            resetCalculatingTask();
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
    }

    public void recalculate() {
        if (calculatingTask == null || calculatingTask.isCancelled()) {
            calculatingTask = new CalculatingTask();
        }
        calculatingTask.execute();
    }

    public void cancel() {
        calculatingTask.cancel(true);
        calculatingTask = new CalculatingTask();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Define square size (the view isn't allowed to be a rectangle)
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        int size = Math.min(width, height);

        // Draw bitmap
        if (scaledBitmap != null) {
            if (scaledBitmap.getWidth() != size) {
                scaledBitmap = Bitmap.createScaledBitmap(scaledBitmap, size, size, false);
            }
        } else if (bitmap != null) {
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, size, size, false);
        }
        if (scaledBitmap != null) {
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
            if (checkTap(event)) {
                return false;
            }
            zoomIn();
            zoomStartX = zoomStartY = zoomEndX = zoomEndY = -1;
            recalculate();
            return true;
        }
        return super.onTouchEvent(event);
    }

    private boolean checkTap(MotionEvent e) {
        boolean xNotChanged = Math.abs(e.getX() - zoomStartX) <= 2;
        boolean yNotChanged = Math.abs(e.getY() - zoomStartY) <= 2;
        return xNotChanged && yNotChanged;
    }

    private void zoomIn() {
        double startReal = absoluteRealValue(zoomStartX);
        double endReal = absoluteRealValue(zoomEndX);
        double startImg = absoluteImaginaryValue(zoomStartY);
        double endImg = absoluteImaginaryValue(zoomEndY);

        double xRange = Math.abs(startReal - endReal);
        double yRange = Math.abs(startImg - endImg);

        calculatingTask.range = Math.max(xRange, yRange);
        calculatingTask.startReal = Math.min(startReal, endReal);
        calculatingTask.startImg = Math.max(startImg, endImg);
    }

    private void resetCalculatingTask() {
        calculatingTask = new CalculatingTask();
    }

    protected int resolveColor(int iterations) {
        // white --> green --> red --> blue
        double value = Math.pow(2, iterations / precision);
        //double value = Math.pow(-Math.log(iterations/precision), -1);
        //value = 1.0 / (value * value);
        // low value => blue
        // high value => white
        // 1.0 (highest) value => black
        if (value >= 1.0) return Color.BLACK;

        // 0.00 blue
        // 0.33 red
        // 0.67 green
        // 0.99 white

        // 0.00 => 255 blue
        // 0.33 => 0   blue
        int blue = (int) Math.max((1.0 - value / 0.33) * 255, 0);

        // 0.00 => 0   red
        // 0.33 => 255 red
        // 0.67 => 0   red
        int red = (int) Math.max((1.0 - Math.abs(value - 0.33) / 0.33) * 255, 0);
        int green = (int) Math.max((1.0 - Math.abs(value - 0.67) / 0.33) * 255, 0);

        // 0.67 => 0.0 white factor
        // 1.00 => 1.0 white factor
        double whiteFactor = Math.max((1.0 - Math.abs(value - 1.0) / 0.33), 0);

        // Whitify:
        // Only green has to be whitified because red and white can never mix.
        green += (255 - blue) * whiteFactor;
        red += (255 - red) * whiteFactor;
        blue += (255 - blue) * whiteFactor;

        Log.d("colors", "value: " + value);

        return Color.rgb(red, green, blue);
    }

    protected void init() {
        axisPaint = new Paint();
        zoomPaint = new Paint();
        axisPaint.setColor(Color.BLACK);
        zoomPaint.setARGB(128, 50, 50, 200);

        calculatingTask = new CalculatingTask();
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

    public int getResolution() {
        return resolution;
    }

    public void setResolution(int resolution) {
        calculatingTask.resolution = resolution;
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        calculatingTask.precision = precision;
    }

    public double getStartReal() {
        return startReal;
    }

    public void setStartReal(double startReal) {
        calculatingTask.startReal = startReal;
    }

    public double getStartImg() {
        return startImg;
    }

    public void setStartImg(double startImg) {
        calculatingTask.startImg = startImg;
    }

    public double getRange() {
        return range;
    }

    public void setRange(double range) {
        calculatingTask.range = range;
    }

    public double getEscapeValue() {
        return escapeValue;
    }

    public void setEscapeValue(double escapeValue) {
        calculatingTask.escapeValue = escapeValue;
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
