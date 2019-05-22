package org.apache.cordova.inappbrowser;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.view.WindowManager.LayoutParams;
import android.widget.*;
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient;
import com.tencent.smtt.export.external.interfaces.JsResult;
import com.tencent.smtt.sdk.*;
import com.amzport.haitang.MainActivity;
import org.apache.cordova.LOG;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;

public class InAppBrowserActivity extends Activity {

    public static InAppBrowserActivity currentNode = null;

    private static final String LOG_TAG = "InAppBrowser";
    private static final String TOOLBAR_COLOR = "toolbarcolor";
    private static final String TOOLBAR_BTN_COLOR = "toolbarbtncolor";
    private static final String TOOLBAR_TXT_COLOR = "toolbartxtcolor";
    private static final int FILECHOOSER_RESULTCODE = 5288;
    private static final int FILECHOOSER_RESULTCODE_FOR_ANDROID_5 = 5259;

    private URL mIntentUrl;
    public X5WebView inAppWebView;
    private ProgressBar progressBar;
    private ValueCallback<Uri> uploadFile;
    private ValueCallback<Uri[]> uploadFiles;

    private String toolbarColor = "#10234E";
    private String toolbarBtnColor = "#D5DAE6";
    private String toolbarTxtColor = "#9BA7C2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        setWindowFlags();

        Intent intent = getIntent();
        if (intent != null) {
            String title = setUrlByIntent(intent);
            if (mIntentUrl == null) {
                goHome();
            } else {
                init(mIntentUrl.toString(), title);
            }
        } else {
            goHome();
        }
    }

    @Override
    public void onPause() {
        if (inAppWebView != null)
            inAppWebView.onPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        if (inAppWebView != null)
            inAppWebView.onResume();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        if (inAppWebView != null)
            inAppWebView.destroy();
        InAppBrowserActivity.currentNode = null;
        super.onDestroy();
    }

    private void goHome() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
        // 清理当前Activity，回调onDestroy
        this.finish();
    }

    private void goBack() {
        if (inAppWebView != null && inAppWebView.canGoBack()) {
            inAppWebView.goBack();
        } else {
            goHome();
        }
    }

    private void init(final String url, final String title) {
        Runnable runnable = new Runnable() {
            @SuppressLint("NewApi")
            public void run() {
                LinearLayout main = createMainContainer();
                RelativeLayout toolbar = createToolbar(cutTitleText(title),toolbarColor, toolbarBtnColor, toolbarTxtColor);
                RelativeLayout webViewLayout = createWebViewLayout();

                progressBar = new ProgressBar(InAppBrowserActivity.this, null, android.R.attr.progressBarStyleHorizontal);
                progressBar.setIndeterminate(true);
                progressBar.setPadding(0, dpToPixels(-6), 0, 0);

                main.addView(toolbar);
                main.addView(progressBar);
                main.addView(webViewLayout, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

                inAppWebView.loadUrl(url);
                inAppWebView.requestFocus();
                inAppWebView.requestFocusFromTouch();

                setContentView(main);
            }
        };
        runOnUiThread(runnable);
    }

    private String setUrlByIntent(Intent intent) {
        try {
            String title = intent.getStringExtra("title");
            String _toolbarColor = intent.getStringExtra(TOOLBAR_COLOR);
            String _toolbarBtnColor = intent.getStringExtra(TOOLBAR_BTN_COLOR);
            String _toolbarTxtColor = intent.getStringExtra(TOOLBAR_TXT_COLOR);
            if (_toolbarColor != null) toolbarColor = _toolbarColor;
            if (_toolbarBtnColor != null) toolbarBtnColor = _toolbarBtnColor;
            if (_toolbarTxtColor != null) toolbarTxtColor = _toolbarTxtColor;
            mIntentUrl = new URL(intent.getStringExtra("url"));
            if (title != null && !title.isEmpty()) {
                return title;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }
        return "";
    }

    private void setWindowFlags() {
        try {
            if (Integer.parseInt(android.os.Build.VERSION.SDK) >= 11) {
                getWindow()
                        .setFlags(
                                android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                                android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
            }
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            requestWindowFeature(Window.FEATURE_PROGRESS);
        } catch (Exception e) {
        }
    }

    private String cutTitleText(String title) {
        if (title == null)
            return "";
        else if (title.length() > 14)
            return title.subSequence(0, 14) + "...";
        else
            return title;
    }

    /**
     * Convert our DIP units to Pixels
     */
    private int dpToPixels(int dipValue) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float) dipValue,
                getResources().getDisplayMetrics()
        );
    }

    private LinearLayout createMainContainer() {
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        return main;
    }

    private RelativeLayout createWebViewLayout() {
        initWebView();
        RelativeLayout webViewLayout = new RelativeLayout(this);
        FrameLayout mViewParent = new FrameLayout(this);
        mViewParent.setId(7);
        mViewParent.addView(inAppWebView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        webViewLayout.addView(mViewParent, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        return webViewLayout;
    }

    private void initWebView() {
        final InAppBrowserActivity that = this;
        inAppWebView = new X5WebView(this, null);
        inAppWebView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        inAppWebView.setId(6);

        inAppWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Log.d("InAppBrowser", url);
                if (url.startsWith("androidamap://")) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    Uri uri = Uri.parse(url);
                    intent.setData(uri);
                    startActivity(intent);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }

                try {
                    if (InAppBrowser.currentNode != null) {
                        InAppBrowserActivity.currentNode = that;

                        LOG.d(LOG_TAG, "loadstop");
                        JSONObject obj = new JSONObject();
                        obj.put("type", "loadstop");
                        obj.put("url", url);
                        InAppBrowser.currentNode.sendUpdate(obj, true);
                    }
                } catch (Exception ex) {
                    LOG.d(LOG_TAG, ex.getMessage());
                }
            }

        });

        inAppWebView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onJsConfirm(WebView arg0, String arg1, String arg2,
                                       JsResult arg3) {
                return super.onJsConfirm(arg0, arg1, arg2, arg3);
            }

            View myVideoView;
            View myNormalView;
            IX5WebChromeClient.CustomViewCallback callback;

            @Override
            public void onShowCustomView(View view,
                                         IX5WebChromeClient.CustomViewCallback customViewCallback) {
                FrameLayout normalView = findViewById(7);
                ViewGroup viewGroup = (ViewGroup) normalView.getParent();
                viewGroup.removeView(normalView);
                viewGroup.addView(view);
                myVideoView = view;
                myNormalView = normalView;
                callback = customViewCallback;
            }

            @Override
            public void onHideCustomView() {
                if (callback != null) {
                    callback.onCustomViewHidden();
                    callback = null;
                }
                if (myVideoView != null) {
                    ViewGroup viewGroup = (ViewGroup) myVideoView.getParent();
                    viewGroup.removeView(myVideoView);
                    viewGroup.addView(myNormalView);
                }
            }

            @Override
            public boolean onJsAlert(WebView arg0, String arg1, String arg2,
                                     JsResult arg3) {
                return super.onJsAlert(null, arg1, arg2, arg3);
            }

            // <input type=file> support:
            // openFileChooser() is for pre KitKat and in KitKat mr1 (it's known broken in KitKat).
            // For Lollipop, we use onShowFileChooser().
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                this.openFileChooser(uploadMsg, "*/*");
            }

            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
                this.openFileChooser(uploadMsg, acceptType, null);
            }

            public void openFileChooser(final ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                uploadFile = uploadMsg;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, FILECHOOSER_RESULTCODE);
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean onShowFileChooser(WebView webView, final ValueCallback<Uri[]> filePathsCallback, final FileChooserParams fileChooserParams) {
                uploadFiles = filePathsCallback;
                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILECHOOSER_RESULTCODE_FOR_ANDROID_5);
                } catch (ActivityNotFoundException e) {
                    filePathsCallback.onReceiveValue(null);
                }
                return true;
            }

        });

        inAppWebView.setDownloadListener(new DownloadListener() {

            @Override
            public void onDownloadStart(final String arg0, String arg1, String arg2,
                                        String arg3, long arg4) {
                new AlertDialog.Builder(InAppBrowserActivity.this)
                        .setTitle("文件下载")
                        .setMessage("确认打开系统浏览器下载该文件？")
                        .setPositiveButton("确认",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        Toast.makeText(
                                                InAppBrowserActivity.this,
                                                "前往下载...",
                                                1000).show();

                                        downloadFile(Uri.parse(arg0));
                                    }
                                })
                        .setNegativeButton("取消",
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        Toast.makeText(
                                                InAppBrowserActivity.this,
                                                "取消下载...",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                })
                        .setOnCancelListener(
                                new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        Toast.makeText(
                                                InAppBrowserActivity.this,
                                                "取消下载...",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }).show();
            }
        });

        WebSettings webSetting = inAppWebView.getSettings();
        webSetting.setAllowFileAccess(true);
        webSetting.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        webSetting.setSupportZoom(true);
        webSetting.setBuiltInZoomControls(true);
        webSetting.setUseWideViewPort(true);
        webSetting.setSupportMultipleWindows(false);
        // webSetting.setLoadWithOverviewMode(true);
        webSetting.setAppCacheEnabled(true);
        // webSetting.setDatabaseEnabled(true);
        webSetting.setDomStorageEnabled(true);
        webSetting.setJavaScriptEnabled(true);
        webSetting.setGeolocationEnabled(true);
        webSetting.setAppCacheMaxSize(Long.MAX_VALUE);
        webSetting.setAppCachePath(this.getDir("appcache", 0).getPath());
        webSetting.setDatabasePath(this.getDir("databases", 0).getPath());
        webSetting.setGeolocationDatabasePath(this.getDir("geolocation", 0).getPath());
        // webSetting.setPageCacheCapacity(IX5WebSettings.DEFAULT_CACHE_CAPACITY);
        webSetting.setPluginState(WebSettings.PluginState.ON_DEMAND);
        // webSetting.setRenderPriority(WebSettings.RenderPriority.HIGH);
        // webSetting.setPreFectch(true);
        webSetting.setAllowFileAccess(true);

        webSetting.setAllowContentAccess(true);
        webSetting.setAllowFileAccessFromFileURLs(true);
        webSetting.setAllowUniversalAccessFromFileURLs(true);

        CookieSyncManager.createInstance(this);
        CookieSyncManager.getInstance().sync();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (inAppWebView != null && inAppWebView.canGoBack()) {
                inAppWebView.goBack();
                return true;
            } else {
                return super.onKeyDown(keyCode, event);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Log.d("onActivityResult", "picker resultCode: " + requestCode);
            switch (requestCode) {
                case FILECHOOSER_RESULTCODE:
                    if (null != uploadFile) {
                        Log.d("onActivityResult", "picker uploadFile: " + uploadFile);
                        Uri result = data == null ? null : data.getData();
                        uploadFile.onReceiveValue(result);
                        uploadFile = null;
                    }
                    break;
                case FILECHOOSER_RESULTCODE_FOR_ANDROID_5:
                    if (null != uploadFiles) {
                        Log.d("onActivityResult", "picker uploadFiles: " + uploadFiles);
                        Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
                        uploadFiles.onReceiveValue(result);
                        uploadFiles = null;
                    }
                    break;
                default:
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {
            switch (requestCode) {
                case FILECHOOSER_RESULTCODE:
                    if (null != uploadFile) {
                        uploadFile.onReceiveValue(null);
                        uploadFile = null;
                    }
                    break;
                case FILECHOOSER_RESULTCODE_FOR_ANDROID_5:
                    if (null != uploadFiles) {
                        uploadFiles.onReceiveValue(null);
                        uploadFiles = null;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private RelativeLayout createToolbar(String title, String toolbarColor, String navigationButtonColor, String navigationTextColor) {
        Resources activityRes = getResources();

        // Toolbar
        RelativeLayout toolbar = new RelativeLayout(this);
        if (toolbarColor != null) toolbar.setBackgroundColor(android.graphics.Color.parseColor(toolbarColor));
        toolbar.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, this.dpToPixels(44)));
        toolbar.setPadding(this.dpToPixels(2), this.dpToPixels(2), this.dpToPixels(2), this.dpToPixels(2));
        toolbar.setHorizontalGravity(Gravity.LEFT);
        toolbar.setVerticalGravity(Gravity.TOP);

        // Action Button Container layout
        RelativeLayout actionButtonContainer = new RelativeLayout(this);
        actionButtonContainer.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        actionButtonContainer.setHorizontalGravity(Gravity.LEFT);
        actionButtonContainer.setVerticalGravity(Gravity.CENTER_VERTICAL);
        actionButtonContainer.setId(1);

        // Back button
        ImageButton back = new ImageButton(this);
        RelativeLayout.LayoutParams backLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        backLayoutParams.addRule(RelativeLayout.ALIGN_LEFT);
        back.setLayoutParams(backLayoutParams);
        back.setContentDescription("返回");
        back.setId(2);
        int backResId = activityRes.getIdentifier("ic_action_previous_item", "drawable", getPackageName());
        Drawable backIcon = activityRes.getDrawable(backResId);
        if (navigationButtonColor != null) back.setColorFilter(android.graphics.Color.parseColor(navigationButtonColor));
        if (Build.VERSION.SDK_INT >= 16)
            back.setBackground(null);
        else
            back.setBackgroundDrawable(null);
        back.setImageDrawable(backIcon);
        back.setScaleType(ImageView.ScaleType.FIT_CENTER);
        back.setPadding(0, this.dpToPixels(5), this.dpToPixels(-5), this.dpToPixels(5));
        if (Build.VERSION.SDK_INT >= 16)
            back.getAdjustViewBounds();

        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                goBack();
            }
        });

        // Back Text
        TextView backText = new TextView(this);
        RelativeLayout.LayoutParams backTextLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        backTextLayoutParams.addRule(RelativeLayout.RIGHT_OF, 2);
        backText.setLayoutParams(backTextLayoutParams);
        backText.setId(3);
        backText.setText("返回");
        // backText.setTextSize(20);
        if (navigationButtonColor != null) backText.setTextColor(android.graphics.Color.parseColor(navigationButtonColor));
        backText.setGravity(android.view.Gravity.CENTER_VERTICAL);
        if (Build.VERSION.SDK_INT >= 16)
            backText.setBackground(null);
        else
            backText.setBackgroundDrawable(null);

        backText.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                goBack();
            }
        });

        // Title
        TextView titleText = new TextView(this);
        RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        textLayoutParams.addRule(RelativeLayout.RIGHT_OF, 1);
        textLayoutParams.addRule(RelativeLayout.LEFT_OF, 5);
        titleText.setLayoutParams(textLayoutParams);
        titleText.setId(4);
        titleText.setSingleLine(true);
        titleText.setText(title);
        // titleText.setTextSize(20);
        if (navigationTextColor != null) titleText.setTextColor(android.graphics.Color.parseColor(navigationTextColor));
        titleText.setGravity(android.view.Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        if (Build.VERSION.SDK_INT >= 16)
            titleText.setBackground(null);
        else
            titleText.setBackgroundDrawable(null);
        // titleText.setPadding(this.dpToPixels(15), 0, this.dpToPixels(10), 0);

        // Header Close button
        View close = createCloseButton("关闭", navigationButtonColor);
        close.setId(5);

        actionButtonContainer.addView(back);
        actionButtonContainer.addView(backText);
        toolbar.addView(actionButtonContainer);
        toolbar.addView(titleText);
        toolbar.addView(close);

        return toolbar;
    }

    private View createCloseButton(String closeButtonCaption, String closeButtonColor) {
        View _close;
        Resources activityRes = getResources();

        if (closeButtonCaption != null) {
            TextView close = new TextView(this);
            close.setText(closeButtonCaption);
            // close.setTextSize(20);
            if (closeButtonColor != null) close.setTextColor(android.graphics.Color.parseColor(closeButtonColor));
            close.setGravity(android.view.Gravity.CENTER_VERTICAL);
            close.setPadding(this.dpToPixels(10), 0, this.dpToPixels(10), 0);
            _close = close;
        } else {
            ImageButton close = new ImageButton(this);
            int closeResId = activityRes.getIdentifier("ic_action_remove", "drawable", getPackageName());
            Drawable closeIcon = activityRes.getDrawable(closeResId);
            if (closeButtonColor != null) close.setColorFilter(android.graphics.Color.parseColor(closeButtonColor));
            close.setImageDrawable(closeIcon);
            close.setScaleType(ImageView.ScaleType.FIT_CENTER);
            if (Build.VERSION.SDK_INT >= 16)
                close.getAdjustViewBounds();
            _close = close;
        }

        RelativeLayout.LayoutParams closeLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        _close.setLayoutParams(closeLayoutParams);

        if (Build.VERSION.SDK_INT >= 16)
            _close.setBackground(null);
        else
            _close.setBackgroundDrawable(null);

        _close.setContentDescription("Close Button");
        _close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                goHome();
            }
        });

        return _close;
    }

    private void downloadFile(Uri uri) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri, "text/html");
        startActivity(i);
    }
}
