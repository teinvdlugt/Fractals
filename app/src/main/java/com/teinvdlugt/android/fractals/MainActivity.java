package com.teinvdlugt.android.fractals;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FractalView fractalView = (FractalView) findViewById(R.id.fractalView);
        fractalView.recalculate();
    }
}
