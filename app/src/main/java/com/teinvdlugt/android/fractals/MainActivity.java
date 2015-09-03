package com.teinvdlugt.android.fractals;

import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

public class MainActivity extends AppCompatActivity {

    FractalView fractalView;
    EditText resolutionET, precisionET, escapeValueET;
    DrawerLayout drawerLayout;
    CheckBox colorCB;
    Spinner fractalSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        resolutionET.setText(fractalView.getResolution() + "");
        precisionET.setText(fractalView.getPrecision() + "");
        escapeValueET.setText(fractalView.getEscapeValue() + "");

        setSpinnerAdapter();
        setCheckBox();
        setTextWatchers();
    }

    private void initViews() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        fractalView = (FractalView) findViewById(R.id.fractalView);
        resolutionET = (EditText) findViewById(R.id.resolution);
        precisionET = (EditText) findViewById(R.id.precision);
        escapeValueET = (EditText) findViewById(R.id.escapeValue);
        colorCB = (CheckBox) findViewById(R.id.colorCheckbox);
        fractalSpinner = (Spinner) findViewById(R.id.fractalSpinner);
    }

    private void setSpinnerAdapter() {
        String[] strings = {"Mandelbrot set", "Tricorn", "Burning ship", "Multibrot set (3)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, strings);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fractalSpinner.setAdapter(adapter);

        fractalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fractalView.setCurrentFractal(position);
            }

            public void onNothingSelected(AdapterView<?> parent) {/*ignored*/}
        });

        fractalSpinner.setSelection(fractalView.getCurrentFractal());
    }

    private void setCheckBox() {
        colorCB.setChecked(fractalView.isUseColor());
        colorCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                fractalView.setUseColor(isChecked);
            }
        });
    }

    private void setTextWatchers() {
        resolutionET.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    fractalView.setResolution(Integer.parseInt(resolutionET.getText().toString()));
                } catch (NumberFormatException ignored) {/*ignored*/}
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {/*ignored*/}

            public void afterTextChanged(Editable s) {/*ignored*/}
        });
        precisionET.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    fractalView.setPrecision(Integer.parseInt(precisionET.getText().toString()));
                } catch (NumberFormatException ignored) {/*ignored*/}
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {/*ignored*/}

            public void afterTextChanged(Editable s) {/*ignored*/}
        });
        escapeValueET.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    fractalView.setEscapeValue(Double.parseDouble(escapeValueET.getText().toString()));
                } catch (NumberFormatException ignored) {/*ignored*/}
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {/*ignored*/}

            public void afterTextChanged(Editable s) {/*ignored*/}
        });
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
        fractalView.restoreZoom();
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    public void onClickCancel(View view) {
        fractalView.cancel();
    }

    public void onClickSettings(View view) {
        drawerLayout.openDrawer(GravityCompat.START);
    }
}
