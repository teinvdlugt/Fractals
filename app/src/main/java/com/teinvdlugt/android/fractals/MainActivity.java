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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Spinner;

import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity {
    private static final String USE_NEW_VIEW_PREF = "use_new_view";

    private FirebaseAnalytics firebaseAnalytics;

    private AbstractFractalView fractalView;
    private FrameLayout fractalViewContainer;
    private EditText resolutionET, precisionET, escapeValueET, maxColorIterationsET, colorDistributionET;
    private SeekBar resolutionSB, precisionSB, escapeValueSB, maxColorIterationsSB, colorDistributionSB;
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
        setTextWatchers();
        makeTabletLayout();
        setUseNewView();
        setSpinnerAdapter();
        setCheckBox();
        setSwitchListener();
        setOnSystemUiVisibilityChangeListener();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setEditTextTexts();
        setSeekBarProgresses();
    }

    private void initViews() {
        bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        fractalViewContainer = (FrameLayout) findViewById(R.id.fractalView_container);

        resolutionET = (EditText) findViewById(R.id.resolution_editText);
        precisionET = (EditText) findViewById(R.id.precision_editText);
        escapeValueET = (EditText) findViewById(R.id.escapeValue_editText);
        maxColorIterationsET = (EditText) findViewById(R.id.maxColorIterations_editText);
        colorDistributionET = (EditText) findViewById(R.id.colorDistribution_editText);

        resolutionSB = (SeekBar) findViewById(R.id.resolution_seekBar);
        precisionSB = (SeekBar) findViewById(R.id.precision_seekBar);
        escapeValueSB = (SeekBar) findViewById(R.id.escapeValue_seekBar);
        maxColorIterationsSB = (SeekBar) findViewById(R.id.maxColorIterations_seekBar);
        colorDistributionSB = (SeekBar) findViewById(R.id.colorDistribution_seekBar);

        // SeekBar listeners
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onSeekBarChanged(seekBar, progress, fromUser);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}

            public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        resolutionSB.setOnSeekBarChangeListener(listener);
        precisionSB.setOnSeekBarChangeListener(listener);
        escapeValueSB.setOnSeekBarChangeListener(listener);
        maxColorIterationsSB.setOnSeekBarChangeListener(listener);
        colorDistributionSB.setOnSeekBarChangeListener(listener);

        colorCB = (CheckBox) findViewById(R.id.colorCheckbox);
        fractalSpinner = (Spinner) findViewById(R.id.fractalSpinner);
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
        try {
            resolutionET.setText(fractalView.getResolution() + "");
        } catch (UnsupportedOperationException e) {
            resolutionET.setText(getString(R.string.not_yet_available));
            resolutionET.setEnabled(false);
        }

        precisionET.setText(fractalView.getPrecision() + "");
        escapeValueET.setText(fractalView.getEscapeValue() + "");
        maxColorIterationsET.setText(fractalView.getMaxColorIterations() + "");
        colorDistributionET.setText(fractalView.getColorDistribution() + "");
    }

    private void setSeekBarProgresses() {
        try {
            setSeekBar(resolutionSB, fractalView.getResolution());
        } catch (UnsupportedOperationException e) {
            resolutionSB.setEnabled(false);
        }

        setSeekBar(precisionSB, fractalView.getPrecision());
        setSeekBar(escapeValueSB, (int) (fractalView.getEscapeValue() * 100));
        setSeekBar(maxColorIterationsSB, (int) fractalView.getMaxColorIterations());
        setSeekBar(colorDistributionSB, (int) (fractalView.getColorDistribution() * 100));
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

    private void setOnSystemUiVisibilityChangeListener() {
        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0 && Build.VERSION.SDK_INT >= 19) {
                    decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
            }
        });
    }

    private void onSwitchChanged(boolean isChecked) {
        if (fractalView != null) fractalView.onClickCancel();
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean(USE_NEW_VIEW_PREF, isChecked).apply();
        recreate();
    }

    private void onSeekBarChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser || seekBar == null)
            return;

        if (seekBar.equals(resolutionSB)) {
            resolutionET.setText(progress + "");
        } else if (seekBar.equals(precisionSB)) {
            precisionET.setText(progress + "");
        } else if (seekBar.equals(escapeValueSB)) {
            escapeValueET.setText(progress / 100. + "");
        } else if (seekBar.equals(maxColorIterationsSB)) {
            maxColorIterationsET.setText(progress + "");
        } else if (seekBar.equals(colorDistributionSB)) {
            colorDistributionET.setText(progress / 100 + "");
        }
    }

    private void setTextWatchers() {
        resolutionET.addTextChangedListener(new TextWatcher() { // TODO: 4-7-2016 Does resolution work?
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                int newValue = parseInteger(resolutionET.getText().toString());
                if (newValue != -1) {
                    setSeekBar(resolutionSB, newValue);
                    fractalView.setResolution(newValue);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        precisionET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                int newValue = parseInteger(precisionET.getText().toString());
                if (newValue != -1) {
                    setSeekBar(precisionSB, newValue);
                    fractalView.setPrecision(newValue);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        escapeValueET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                double newValue = parseDouble(escapeValueET.getText().toString());
                if (newValue != -1) {
                    setSeekBar(escapeValueSB, (int) (newValue * 100));
                    fractalView.setEscapeValue(newValue);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        maxColorIterationsET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                double newValue = parseDouble(maxColorIterationsET.getText().toString());
                if (newValue != -1) {
                    setSeekBar(maxColorIterationsSB, (int) newValue);
                    fractalView.setMaxColorIterations(newValue);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        colorDistributionET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                double newValue = parseDouble(colorDistributionET.getText().toString());
                if (newValue != -1) {
                    setSeekBar(colorDistributionSB, (int) (newValue * 100));
                    fractalView.setColorDistribution(newValue);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
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
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private static void setSeekBar(SeekBar seekBar, int progress) {
        seekBar.setProgress(Math.min(progress, seekBar.getMax()));
    }

    private static int parseInteger(String string) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private static double parseDouble(String string) {
        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return -1;
        }
    }
}
