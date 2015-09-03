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

    protected double startReal = -2, startImg = 2, rangeReal = 4, rangeImg = 4, escapeValue = 2;
    protected int widthResolution = 512, heightResolution = 512, precision = 400, updateRows = 10;
    private double backupStartReal = -2, backupStartImg = 2, backupRangeReal = 4, backupRangeImg = 4, backupEscapeValue = 2;
    private int backupWidthResolution = 512, backupHeightResolution = 512, backupPrecision = 400, backupUpdateRows = 10;

    private int physicalWidth, physicalHeight;
    protected Bitmap bitmap;
    protected Bitmap scaledBitmap;
    private CalculatingTask calculatingTask;

    protected Paint axisPaint;
    protected Paint zoomPaint;

    private class CalculatingTask extends AsyncTask<Void, Void, Void> {
        double finalStartReal = -1, finalStartImg = -1, finalRangeReal = -1, finalRangeImg = -1, finalEscapeValue = -1;
        int finalWidthResolution = -1, finalHeightResolution = -1, finalPrecision = -1, finalUpdateRows = -1;
        Bitmap backupBitmap;
        boolean restoreBackup = true;

        OnCancelledListener onCancelledListener;

        @Override
        protected void onPreExecute() {
            // Copy new values to calculate with to the finalValues:
            finalStartReal = startReal;
            finalStartImg = startImg;
            finalRangeReal = rangeReal;
            finalRangeImg = rangeImg;
            finalEscapeValue = escapeValue;
            finalWidthResolution = widthResolution;
            finalHeightResolution = heightResolution;
            finalPrecision = precision;
            finalUpdateRows = updateRows;
        }

        @Override
        protected Void doInBackground(Void... params) {
            long time = System.nanoTime();
            if (bitmap != null) {
                backupBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
                if (bitmap.getWidth() != widthResolution || bitmap.getHeight() != heightResolution) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, finalWidthResolution, finalHeightResolution, false);
                }
            } else {
                bitmap = Bitmap.createBitmap(finalWidthResolution, finalHeightResolution, Bitmap.Config.RGB_565);
            }
            int[] progressLine = new int[finalWidthResolution];
            Arrays.fill(progressLine, Color.RED);

            int[] colors = new int[finalWidthResolution * finalUpdateRows];
            for (int y = 0; y < finalHeightResolution; y++) {
                if (isCancelled()) return null;
                for (int x = 0; x < finalWidthResolution; x++) {
                    double cReal = absoluteRealValue(x);
                    double cImg = absoluteImaginaryValue(y);
                    double zReal = cReal, zImg = cImg;

                    int iterations = 0;
                    while (zReal * zReal + zImg * zImg <= finalEscapeValue * finalEscapeValue && iterations < finalPrecision) {
                        double zRealNew = zReal * zReal - zImg * zImg + cReal;
                        zImg = 2 * zReal * zImg + cImg;
                        zReal = zRealNew;
                        iterations++;
                    }

                    colors[(y % finalUpdateRows) * finalWidthResolution + x] = iterations == finalPrecision ? Color.BLACK : Color.WHITE;
                }

                if (isCancelled()) return null;

                if ((y + 1) % finalUpdateRows == 0) {
                    bitmap.setPixels(colors, 0, finalWidthResolution, 0, y - finalUpdateRows + 1, finalWidthResolution, finalUpdateRows);
                    if (y + 1 != bitmap.getHeight()) {
                        bitmap.setPixels(progressLine, 0, finalWidthResolution, 0, y + 1, finalWidthResolution, 1);
                    }

                    scaledBitmap = Bitmap.createScaledBitmap(bitmap, physicalWidth, physicalHeight, false);
                    publishProgress();
                } else if (y == finalHeightResolution - 1) {
                    bitmap.setPixels(colors, 0, finalWidthResolution, 0, finalHeightResolution - finalHeightResolution % finalUpdateRows,
                            finalWidthResolution, finalHeightResolution % finalUpdateRows);
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
            Log.d("marshmallow", "onPostExecute");
            // Change the backup values to the new values which have just been calculated with.
            updateBackup();

            invalidate();
            requestLayout();
        }

        @Override
        protected void onCancelled() {
            if (restoreBackup) {
                // The backupValues don't (all) have to 'restore' anything. They are just there when you need them.
                // However, the bitmap (and scaledBitmap) have to be restored by the backupBitmap because onDraw only
                // draws scaledBitmap to the canvas and not backupBitmap.
                // Also the backup of the startReal/Img and rangeReal/Img have to be restored.
                bitmap = Bitmap.createBitmap(backupBitmap, 0, 0, backupBitmap.getWidth(), backupBitmap.getHeight());
                scaledBitmap = Bitmap.createScaledBitmap(backupBitmap, physicalWidth, physicalHeight, false);

                startReal = backupStartReal;
                startImg = backupStartImg;
                rangeReal = backupRangeReal;
                rangeImg = backupRangeImg;

                invalidate();
                requestLayout();
            } else {
                // Act as if the calculation is properly finished.
                updateBackup();
            }

            if (onCancelledListener != null) onCancelledListener.onCancelled();

            invalidate();
            requestLayout();
        }

        private void updateBackup() {
            backupStartReal = finalStartReal;
            backupStartImg = finalStartImg;
            backupRangeReal = finalRangeReal;
            backupRangeImg = finalRangeImg;
            backupEscapeValue = finalEscapeValue;
            backupWidthResolution = finalWidthResolution;
            backupHeightResolution = finalHeightResolution;
            backupPrecision = finalPrecision;
            backupUpdateRows = finalUpdateRows;
        }

        /**
         * The real value in the complex field represented by a column of virtual pixels.
         *
         * @param column The column of the 'virtual' pixels (defined in {@code widthResolution})
         * @return The real value in the complex field
         */
        protected double absoluteRealValue(int column) {
            return finalStartReal + finalRangeReal / finalWidthResolution * column;
        }

        /**
         * The imaginary value in the complex field represented by a row of virtual pixels.
         *
         * @param row The row of the 'virtual' pixels (defined in {@code heightResolution})
         * @return The imaginary value in the complex field
         */
        protected double absoluteImaginaryValue(int row) {
            return finalStartImg - finalRangeImg / finalHeightResolution * row;
        }

        public void setRestoreBackup(boolean restoreBackup) {
            this.restoreBackup = restoreBackup;
        }

        public void setOnCancelledListener(OnCancelledListener onCancelledListener) {
            this.onCancelledListener = onCancelledListener;
        }
    }

    /**
     * Listener with method invoked by CalculatingTask when onCancelled is called.
     * The reason that it's here and not inside the CalculatingTask is just that
     * an inner class cannot have a static declaration.
     */
    interface OnCancelledListener {
        void onCancelled();
    }

    public void recalculate() {
        if (calculatingTask != null
                && calculatingTask.getStatus() == AsyncTask.Status.RUNNING)
            calculatingTask.cancel(true);
        calculatingTask = new CalculatingTask();
        /*if (calculatingTask == null || calculatingTask.getStatus() != AsyncTask.Status.PENDING) {
            calculatingTask = new CalculatingTask();
        }*/
        calculatingTask.execute();
    }

    public void cancel() {
        if (calculatingTask != null) {
            calculatingTask.setRestoreBackup(true);
            calculatingTask.cancel(true);
        }
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

        applyDimensions(w, h, oldw, oldh);
        recalculate();
    }

    private void applyDimensions(int w, int h, int oldw, int oldh) {
        double rangeRealDiff = (w - oldw) / (double) oldw * rangeReal;
        double rangeImgDiff = (h - oldh) / (double) oldh * rangeImg;
        startReal = startReal - rangeRealDiff / 2.0;
        startImg = startImg + rangeImgDiff / 2.0;
        rangeReal = rangeReal + rangeRealDiff;
        rangeImg = rangeImg + rangeImgDiff;
        widthResolution = w / oldw * widthResolution;
        heightResolution = h / oldh * heightResolution;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw bitmap
        if (scaledBitmap != null) {
            canvas.drawBitmap(scaledBitmap, 0f, 0f, null);
        }

        // Draw axes
        float real0 = (float) (canvas.getWidth() * (-backupStartReal / backupRangeReal));
        float img0 = (float) (canvas.getHeight() * (backupStartImg / backupRangeImg));
        canvas.drawLine(real0, 0, real0, canvas.getHeight(), axisPaint);
        canvas.drawLine(0, img0, canvas.getWidth(), img0, axisPaint);

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
        OnCancelledListener listener = new OnCancelledListener() {
            @Override
            public void onCancelled() {
                double startReal = absoluteRealValue(zoomStartX);
                double endReal = absoluteRealValue(zoomEndX);
                double startImg = absoluteImaginaryValue(zoomStartY);
                double endImg = absoluteImaginaryValue(zoomEndY);

                zoomStartX = zoomStartY = zoomEndX = zoomEndY = -1;

                double xRange = Math.abs(startReal - endReal);
                double yRange = Math.abs(startImg - endImg);

                double bigFactor = backupRangeReal / backupRangeImg;
                double smallFactor = xRange / yRange;

                if (bigFactor > smallFactor) {
                    // Current screen is more horizontal than zoom frame
                    FractalView.this.startImg = Math.max(startImg, endImg);
                    FractalView.this.rangeImg = yRange;
                    FractalView.this.rangeReal = bigFactor / smallFactor * xRange;
                    FractalView.this.startReal = Math.min(startReal, endReal) - Math.abs(xRange - FractalView.this.rangeReal) / 2;
                } else {
                    // Current screen is more vertical than zoom frame
                    FractalView.this.startReal = Math.min(startReal, endReal);
                    FractalView.this.rangeReal = xRange;
                    FractalView.this.rangeImg = smallFactor / bigFactor * yRange;
                    FractalView.this.startImg = Math.max(startImg, endImg) + Math.abs(yRange - FractalView.this.rangeImg) / 2;
                }

                recalculate();
            }
        };

        if (calculatingTask != null && calculatingTask.getStatus() == AsyncTask.Status.RUNNING) {
            // Wait for the calculatingTask to be cancelled before executing code in listener.onCancelled()
            calculatingTask.setOnCancelledListener(listener);
            calculatingTask.setRestoreBackup(false);
            calculatingTask.cancel(true);
        } else {
            // Execute code directly
            listener.onCancelled();
        }
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
    }

    /**
     * The real value in the complex field represented by a device pixel in {@code scaledBitmap}.
     *
     * @param x The x position of the device pixel from which to retrieve the real value
     * @return The real value in the complex field
     */
    protected double absoluteRealValue(float x) {
        return backupStartReal + x / getWidth() * backupRangeReal;
    }

    /**
     * The imaginary value in the complex field represented by a device pixel in {@code scaledBitmap}.
     *
     * @param y The y position of the device pixel from which to retrieve the imaginary value
     * @return The imaginary value in the complex field
     */
    protected double absoluteImaginaryValue(float y) {
        return backupStartImg - y / getHeight() * backupRangeImg;
    }

    public void restoreZoom() {
        startReal = -2;
        rangeReal = rangeImg = 4;
        startImg = 2;

        int oldSizes = Math.min(getWidth(), getHeight());
        applyDimensions(getWidth(), getHeight(), oldSizes, oldSizes);
        recalculate();
    }

    public void setResolution(int resolution) {
        heightResolution = resolution * getHeight() / getWidth();
        widthResolution = resolution;
    }

    public int getResolution() {
        return backupWidthResolution;
    }

    public int getPrecision() {
        return backupPrecision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public double getEscapeValue() {
        return backupEscapeValue;
    }

    public void setEscapeValue(double escapeValue) {
        this.escapeValue = escapeValue;
    }

    public int getUpdateRows() {
        return backupUpdateRows;
    }

    public void setUpdateRows(int updateRows) {
        this.updateRows = updateRows;
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
