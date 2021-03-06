package org.ereuse.scanner.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import org.ereuse.scanner.R;
import org.ereuse.scanner.data.Device;
import org.ereuse.scanner.data.Grade;
import org.ereuse.scanner.data.GradeOption;
import org.ereuse.scanner.data.Manufacturer;
import org.ereuse.scanner.services.AsyncService;
import org.ereuse.scanner.services.api.ApiException;
import org.ereuse.scanner.services.api.ApiResponse;
import org.ereuse.scanner.services.api.ManufacturersResponse;
import org.ereuse.scanner.services.api.SnapshotResponse;
import org.ereuse.scanner.utils.ScanUtils;
import org.ereuse.scanner.utils.SearchableListAdapter;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * Created by Jamgo SCCL.
 */
public class SnapshotActivity extends ScanActivity {

    public static final String EXTRA_MODE = "mode";
    public static final String MODE_SELF = "self";
    public static final String MODE_EXTERNAL_DEVICE = "External";
    //As per eReuse request, until they can provide this information dinamically this will be a static list
    private static final Map<String, List<String>> DEVICETYPES;

    static {
        Map<String, List<String>> deviceTypesMap = new HashMap<String, List<String>>();
        deviceTypesMap.put("Computer", new ArrayList<String>(Arrays.asList("Netbook", "Laptop", "Microtower", "Server")));
        deviceTypesMap.put("ComputerMonitor", new ArrayList<String>(Arrays.asList("TFT", "LCD", "OLED", "LED")));
        deviceTypesMap.put("Peripheral", new ArrayList<String>(Arrays.asList("Router", "Switch", "Printer",
                "Scanner", "MultifunctionPrinter", "Terminal", "HUB", "SAI",
                "Keyboard", "Mouse", "WirelessAccessPoint", "LabelPrinter", "Projector", "VideoconferenceDevice",
                "WirelessMicrophone", "Scaler", "VideoScaler", "MemoryCardReader", "Amplifier", "AudioAmplifier")));
        DEVICETYPES = Collections.unmodifiableMap(deviceTypesMap);
    }

    EditText serialNumberEditText;
    EditText modelEditText;
    Button manufacturerButton;
    EditText licenseKeyEditText;
    EditText giverEditText;
    EditText refurbishedEditText;
    EditText systemEditText;
    EditText commentsEditText;
    Spinner deviceTypeSpinner;
    Spinner deviceSubTypeSpinner;
    String deviceType;
    String deviceSubType;
    Boolean deviceSubTypeSpinnerReloadUpdate = false;

    Button gradeAppearanceButton;
    Button gradeFunctionalityButton;
    Button gradeBiosButton;
    CheckBox gradeLabelsCheckBox;
    Grade gradeConditions = new Grade();
    private String mode;
    private AlertDialog gradeSelectorDialog = null;
    private AlertDialog searchableSelectorDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snapshot);
        setToolbar();

        this.mode = this.getIntent().getStringExtra(EXTRA_MODE);

        if (CollectionUtils.isEmpty(this.getScannerApplication().getManufacturers())) {
            retrieveManufacturers();
        }

        this.initLayout();
    }

    private void initLayout() {
        this.manufacturerButton = (Button) this.findViewById(R.id.snapshotManufacturer);
        this.modelEditText = (EditText) this.findViewById(R.id.snapshotModelEditText);
        this.serialNumberEditText = (EditText) this.findViewById(R.id.snapshotSerialNumberEditText);
        this.licenseKeyEditText = (EditText) this.findViewById(R.id.snapshotLicenseEditText);

        this.giverEditText = (EditText) this.findViewById(R.id.snapshotGiverEditText);
        this.refurbishedEditText = (EditText) this.findViewById(R.id.snapshotRefurbisherEditText);
        this.systemEditText = (EditText) this.findViewById(R.id.snapshotSystemEditText);

        this.commentsEditText = (EditText) this.findViewById(R.id.snapshotCommentsEditText);
        this.deviceTypeSpinner = (Spinner) this.findViewById(R.id.snapshotDeviceType);
        this.deviceSubTypeSpinner = (Spinner) this.findViewById(R.id.snapshotDeviceSubType);

        this.initGradeSpinners();

        this.gradeLabelsCheckBox = (CheckBox) this.findViewById(R.id.gradeLabelsCheckBox);

        if (MODE_SELF.equals(this.mode)) {
            initializeSelfSnapshotLayout();
        } else {
            initializeExternalDeviceSnapshotLayout();
        }
    }

    private void initGradeSpinners() {
        this.gradeAppearanceButton = (Button) this.findViewById(R.id.gradeAppearanceValueLabel);
        this.gradeFunctionalityButton = (Button) this.findViewById(R.id.gradeFunctionalityValueLabel);
        this.gradeBiosButton = (Button) this.findViewById(R.id.gradeBiosValueLabel);
    }

    private void initializeSelfSnapshotLayout() {
        this.deviceType = "Mobile";
        this.deviceSubType = "Smartphone";

        TextView deviceTypeLabel = (TextView) this.findViewById(R.id.snapshotDeviceTypeLabel);
        deviceTypeLabel.setVisibility(View.GONE);
        this.deviceTypeSpinner.setVisibility(View.GONE);
        this.deviceSubTypeSpinner.setVisibility(View.GONE);

        this.manufacturerButton.setText(Build.MANUFACTURER);
        this.modelEditText.setText(Build.MODEL);
        this.modelEditText.setFocusable(false);
        this.serialNumberEditText.setText(Build.SERIAL);
        this.serialNumberEditText.setFocusable(false);

        this.hideScanButtons();

        TextView commentsLabel = (TextView) this.findViewById(R.id.snapshotCommentsLabel);
        commentsLabel.setVisibility(View.GONE);
        this.commentsEditText.setVisibility(View.GONE);
    }

    private void initializeExternalDeviceSnapshotLayout() {
        String[] spinnerDeviceTypeValues = DEVICETYPES.keySet().toArray(new String[DEVICETYPES.keySet().size()]);
        ArrayAdapter<String> deviceTypeAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, spinnerDeviceTypeValues);
        deviceTypeAdapter.setDropDownViewResource(R.layout.spinner_item);
        this.deviceTypeSpinner.setAdapter(deviceTypeAdapter);

        this.deviceTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedDeviceType = (String) adapterView.getItemAtPosition(i);
                deviceType = selectedDeviceType;
                String[] spinnerDeviceSubTypeValues = DEVICETYPES.get(selectedDeviceType).toArray(new String[DEVICETYPES.get(selectedDeviceType).size()]);
                ArrayAdapter<String> deviceSubTypeAdapter = new ArrayAdapter<String>(SnapshotActivity.this, R.layout.spinner_item, spinnerDeviceSubTypeValues);
                if (deviceSubTypeSpinnerReloadUpdate) {
                    deviceSubTypeSpinnerReloadUpdate = false;
                } else {
                    deviceSubTypeSpinner.setAdapter(deviceSubTypeAdapter);
                    deviceSubTypeSpinner.setVisibility(View.VISIBLE);
                }
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        this.deviceSubTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedDeviceSubType = (String) adapterView.getItemAtPosition(i);
                deviceSubType = selectedDeviceSubType;
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        this.reloadLatestSuccessfulSnapshotData();

        this.deviceSubTypeSpinner.setVisibility(View.VISIBLE);
    }

    private void hideScanButtons() {
        ImageButton serialNumberScanButton = (ImageButton) this.findViewById(R.id.snapshotSerialNumberScanButton);
        ImageButton modelScanButton = (ImageButton) this.findViewById(R.id.snapshotModelScanButton);
        ImageButton licenseScanButton = (ImageButton) this.findViewById(R.id.snapshotLicenseScanButton);

        serialNumberScanButton.setVisibility(View.GONE);
        modelScanButton.setVisibility(View.GONE);
        licenseScanButton.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLogin();
    }

    public String getSelectedManufacturer() {
        return this.manufacturerButton.getText().toString();
    }

    public void sendSnapshot(View view) {
        if (doValidate()) {
            String serialNumber = this.serialNumberEditText.getText().toString();
            String model = this.modelEditText.getText().toString();
            String manufacturer = this.getSelectedManufacturer();
            String licenseKey = this.licenseKeyEditText.getText().toString();

            String giverId = this.giverEditText.getText().toString();
            String refurbisherId = this.refurbishedEditText.getText().toString();
            String systemId = this.systemEditText.getText().toString();

            String comment = this.commentsEditText.getText().toString();

            AsyncService asyncService = new AsyncService(this);
            asyncService.doSnapshot(this.getServer(), this.getUser(), deviceType, deviceSubType, serialNumber, model, manufacturer, licenseKey, giverId, refurbisherId, systemId, comment, gradeConditions);
        }

    }

    private boolean doValidate() {
        List<String> mandatoryEmptyFields = new ArrayList<String>();

        if (this.serialNumberEditText.getText().toString().isEmpty()) {
            mandatoryEmptyFields.add(getString(R.string.snapshot_serial_number_label));
        }
        if (this.modelEditText.getText().toString().isEmpty()) {
            mandatoryEmptyFields.add(getString(R.string.snapshot_model_label));

        }
        if (this.manufacturerButton.getText().toString().isEmpty() || this.manufacturerButton.getText().toString().equals(getString(R.string.snapshot_select_text))) {
            mandatoryEmptyFields.add(getString(R.string.snapshot_manufacturer_label));
        }

        if (!mandatoryEmptyFields.isEmpty()) {
            String errorMessage = getString(R.string.empty_fields_error_message) + "\n";
            for (String emptyField : mandatoryEmptyFields) {
                errorMessage += "\n - " + emptyField;
            }
            launchActionMessageDialog(getString(R.string.dialog_validation_error_title), errorMessage);
            return false;
        }

        return true;
    }

    @Override
    public void onSuccess(ApiResponse response) {
        super.onSuccess(response);

        if (response.getClass().equals(ManufacturersResponse.class)) {
            ManufacturersResponse manufacturersResponse = (ManufacturersResponse) response;
            this.getScannerApplication().getManufacturers().addAll(manufacturersResponse.getManufacturers());
            if (manufacturersResponse.getLinks().getNext() != null) {
                retrieveManufacturers(manufacturersResponse.getLinks().getNext().getHref());
            }


        } else {
            SnapshotResponse snapshotResponse = (SnapshotResponse) response;

            if (snapshotResponse.getStatus().equals(getString(R.string.server_response_status_ok))) {

                this.resetUniqueFields();
                this.saveSuccessfulSnapshotData();

                launchActionMessageDialog(getString(R.string.snapshot_success), true);
            }
        }

    }

    @Override
    public void onError(ApiException exception) {
        super.onError(exception);

        this.getScannerApplication().setServer(null);
        this.getScannerApplication().setUser(null);
    }

    private void resetUniqueFields() {
        this.serialNumberEditText.setText("");
        this.licenseKeyEditText.setText("");
        this.giverEditText.setText("");
        this.refurbishedEditText.setText("");
        this.systemEditText.setText("");
    }

    private void saveSuccessfulSnapshotData() {
        Device successSnapshot = new Device();
        successSnapshot.setModel(this.modelEditText.getText().toString());
        successSnapshot.setManufacturer(this.manufacturerButton.getText().toString());
        successSnapshot.setDeviceType(this.deviceType);
        successSnapshot.setDeviceSubType(this.deviceSubType);
        this.getScannerApplication().setLatestSuccessfulSnapshot(successSnapshot);
    }

    private void reloadLatestSuccessfulSnapshotData() {
        Device successSnapshot = this.getScannerApplication().getLatestSuccessfulSnapshot();

        if (successSnapshot != null) {
            this.modelEditText.setText(successSnapshot.getModel());
            this.manufacturerButton.setText(successSnapshot.getManufacturer());

            String[] spinnerDeviceSubTypeValues = DEVICETYPES.get(successSnapshot.getDeviceType()).toArray(new String[DEVICETYPES.get(successSnapshot.getDeviceType()).size()]);
            int deviceTypePosition = 0;
            for (String eachDeviceType : DEVICETYPES.keySet()) {
                if (eachDeviceType.equals(successSnapshot.getDeviceType())) {
                    break;
                }
                deviceTypePosition++;
            }
            this.deviceTypeSpinner.setSelection(deviceTypePosition);
            ArrayAdapter<String> deviceSubTypeAdapter = new ArrayAdapter<String>(SnapshotActivity.this, R.layout.spinner_item, spinnerDeviceSubTypeValues);

            this.deviceSubTypeSpinner.setAdapter(deviceSubTypeAdapter);


            this.deviceType = successSnapshot.getDeviceType();
            this.deviceSubType = successSnapshot.getDeviceSubType();

            this.deviceSubTypeSpinner.setVisibility(View.VISIBLE);
            this.deviceSubTypeSpinnerReloadUpdate = true;

            refreshDeviceSubTypeSpinner(spinnerDeviceSubTypeValues);


        }
    }

    public void refreshDeviceSubTypeSpinner(String[] spinnerDeviceSubTypeValues) {
        int deviceSubTypePosition = 0;
        for (String eachDeviceSubType : spinnerDeviceSubTypeValues) {
            if (eachDeviceSubType.equals(this.deviceSubType)) {
                break;
            }
            deviceSubTypePosition++;
        }
        deviceSubTypeSpinner.setSelection(deviceSubTypePosition, true);
    }

    public void showHelp() {
        RelativeLayout snapshotHelpLayout = (RelativeLayout) findViewById(R.id.SnapshotHelpLayout);
        TextView helpCreate = (TextView) findViewById(R.id.snapshot_help_text);

        String dialogText = getString(R.string.snapshot_dialog_help_multiple_snapshots_in_row);
        helpCreate.setText(dialogText);
        snapshotHelpLayout.setVisibility(View.VISIBLE);
    }

    public void dissmissHelp(View view) {
        RelativeLayout mapHelpLayout = (RelativeLayout) findViewById(R.id.SnapshotHelpLayout);
        mapHelpLayout.setVisibility(View.GONE);
    }

    /* barcode scan actions */
    public void scanSerialNumber(View view) {
        checkCameraPermission(REQUEST_CODE_SERIALNUMBER_CAMERA_PERMISSIONS);
    }

    public void scanModel(View view) {
        checkCameraPermission(REQUEST_CODE_MODEL_CAMERA_PERMISSIONS);
    }

    public void scanManufacturer(View view) {
        checkCameraPermission(REQUEST_CODE_MANUFACTURER_CAMERA_PERMISSIONS);
    }

    public void scanLicenseKey(View view) {
        checkCameraPermission(REQUEST_CODE_LICENSEKEY_CAMERA_PERMISSIONS);
    }

    public void scanGiverId(View view) {
        checkCameraPermission(REQUEST_CODE_GIVER_CAMERA_PERMISSIONS);
    }

    public void scanRefurbisherId(View view) {
        checkCameraPermission(REQUEST_CODE_REFURBISHER_CAMERA_PERMISSIONS);
    }

    public void scanSystemId(View view) {
        checkCameraPermission(REQUEST_CODE_SYSTEM_CAMERA_PERMISSIONS);
    }

    @Override
    protected void launchScanAction(int permissionCode) {
        Intent intent = new Intent(this, BarcodeCaptureActivity.class);
        intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
        intent.putExtra(BarcodeCaptureActivity.UseFlash, false);

        startActivityForResult(intent, permissionCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (REQUEST_CODEBAR_PERMISSIONS.contains(requestCode)) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    Toast.makeText(this, getString(R.string.toast_barcode_scanned) + " " + barcode.displayValue, Toast.LENGTH_LONG).show();
                    this.fillScannedCode(barcode.displayValue, requestCode);
                } else {
                    Toast.makeText(this, getString(R.string.toast_barcode_cancel), Toast.LENGTH_LONG).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void fillScannedCode(String scannedCode, int requestCode) {

        switch (requestCode) {
            case REQUEST_CODE_SERIALNUMBER_CAMERA_PERMISSIONS:
                this.serialNumberEditText.setText(scannedCode);
                break;
            case REQUEST_CODE_MODEL_CAMERA_PERMISSIONS:
                this.modelEditText.setText(scannedCode);
                break;
            case REQUEST_CODE_LICENSEKEY_CAMERA_PERMISSIONS:
                this.licenseKeyEditText.setText(scannedCode);
                break;
            case REQUEST_CODE_GIVER_CAMERA_PERMISSIONS:
                this.giverEditText.setText(scannedCode);
                break;
            case REQUEST_CODE_REFURBISHER_CAMERA_PERMISSIONS:
                this.refurbishedEditText.setText(scannedCode);
                break;
            case REQUEST_CODE_SYSTEM_CAMERA_PERMISSIONS:
                this.systemEditText.setText(ScanUtils.getSystemIdFromUrl(scannedCode));
                break;
            default:
                break;
        }
    }

    @Override
    protected void dialogCallback() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        if (!sharedPreferences.getBoolean("snapshotHelpShown", false)) {
            sharedPreferences.edit().putBoolean("snapshotHelpShown", true).commit();
            showHelp();
        }
    }

    public void showManufacturerSelector(View view) {
        List<Manufacturer> manufacturers = this.getScannerApplication().getManufacturers();
        this.showSearchableSelector(getString(R.string.snapshot_manufacturer_label), manufacturers, manufacturerButton);
    }

    public void showAppearanceSelector(View view) {
        String title = getString(R.string.snapshot_grade_appearance_help);
        int gradeAttributeArray = R.array.snapshot_grade_appearance_array;
        this.showGradeSelector(GRADE_OPTION.APPEARANCE.toString(), title, gradeAttributeArray, gradeAppearanceButton, GRADE_OPTION.APPEARANCE);
    }

    public void showFunctionalitySelector(View view) {
        String title = getString(R.string.snapshot_grade_functionality_help);
        int gradeAttributeArray = R.array.snapshot_grade_functionality_array;
        this.showGradeSelector(GRADE_OPTION.FUNCTIONALITY.toString(), title, gradeAttributeArray, gradeFunctionalityButton, GRADE_OPTION.FUNCTIONALITY);
    }

    public void showBiosSelector(View view) {
        String title = getString(R.string.snapshot_grade_bios_help);
        int gradeAttributeArray = R.array.snapshot_grade_bios_array;
        this.showGradeSelector(GRADE_OPTION.BIOS.toString(), title, gradeAttributeArray, gradeBiosButton, GRADE_OPTION.BIOS);
    }

    private void showSearchableSelector(String title, List<Manufacturer> possibleOptions, final Button searchableButton) {
        if (this.searchableSelectorDialog != null && this.searchableSelectorDialog.isShowing()) {
            return;
        }

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View searchableDialogView = inflater.inflate(R.layout.searchable_list_dialog,
                null, false);

        /* get 90% of screen height */
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        searchableDialogView.setMinimumHeight((int) (size.y * 0.9f));

        TextView searchTitle = (TextView) searchableDialogView.findViewById(R.id.searchTitleTextView);
        searchTitle.setText(title);
        ListView listView = (ListView) searchableDialogView.findViewById(R.id.searchableListItems);
        listView.clearDisappearingChildren();

        final SearchableListAdapter listAdapter = new SearchableListAdapter(this, possibleOptions);

        SearchView searchView = (SearchView) searchableDialogView.findViewById(R.id.searchView);
        searchView.setActivated(true);
        searchView.onActionViewExpanded();
        searchView.setIconified(false);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                listAdapter.getFilter().filter(newText);
                return false;
            }
        });

        listView.setAdapter(listAdapter);

        dialogBuilder.setView(searchableDialogView);
        this.searchableSelectorDialog = dialogBuilder.create();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView adapter, View view, int position, long id) {
                TextView selectedManufacturer = (TextView) view.findViewById(R.id.searchableItemTextview);
                searchableSelectorCallback(selectedManufacturer.getText().toString(), searchableButton);
                SnapshotActivity.this.searchableSelectorDialog.dismiss();
            }

        });

        this.searchableSelectorDialog.show();
    }

    private void searchableSelectorCallback(String selectedLabel, Button gradeButton) {
        gradeButton.setText(selectedLabel);
    }

    private void showGradeSelector(String title, String helpText, final int gradeAttributeArray, final Button gradeButton, final GRADE_OPTION gradeOption) {
        if (this.gradeSelectorDialog != null && this.gradeSelectorDialog.isShowing()) {
            return;
        }
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View gradeDialogView = inflater.inflate(R.layout.single_choice_dialog,
                null, false);

        TextView gradeHelpTextView = (TextView) gradeDialogView.findViewById(R.id.gradeHelpTextView);
        gradeHelpTextView.setText(helpText);
        RadioGroup gradeRadioGroup = (RadioGroup) gradeDialogView.findViewById(R.id.gradeRadioGroup);
        gradeRadioGroup.removeAllViews();
        gradeRadioGroup.clearDisappearingChildren();

        TextView gradeTitleTextView = (TextView) gradeDialogView.findViewById(R.id.gradeTitleTextView);
        gradeTitleTextView.setText(title);

        String[] gradePossibleValues = getResources().getStringArray(gradeAttributeArray);
        for (int i = 0; i < gradePossibleValues.length; i++) {
            String eachGradePossibleValue = gradePossibleValues[i];
            RadioButton gradePossibleValueButton = new RadioButton(this);
            gradePossibleValueButton.setText(eachGradePossibleValue);
            gradePossibleValueButton.setTag(i);
            gradeRadioGroup.addView(gradePossibleValueButton, i);
        }

        dialogBuilder.setView(gradeDialogView);
        this.gradeSelectorDialog = dialogBuilder.create();

        gradeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int realCheckedId = getRealCheckedId(group, checkedId);
                gradeSelectorCallback(realCheckedId, gradeButton, gradeAttributeArray, gradeOption);
                SnapshotActivity.this.gradeSelectorDialog.dismiss();// dismiss the alertbox after choose an option
            }
        });

        this.gradeSelectorDialog.show();
    }

    private int getRealCheckedId(RadioGroup group, int checkedId) {
        RadioButton selectedRadioButton = (RadioButton) group.findViewById(checkedId);
        return (int) selectedRadioButton.getTag();
    }

    private void gradeSelectorCallback(int selectedItemId, Button gradeButton, int gradeAttributeArray, GRADE_OPTION gradeOption) {
        gradeButton.setText(getResources().getStringArray(gradeAttributeArray)[selectedItemId]);
        switch (gradeOption) {
            case APPEARANCE:
                GradeOption appearance = new GradeOption(getResources().getStringArray(R.array.snapshot_grade_appearance_array_values)[selectedItemId]);
                gradeConditions.setAppearance(appearance);
                break;
            case FUNCTIONALITY:
                GradeOption functionality = new GradeOption(getResources().getStringArray(R.array.snapshot_grade_functionality_array_values)[selectedItemId]);
                gradeConditions.setFunctionality(functionality);
                break;
            case BIOS:
                GradeOption bios = new GradeOption(getResources().getStringArray(R.array.snapshot_grade_bios_array_values)[selectedItemId]);
                gradeConditions.setBios(bios);
                break;
        }
    }

    private void retrieveManufacturers() {
        if (this.getScannerApplication().getManufacturers() == null) {
            this.getScannerApplication().setManufacturers(new ArrayList<Manufacturer>());
        }
        AsyncService asyncService = new AsyncService(this);
        asyncService.getManufacturers(this.getServer(), this.getUser());

    }

    private void retrieveManufacturers(String href) {
        AsyncService asyncService = new AsyncService(this);
        asyncService.getManufacturers(this.getServer(), this.getUser(), href);

    }

    static enum GRADE_OPTION {
        APPEARANCE,
        FUNCTIONALITY,
        BIOS
    }
}
