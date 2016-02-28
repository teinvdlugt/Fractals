package com.teinvdlugt.android.fractals;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public abstract class AbstractFractalView extends View {
    public abstract int getPrecision();
    public abstract void setPrecision(int precision);
    public abstract double getEscapeValue();
    public abstract void setEscapeValue(double escapeValue);
    public abstract double getMaxColorIterations();
    public abstract void setMaxColorIterations(double maxColorIterations);
    public abstract double getColorDistribution();
    public abstract void setColorDistribution(double colorDistribution);
    public abstract boolean getUseColor();
    public abstract void setUseColor(boolean useColor);
    public abstract int getResolution();
    public abstract void setResolution(int resolution);

    public abstract String[] getAvailableFractals();
    public abstract void setFractal(int fractal);
    public abstract int getFractal();

    public abstract void onClickApply();
    public abstract void onClickRestoreZoom();
    public abstract void onClickCancel();

    public AbstractFractalView(Context context) {
        super(context);
    }

    public AbstractFractalView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AbstractFractalView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
