package nz.padmd;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.provider.OpenableColumns;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * padMD as an Android app: a full-screen WebView showing the same padMD.html page,
 * plus a small bridge so the page can open files, save back over them, share and print.
 */
public class MainActivity extends Activity {
    private static final int REQ_OPEN = 1;
    private static final int REQ_CREATE = 2;
    private static final int MAX_BYTES = 8 * 1024 * 1024;

    private WebView web;
    private boolean pageReady = false;
    private Intent pendingIntent = null;
    private String pendingText = null;
    private boolean pendingIsDoc = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#191b1d"));
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setTextZoom(100);

        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                if (pendingIntent != null) {
                    Intent i = pendingIntent;
                    pendingIntent = null;
                    handleIntent(i);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                String scheme = u.getScheme();
                if ("http".equals(scheme) || "https".equals(scheme) || "mailto".equals(scheme) || "tel".equals(scheme)) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
                    return true;
                }
                return false;
            }
        });
        web.addJavascriptInterface(new Bridge(), "Android");
        web.loadUrl("file:///android_asset/padMD.html");
        pendingIntent = getIntent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (pageReady) handleIntent(intent); else pendingIntent = intent;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (pageReady) web.evaluateJavascript("try{persist(true)}catch(e){}", null);
    }

    @Override
    public void onBackPressed() {
        if (!pageReady) { super.onBackPressed(); return; }
        web.evaluateJavascript("(function(){try{return padBack()}catch(e){return false}})()", value -> {
            if (!"true".equals(value)) MainActivity.super.onBackPressed();
        });
    }

    /* ── Files handed to padMD by other apps ── */
    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action) || Intent.ACTION_EDIT.equals(action)) {
            Uri uri = intent.getData();
            if (uri == null) return;
            boolean writable = Intent.ACTION_EDIT.equals(action)
                    || (intent.getFlags() & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0;
            try {
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                writable = true;
            } catch (Exception ignored) { }
            loadUri(uri, writable);
            intent.setAction(Intent.ACTION_MAIN);
        } else if (Intent.ACTION_SEND.equals(action)) {
            Uri stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (stream != null) {
                loadUri(stream, false);
            } else {
                String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                if (text != null) callJs("padLoad", text, subject != null ? subject + ".md" : "Shared.md", "", false);
            }
            intent.setAction(Intent.ACTION_MAIN);
        }
    }

    private void loadUri(Uri uri, boolean writable) {
        try {
            String text = readText(uri);
            callJs("padLoad", text, displayName(uri), uri.toString(), writable);
        } catch (Exception e) {
            toastJs("Could not open that file");
        }
    }

    private String readText(Uri uri) throws Exception {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new Exception("no stream");
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] b = new byte[16384];
            int n;
            while ((n = in.read(b)) > 0) {
                buf.write(b, 0, n);
                if (buf.size() > MAX_BYTES) throw new Exception("too big");
            }
            String t = buf.toString(StandardCharsets.UTF_8.name());
            if (t.startsWith("﻿")) t = t.substring(1);
            return t;
        }
    }

    private void writeText(Uri uri, String text) throws Exception {
        ContentResolver cr = getContentResolver();
        OutputStream os;
        try { os = cr.openOutputStream(uri, "wt"); } catch (Exception e) { os = cr.openOutputStream(uri, "w"); }
        if (os == null) throw new Exception("no stream");
        try (OutputStream o = os) { o.write(text.getBytes(StandardCharsets.UTF_8)); }
    }

    private String displayName(Uri uri) {
        String name = null;
        try (Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) name = c.getString(0);
        } catch (Exception ignored) { }
        if (name == null) name = uri.getLastPathSegment();
        return name == null ? "Untitled.md" : name;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) { pendingText = null; return; }
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (Exception ignored) { }
        if (requestCode == REQ_OPEN) {
            loadUri(uri, true);
        } else if (requestCode == REQ_CREATE && pendingText != null) {
            try {
                writeText(uri, pendingText);
                if (pendingIsDoc) callJs("padSaved", uri.toString(), displayName(uri));
                else toastJs("Saved " + displayName(uri));
            } catch (Exception e) {
                toastJs("Could not save there. Try another folder.");
            }
            pendingText = null;
        }
    }

    /* ── Calls into the page ── */
    private void callJs(String fn, Object... args) {
        StringBuilder sb = new StringBuilder(fn).append('(');
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(',');
            Object a = args[i];
            if (a instanceof Boolean) sb.append(a.toString());
            else sb.append(JSONObject.quote(String.valueOf(a)));
        }
        sb.append(')');
        String js = sb.toString();
        runOnUiThread(() -> web.evaluateJavascript(js, null));
    }

    private void toastJs(String msg) { callJs("padToast", msg); }

    /* ── What the page can ask the app to do ── */
    private class Bridge {
        @JavascriptInterface
        public void openFile() {
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/*", "application/octet-stream"});
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                try { startActivityForResult(i, REQ_OPEN); } catch (Exception e) { toastJs("No file picker found"); }
            });
        }

        @JavascriptInterface
        public void saveTo(String uri, String text, String name) {
            runOnUiThread(() -> {
                try {
                    Uri u = Uri.parse(uri);
                    writeText(u, text);
                    callJs("padSaved", uri, displayName(u));
                } catch (Exception e) {
                    // Permission lost or file moved: fall back to choosing a place to save
                    pendingText = text;
                    pendingIsDoc = true;
                    launchCreate(name, "text/markdown");
                }
            });
        }

        @JavascriptInterface
        public void saveAs(String name, String text, String mime, boolean isDoc) {
            runOnUiThread(() -> {
                pendingText = text;
                pendingIsDoc = isDoc;
                launchCreate(name, mime);
            });
        }

        @JavascriptInterface
        public void share(String name, String text) {
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, text);
                i.putExtra(Intent.EXTRA_SUBJECT, name);
                try { startActivity(Intent.createChooser(i, "Share " + name)); } catch (Exception e) { toastJs("Nothing to share with"); }
            });
        }

        @JavascriptInterface
        public void print(String title) {
            runOnUiThread(() -> {
                PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
                PrintAttributes attrs = new PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4).build();
                pm.print(title, web.createPrintDocumentAdapter(title), attrs);
            });
        }
    }

    private void launchCreate(String name, String mime) {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(mime);
        i.putExtra(Intent.EXTRA_TITLE, name);
        try { startActivityForResult(i, REQ_CREATE); } catch (Exception e) { toastJs("No save screen found"); }
    }
}
