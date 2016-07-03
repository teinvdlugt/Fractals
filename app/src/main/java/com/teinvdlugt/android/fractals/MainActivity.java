package com.teinvdlugt.android.fractals;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;

import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity {
    private static final String USE_NEW_VIEW_PREF = "use_new_view";

    private FirebaseAnalytics firebaseAnalytics;

    private AbstractFractalView fractalView;
    private FrameLayout fractalViewContainer;
    private EditText resolutionET, precisionET, escapeValueET, maxColorIterationsET, colorDistributionET;
    private CheckBox colorCB;
    private Spinner fractalSpinner;
    private SwitchCompat useNewViewSwitch;
    private View bottomSheet;
    private BottomSheetBehavior bottomSheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        setContentView(R.layout.activity_main);

        initViews();
        makeTabletLayout();
        setUseNewView();
        setSpinnerAdapter();
        setCheckBox();
        setSwitchListener();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setEditTextTexts();
    }

    private void initViews() {
        bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        //fractalView = (FractalView2) findViewById(R.id.fractalView);
        fractalViewContainer = (FrameLayout) findViewById(R.id.fractalView_container);
        resolutionET = (EditText) findViewById(R.id.resolution);
        precisionET = (EditText) findViewById(R.id.precision);
        escapeValueET = (EditText) findViewById(R.id.escapeValue);
        colorCB = (CheckBox) findViewById(R.id.colorCheckbox);
        fractalSpinner = (Spinner) findViewById(R.id.fractalSpinner);
        maxColorIterationsET = (EditText) findViewById(R.id.maxColorIterations_editText);
        colorDistributionET = (EditText) findViewById(R.id.colorDistribution_editText);
        useNewViewSwitch = (SwitchCompat) findViewById(R.id.useNewView_switch);
    }

    private void makeTabletLayout() {
        bottomSheet.post(new Runnable() {
            @Override
            public void run() {
                float maxBottomSheetWidth = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 620, getResources().getDisplayMetrics());
                if (bottomSheet.getWidth() > maxBottomSheetWidth) {
                    int leftRightMargin = (int) ((bottomSheet.getWidth() - maxBottomSheetWidth) / 2);
                    CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) bottomSheet.getLayoutParams();
                    params.leftMargin = params.rightMargin = leftRightMargin;
                }
            }
        });
    }

    private void setUseNewView() {
        boolean useNewView = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(USE_NEW_VIEW_PREF, false);
        useNewViewSwitch.setChecked(useNewView);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        if (useNewView) {
            fractalView = new FractalView2(MainActivity.this);
        } else {
            fractalView = new FractalView(MainActivity.this);
        }
        fractalViewContainer.removeAllViews();
        fractalViewContainer.addView(fractalView, params);
    }

    @SuppressLint("SetTextI18n")
    private void setEditTextTexts() {
        precisionET.setText(fractalView.getPrecision() + "");
        escapeValueET.setText(fractalView.getEscapeValue() + "");
        maxColorIterationsET.setText(fractalView.getMaxColorIterations() + "");
        colorDistributionET.setText(fractalView.getColorDistribution() + "");

        try {
            resolutionET.setText(fractalView.getResolution() + "");
        } catch (UnsupportedOperationException e) {
            resolutionET.setText(getString(R.string.not_yet_available));
            resolutionET.setEnabled(false);
        }
    }

    private void setSpinnerAdapter() {
        String[] strings = fractalView.getAvailableFractals();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, strings);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fractalSpinner.setAdapter(adapter);

        fractalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fractalView.setFractal(position);
            }

            public void onNothingSelected(AdapterView<?> parent) {/*ignored*/}
        });

        fractalSpinner.setSelection(fractalView.getFractal());
    }

    private void setCheckBox() {
        colorCB.setChecked(fractalView.getUseColor());
        colorCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                fractalView.setUseColor(isChecked);
            }
        });
    }

    private void setSwitchListener() {
        useNewViewSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onSwitchChanged(isChecked);
            }
        });
    }

    private void onSwitchChanged(boolean isChecked) {
        if (fractalView != null) fractalView.onClickCancel();
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean(USE_NEW_VIEW_PREF, isChecked).apply();
        recreate();
    }

    public void onClickApply(View view) {
        applyValues();
        fractalView.onClickApply();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    public void applyValues() {
        try {
            int precision = Integer.parseInt(precisionET.getText().toString());
            fractalView.setPrecision(precision);
        } catch (NumberFormatException ignored) {}
        try {
            double escapeValue = Double.parseDouble(escapeValueET.getText().toString());
            fractalView.setEscapeValue(escapeValue);
        } catch (NumberFormatException ignored) {}
        try {
            double maxColorIterations = Double.parseDouble(maxColorIterationsET.getText().toString());
            fractalView.setMaxColorIterations(maxColorIterations);
        } catch (NumberFormatException ignored) {}
        try {
            int resolution = Integer.parseInt(resolutionET.getText().toString());
            fractalView.setResolution(resolution);
        } catch (NumberFormatException ignored) {}
        try {
            double colorDistribution = Double.parseDouble(colorDistributionET.getText().toString());
            fractalView.setColorDistribution(colorDistribution);
        } catch (NumberFormatException ignored) {}
    }

    public void onClickRestoreZoom(View view) {
        applyValues();
        fractalView.onClickRestoreZoom();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    public void onClickCancel(View view) {
        fractalView.onClickCancel();
    }

    public void onClickSettings(View view) {
        switch (bottomSheetBehavior.getState()) {
            case BottomSheetBehavior.STATE_HIDDEN:
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                break;
            case BottomSheetBehavior.STATE_EXPANDED:
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                break;
            case BottomSheetBehavior.STATE_COLLAPSED:
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                break;
        }
    }

    public void onClickSettingsBar(View view) {
        switch (bottomSheetBehavior.getState()) {
            case BottomSheetBehavior.STATE_EXPANDED:
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                break;
            case BottomSheetBehavior.STATE_COLLAPSED:
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                break;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && Build.VERSION.SDK_INT >= 19) {
            fractalViewContainer.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}
