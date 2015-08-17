package com.teinvdlugt.android.fractals;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

public class MainActivity extends AppCompatActivity {

    FractalView fractalView;
    EditText resolutionET, precisionET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        resolutionET.setText(fractalView.getResolution() + "");
        precisionET.setText(fractalView.getPrecision() + "");

        fractalView.setProgressBar((ProgressBar) findViewById(R.id.progressBar));
        fractalView.recalculate();
    }

    private void initViews() {
        fractalView = (FractalView) findViewById(R.id.fractalView);
        resolutionET = (EditText) findViewById(R.id.resolution);
        precisionET = (EditText) findViewById(R.id.precision);
    }

    public void onClickApply(View view) {
        try {
            int resolution = Integer.parseInt(resolutionET.getText().toString());
            fractalView.setResolution(resolution);
        } catch (NumberFormatException ignored) {/*ignored*/}
        try {
            int precision = Integer.parseInt(precisionET.getText().toString());
            fractalView.setPrecision(precision);
        } catch (NumberFormatException ignored) {/*ignored*/}

        fractalView.recalculate();
    }
}
