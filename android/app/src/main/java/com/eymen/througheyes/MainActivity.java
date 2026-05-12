package com.eymen.througheyes;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.getcapacitor.BridgeActivity;
import java.io.OutputStream;

public class MainActivity extends BridgeActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private PermissionRequest pendingPermissionRequest;

    @Override
    public void onStart() {
        super.onStart();
        WebView webView = getBridge().getWebView();
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                pendingPermissionRequest = request;
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    request.grant(request.getResources());
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
                }
            }
        });

        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public String saveImageToGallery(String base64Data, String fileName) {
                try {
                    byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ContentValues cv = new ContentValues();
                        cv.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                        cv.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                        cv.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ThroughEyes");
                        cv.put(MediaStore.Images.Media.IS_PENDING, 1);
                        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
                        if (uri != null) {
                            OutputStream os = getContentResolver().openOutputStream(uri);
                            bmp.compress(Bitmap.CompressFormat.JPEG, 92, os);
                            os.close();
                            cv.clear();
                            cv.put(MediaStore.Images.Media.IS_PENDING, 0);
                            getContentResolver().update(uri, cv, null, null);
                        }
                    } else {
                        String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/ThroughEyes";
                        java.io.File f = new java.io.File(dir);
                        if (!f.exists()) f.mkdirs();
                        java.io.File file = new java.io.File(f, fileName);
                        OutputStream os = new java.io.FileOutputStream(file);
                        bmp.compress(Bitmap.CompressFormat.JPEG, 92, os);
                        os.close();
                        sendBroadcast(new android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, android.net.Uri.fromFile(file)));
                    }
                    return "success";
                } catch (Exception e) { return "error:" + e.getMessage(); }
            }
        }, "AndroidBridge");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST && pendingPermissionRequest != null) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingPermissionRequest.grant(pendingPermissionRequest.getResources());
            } else {
                pendingPermissionRequest.deny();
            }
            pendingPermissionRequest = null;
        }
    }
}
