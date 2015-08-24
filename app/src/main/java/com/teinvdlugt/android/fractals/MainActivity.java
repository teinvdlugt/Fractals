package com.teinvdlugt.android.fractals;

import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    FractalView fractalView;
    EditText resolutionET, precisionET, escapeValueET;
    DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        resolutionET.setText(fractalView.getWidthResolution() + "");
        precisionET.setText(fractalView.getPrecision() + "");
        escapeValueET.setText(fractalView.getEscapeValue() + "");
    }

    private void initViews() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        fractalView = (FractalView) findViewById(R.id.fractalView);
        resolutionET = (EditText) findViewById(R.id.resolution);
        precisionET = (EditText) findViewById(R.id.precision);
        escapeValueET = (EditText) findViewById(R.id.escapeValue);
    }

    public void onClickApply(View view) {
        applyValues();
        drawerLayout.closeDrawer(GravityCompat.START);
        fractalView.recalculate();
    }

    public void applyValues() {
        try {
            int resolution = Integer.parseInt(resolutionET.getText().toString());
            fractalView.setResolution(resolution);
        } catch (NumberFormatException ignored) {/*ignored*/}
        try {
            int precision = Integer.parseInt(precisionET.getText().toString());
            fractalView.setPrecision(precision);
        } catch (NumberFormatException ignored) {/*ignored*/}
        try {
            double escapeValue = Double.parseDouble(escapeValueET.getText().toString());
            fractalView.setEscapeValue(escapeValue);
        } catch (NumberFormatException ignored) {/*ignored*/}
    }

    public void onClickRestoreZoom(View view) {
        applyValues();
        drawerLayout.closeDrawer(GravityCompat.START);
        fractalView.setStartReal(-2);
        fractalView.setStartImg(2);
        fractalView.setRangeReal(4);
        fractalView.recalculate();
    }

    public void onClickCancel(View view) {
        fractalView.cancel();
    }

    public void onClickSettings(View view) {
        drawerLayout.openDrawer(GravityCompat.START);
    }
}
