package org.ereuse.scanner.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.zxing.common.StringUtils;

import org.ereuse.scanner.R;
import org.ereuse.scanner.data.User;
import org.ereuse.scanner.services.api.ApiServicesImpl;
import org.ereuse.scanner.utils.ScanUtils;

import java.util.ArrayList;

/**
 * Created by Jamgo SCCL.
 */
public class WorkbenchActivity extends ScanActivity {

    private SubMenu selectDb;
    ArrayList<String> databases;
    WebView scanWebView;
    private EditText workbenchServerAddressEditText;
    private String htmlFieldId;
    private boolean urlField;

    public void setHtmlFieldId(String htmlFieldId) { this.htmlFieldId = htmlFieldId; }
    public String getHtmlFieldId() { return this.htmlFieldId; }
    public boolean isUrlField() { return urlField; }
    public void setUrlField(boolean urlField) { this.urlField = urlField; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snapshot_workbench);
        setToolbar();

        this.workbenchServerAddressEditText = new EditText(this);
        this.workbenchServerAddressEditText.setText(getWorkbenchServer());

        this.scanWebView = (WebView)this.findViewById(R.id.workbench_webview);

        this.scanWebView.getSettings().setJavaScriptEnabled(true);
        this.scanWebView.addJavascriptInterface(new WebViewJavaScriptInterface(this), "app");
        this.scanWebView.getSettings().setJavaScriptEnabled(true);

        reloadWebView();
    }

    private void reloadWebView() {
        if(this.workbenchServerAddressEditText.getText().toString().equals("")) {
            String dynamicHtml = "<html>" +
                    "<div>" +
                    "<p>Clica per escanejar:</p>" +
                    "</div>" +
                    "<form>" +
                    "<p>common field:</p>" +
                    "<input type='button' value='scan' onclick='app.startJSScan(\"scanResultField\", false)' />" +
                    "<input type='text' id='scanResultField' />" +
                    "<p>System id:</p>" +
                    "<input type='button' value='scan' onclick='app.startJSScan(\"systemIdField\", true)' />" +
                    "<input type='text' id='systemIdField' />" +
                    "</form>" +
                    "</html>";
            this.scanWebView.loadData(dynamicHtml,"text/html","UTF-8");

        } else {
            final Activity activity = this;

            this.scanWebView.setWebViewClient(new WebViewClient() {
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    Toast.makeText(activity, description, Toast.LENGTH_SHORT).show();
                }
            });
            this.scanWebView.loadUrl(this.workbenchServerAddressEditText.getText().toString());

        }
    }


    @Override
    protected void onResume()
    {
        super.onResume();
    }

    private String getWorkbenchServer() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME,MODE_PRIVATE);
        return sharedPreferences.getString("workbenchServerAddress","");
    }

    private void setWorkbenchServer(String server) {
        this.workbenchServerAddressEditText.setText(server);
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME,MODE_PRIVATE);
        sharedPreferences.edit().putString("workbenchServerAddress", server).commit();
        this.reloadWebView();
    }

    public void showWorkbenchSettings(View view) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle(getString(R.string.workbench_server_address))
                .setView(workbenchServerAddressEditText)
                .setPositiveButton(R.string.dialog_ack, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setWorkbenchServer(workbenchServerAddressEditText.getText().toString());
                        ((ViewGroup)workbenchServerAddressEditText.getParent()).removeAllViews();
                        dialog.dismiss();
                    }
                })
//                .setNeutralButton(R.string.dialog_reset_default, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        setWorkbenchServer(getString(R.string.default_workbench_server_address));
//                    }
//                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((ViewGroup)workbenchServerAddressEditText.getParent()).removeAllViews();
                        dialog.dismiss();
                    }
                }).show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    public void showLogoutDialog(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setIcon(R.drawable.logout);
        dialog.setTitle(getString(R.string.back_to_login));
        dialog.setMessage(getString(R.string.back_to_login_message));
        dialog.setPositiveButton(getString(R.string.dialog_ack), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                doBackToLogin();
            }
        });
        dialog.setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void doBackToLogin(){
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    class SetDatabase implements MenuItem.OnMenuItemClickListener{

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            ApiServicesImpl.setDb(item.getTitleCondensed().toString());
            return true;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (REQUEST_CODEBAR_PERMISSIONS.contains(requestCode)) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
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

        //Toast.makeText(this, getString(R.string.toast_barcode_scanned) + " " + scannedCode, Toast.LENGTH_LONG).show();
        //scanWebView.loadUrl("javascript:document.getElementById('scanresult').innerHTML = '"+scannedCode+"'");
        String htmlFieldId = this.getHtmlFieldId();
        String scanResult = scannedCode;
        if(this.isUrlField()) {
            scanResult = ScanUtils.getSystemIdFromUrl(scannedCode);
        }

        this.setHtmlFieldId(null);
        this.setUrlField(false);

        scanWebView.loadUrl("javascript:(function(){document.getElementById('" + htmlFieldId + "').value = '"+scanResult+"';})()");

    }

    @Override
    protected void launchScanAction(int permissionCode) {
        Intent intent = new Intent(this, BarcodeCaptureActivity.class);
        intent.putExtra(BarcodeCaptureActivity.AutoFocus,true);
        intent.putExtra(BarcodeCaptureActivity.UseFlash,false);

        startActivityForResult(intent, permissionCode);
    }

    protected void checkCameraPermission(int permissionCode, String htmlFieldId, boolean isUrlField) {
        this.setHtmlFieldId(htmlFieldId);
        this.setUrlField(isUrlField);
        this.checkCameraPermission(permissionCode);
    }

    public class WebViewJavaScriptInterface{

        private Context context;

        /*
         * Need a reference to the context in order to sent a post message
         */
        public WebViewJavaScriptInterface(Context context){
            this.context = context;
        }

        /*
         * This method can be called from Android. @JavascriptInterface
         * required after SDK version 17.
         */
        @JavascriptInterface
        public void startJSScan(String htmlFieldId, boolean isUrlField){
            checkCameraPermission(REQUEST_CODE_JS_CAMERA_PERMISSIONS, htmlFieldId, isUrlField);
            //return "OK";
        }
    }
}