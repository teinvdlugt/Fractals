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
    protected double rangeReal = 4;
    protected double rangeImg = 4;
    protected int widthResolution = 512;
    protected int heightResolution = 512;
    protected int precision = 400;
    /**
     * The bitmap will be updated whilst calculating with for example a new resolution.
     * {@code updateRows} is the number of rows to include in one update. The higher the
     * value the higher the performance, but the used memory will increase a bit.
     */
    protected int updateRows = 10;
    protected double escapeValue = 2;
    private int physicalWidth, physicalHeight;
    protected Bitmap bitmap;
    protected Bitmap scaledBitmap;
    protected Paint axisPaint;
    protected Paint zoomPaint;
    private CalculatingTask calculatingTask;

    private class CalculatingTask extends AsyncTask<Void, Void, Void> {
        double startReal = -1, startImg = -1, rangeReal = -1, rangeImg = -1, escapeValue = -1;
        int widthResolution = -1, heightResolution = -1, precision = -1, updateRows = -1;
        Bitmap backup;

        @Override
        protected void onPreExecute() {
            // Copy these values so that changes to the FractalView values will
            // not affect calculating process.
            if (startReal == -1) startReal = FractalView.this.startReal;
            if (startImg == -1) startImg = FractalView.this.startImg;
            if (rangeReal == -1) rangeReal = FractalView.this.rangeReal;
            if (rangeImg == -1) rangeImg = FractalView.this.rangeImg;
            if (escapeValue == -1) escapeValue = FractalView.this.escapeValue;
            if (widthResolution == -1) widthResolution = FractalView.this.widthResolution;
            if (heightResolution == -1) heightResolution = FractalView.this.heightResolution;
            if (precision == -1) precision = FractalView.this.precision;
            if (updateRows == -1) updateRows = FractalView.this.updateRows;
        }

        @Override
        protected Void doInBackground(Void... params) {
            long time = System.nanoTime();
            if (bitmap != null) {
                backup = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
                if (bitmap.getWidth() != widthResolution || bitmap.getHeight() != heightResolution) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, widthResolution, heightResolution, false);
                }
            } else {
                bitmap = Bitmap.createBitmap(widthResolution, heightResolution, Bitmap.Config.RGB_565);
            }
            int[] progressLine = new int[widthResolution];
            Arrays.fill(progressLine, Color.RED);

            int[] colors = new int[widthResolution * updateRows];
            for (int y = 0; y < heightResolution; y++) {
                if (isCancelled()) return null;
                for (int x = 0; x < widthResolution; x++) {
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

                    colors[(y % updateRows) * widthResolution + x] = iterations == precision ? Color.BLACK : Color.WHITE;
                }

                if (isCancelled()) return null;

                if ((y + 1) % updateRows == 0) {
                    bitmap.setPixels(colors, 0, widthResolution, 0, y - updateRows + 1, widthResolution, updateRows);
                    if (y + 1 != bitmap.getHeight()) {
                        bitmap.setPixels(progressLine, 0, widthResolution, 0, y + 1, widthResolution, 1);
                    }

                    scaledBitmap = Bitmap.createScaledBitmap(bitmap, physicalWidth, physicalHeight, false);
                    publishProgress();
                } else if (y == heightResolution - 1) {
                    bitmap.setPixels(colors, 0, widthResolution, 0, heightResolution - heightResolution % updateRows,
                            widthResolution, heightResolution % updateRows);
                    scaledBitmap = Bitmap.createScaledBitmap(bitmap, physicalWidth, physicalHeight, false);
                    publishProgress();
                }
            }

            scaledBitmap = Bitmap.createScaledBitmap(bitmap, physicalWidth, physicalHeight, false);

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
            FractalView.this.rangeReal = rangeReal;
            FractalView.this.rangeImg = rangeImg;
            FractalView.this.escapeValue = escapeValue;
            FractalView.this.widthResolution = widthResolution;
            FractalView.this.heightResolution = heightResolution;
            FractalView.this.precision = precision;
            FractalView.this.updateRows = updateRows;

            invalidate();
            requestLayout();

            resetCalculatingTask();
        }

        @Override
        protected void onCancelled() {
            if (backup != null) {
                bitmap = Bitmap.createBitmap(backup, 0, 0, backup.getWidth(), backup.getHeight());
                scaledBitmap = Bitmap.createScaledBitmap(backup, physicalWidth, physicalHeight, false);
            }

            invalidate();
            requestLayout();

            resetCalculatingTask();
        }

        /**
         * The real value in the complex field represented by a column of virtual pixels.
         *
         * @param column The column of the 'virtual' pixels (defined in {@code widthResolution})
         * @return The real value in the complex field
         */
        protected double absoluteRealValue(int column) {
            return startReal + rangeReal / widthResolution * column;
        }

        /**
         * The imaginary value in the complex field represented by a row of virtual pixels.
         *
         * @param row The row of the 'virtual' pixels (defined in {@code heightResolution})
         * @return The imaginary value in the complex field
         */
        protected double absoluteImaginaryValue(int row) {
            return startImg - rangeImg / heightResolution * row;
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
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cancel();
        physicalWidth = w;
        physicalHeight = h;

        if (oldw == 0 && oldh == 0) {
            // Pretend to be coming from a square, because standard values
            // (startReal -2, rangeReal 4, startImg 2, rangeImg 4) 'presume'
            // a square layout
            oldw = oldh = Math.min(w, h);
        }

        double rangeRealDiff = (w - oldw) / (double) oldw * rangeReal;
        double rangeImgDiff = (h - oldh) / (double) oldh * rangeImg;
        startReal = startReal - rangeRealDiff / 2.0;
        startImg = startImg + rangeImgDiff / 2.0;
        rangeReal = rangeReal + rangeRealDiff;
        rangeImg = rangeImg + rangeImgDiff;
        widthResolution = w / oldw * widthResolution;
        heightResolution = h / oldh * heightResolution;

        //recalculate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw bitmap
        if (scaledBitmap != null) {
            canvas.drawBitmap(scaledBitmap, 0f, 0f, null);
        }

        // Draw axes
        float xAxis = (float) (canvas.getWidth() * (-startReal / rangeReal));
        float yAxis = (float) (canvas.getHeight() * (startImg / rangeImg));
        canvas.drawLine(xAxis, 0, xAxis, canvas.getHeight(), axisPaint);
        canvas.drawLine(0, yAxis, canvas.getWidth(), yAxis, axisPaint);

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
                zoomStartX = zoomStartY = zoomEndX = zoomEndY = -1;
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
        // TODO: 24-8-2015 !!!
        double startReal = absoluteRealValue(zoomStartX);
        double endReal = absoluteRealValue(zoomEndX);
        double startImg = absoluteImaginaryValue(zoomStartY);
        double endImg = absoluteImaginaryValue(zoomEndY);

        double xRange = Math.abs(startReal - endReal);
        double yRange = Math.abs(startImg - endImg);

        calculatingTask.rangeReal = Math.max(xRange, yRange);
        calculatingTask.startReal = Math.min(startReal, endReal);
        calculatingTask.startImg = Math.max(startImg, endImg);
    }

    private void resetCalculatingTask() {
        calculatingTask = new CalculatingTask();
    }

    protected int resolveColor(int iterations, int precision) {
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
        // TODO: 24-8-2015 !!!
        if (scaledBitmap != null) {
            return startReal + x / scaledBitmap.getWidth() * rangeReal;
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
        // TODO: 24-8-2015 !!!
        if (scaledBitmap != null) {
            return startImg - y / scaledBitmap.getHeight() * rangeReal;
        }
        return -1;
    }

    public void setResolution(int resolution) {
        calculatingTask.heightResolution = resolution / calculatingTask.widthResolution * calculatingTask.heightResolution;
        calculatingTask.widthResolution = resolution;
    }

    public int getResolution() {
        return widthResolution;
    }

    public int getWidthResolution() {
        return widthResolution;
    }

    public void setWidthResolution(int widthResolution) {
        calculatingTask.widthResolution = widthResolution;
    }

    public int getHeightResolution() {
        return heightResolution;
    }

    public void setHeightResolution(int heightResolution) {
        calculatingTask.heightResolution = heightResolution;
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

    public double getRangeReal() {
        return rangeReal;
    }

    public void setRangeReal(double rangeReal) {
        calculatingTask.rangeReal = rangeReal;
    }

    public double getRangeImg() {
        return rangeImg;
    }

    public void setRangeImg(double rangeImg) {
        calculatingTask.rangeImg = rangeImg;
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
