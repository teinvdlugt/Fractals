package com.teinvdlugt.android.fractals;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;

public class FractalView extends View {

    private double startReal = -2;
    private double startImg = 2;
    private double range = 4;
    private int resolution = 512;
    private int precision = 400;
    private ProgressBar progressBar;
    private Bitmap bitmap;
    private Paint paint = new Paint();

    public void recalculate() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int finalResolution = resolution;
                final int finalPrecision = precision;
                final int[] colors = new int[finalResolution * finalResolution];

                for (int y = 0; y < resolution; y++) {
                    for (int x = 0; x < resolution; x++) {
                        double cReal = startReal + range / finalResolution * x;
                        double cImg = startImg - range / finalResolution * y;
                        double zReal = cReal, zImg = cImg;

                        int i = 0;
                        while (zReal * zReal + zImg * zImg <= 4 && i < finalPrecision) {
                            double zRealNew = zReal * zReal - zImg * zImg + cReal;
                            zImg = 2 * zReal * zImg + cImg;
                            zReal = zRealNew;
                            i++;
                        }

                        colors[resolution * y + x] = i == finalPrecision ? Color.BLACK : Color.WHITE;
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
        paint.setColor(Color.BLACK);

        int width = canvas.getWidth();
        int height = canvas.getHeight();
        int size = Math.min(width, height);

        if (bitmap != null) {
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, size, size, false);
            canvas.drawBitmap(scaled, 0f, 0f, null);
        }

        // Draw y axis TODO doesn't work
        float xAxis = (float) (size * (-startReal / range));
        float yAxis = (float) (size * (startImg / range));
        canvas.drawLine(xAxis, 0, xAxis, size, paint);
        canvas.drawLine(0, yAxis, size, yAxis, paint);
    }


    public ProgressBar getProgressBar() {
        return progressBar;
    }

    /**
     * The {@code ProgressBar} to which the {@code FractalView} will report its progress.
     * If you want to detach the {@code ProgressBar} from the {@code FractalView}, pass null.
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
        this.resolution = resolution;
        progressBar.setMax(resolution);
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
