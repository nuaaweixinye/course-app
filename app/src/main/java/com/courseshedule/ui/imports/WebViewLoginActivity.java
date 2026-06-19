package com.courseshedule.ui.imports;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.courseshedule.R;
import com.courseshedule.data.imports.ImportException;
import com.courseshedule.data.imports.NuaaEamsParser;
import com.courseshedule.data.imports.ParsedCourse;
import com.courseshedule.databinding.ActivityWebviewLoginBinding;

import java.util.List;

/**
 * Loads the NUAA 教务 timetable URL in a WebView. The user logs in to CAS
 * themselves (handling any captcha); the app never sees credentials. Once the
 * timetable page is reached, {@link #capturePage()} reads the rendered DOM,
 * runs {@link NuaaEamsParser}, and on success returns the page HTML to
 * {@link ImportActivity} which shows the standard preview/confirm flow.
 */
public class WebViewLoginActivity extends AppCompatActivity {

    /** Result extra: the captured timetable page HTML for ImportActivity to parse. */
    public static final String EXTRA_HTML = "captured_html";

    /** Entry URL — opening courseTableForStd.action triggers CAS redirect when not logged in. */
    private static final String TIMETABLE_URL =
            "https://aao-eas.nuaa.edu.cn/eams/courseTableForStd.action";
    private static final String DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private ActivityWebviewLoginBinding binding;
    private boolean navigatedToTimetable = false;
    private boolean captured = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWebviewLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_capture) {
                fetchViaHttp();
                return true;
            }
            return false;
        });

        setupWebView();
        binding.webView.loadUrl(TIMETABLE_URL);

        // Back gesture/button: navigate the WebView history first, else finish.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void setupWebView() {
        WebSettings s = binding.webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        // Render the desktop 教务 page properly: a clean desktop Chrome UA so the
        // server sends the full page (its default UA marks it Mobile/WebView),
        // wide viewport + overview fit, and allow mixed HTTP/HTTPS subresources.
        s.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(binding.webView, true);

        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                return false; // load everything in the WebView
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                binding.progress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                binding.progress.setVisibility(View.GONE);
                // Once the user is logged in (any /eams/ page), skip the flaky desktop
                // portal rendering and fetch the timetable directly via HTTP with the
                // session cookies the WebView just established.
                if (url != null && url.contains("/eams/") && !captured) {
                    fetchViaHttp();
                }
            }
        });
        binding.webView.setWebChromeClient(new WebChromeClient());
    }

    /** Grab the WebView's session cookies and HTTP-GET the timetable page directly. */
    private void fetchViaHttp() {
        binding.progress.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                String html = httpGet(TIMETABLE_URL + "?setting.kind=std");
                android.util.Log.d("EAMS_HTTP", "first-GET len=" + html.length()
                        + " hasTaskActivity=" + html.contains("new TaskActivity(")
                        + " hasIds=" + html.contains("ids=")
                        + " isCasLogin=" + html.contains("统一身份认证"));

                if (!html.contains("new TaskActivity(")) {
                    // Dump every form field so we know what to POST.
                    java.util.regex.Matcher inM = java.util.regex.Pattern.compile(
                            "<input[^>]*>").matcher(html);
                    StringBuilder fields = new StringBuilder();
                    while (inM.find()) {
                        String tag = inM.group();
                        String nm = firstMatch(tag, "name=\"([^\"]*)\"");
                        String vl = firstMatch(tag, "value=\"([^\"]*)\"");
                        if (nm != null) fields.append(nm).append("=").append(vl).append(" | ");
                    }
                    android.util.Log.d("EAMS_HTTP", "form fields: " + fields);
                    String formAction = firstMatch(html, "action=\"([^\"]*courseTable[^\"]*)\"");
                    android.util.Log.d("EAMS_HTTP", "form action=" + formAction);

                    String ids = firstMatch(html, "ids=(\\d+)");
                    if (ids == null) {
                        // POST the selector form — that's how the timetable gets rendered.
                        android.util.Log.d("EAMS_HTTP", "POSTing selector form...");
                        html = httpPost(formAction != null ? formAction
                                : (TIMETABLE_URL + "!courseTable.action"), extractFormParams(html));
                        android.util.Log.d("EAMS_HTTP", "POST result len=" + html.length()
                                + " hasTaskActivity=" + html.contains("new TaskActivity(")
                                + " hasIds=" + html.contains("ids="));
                        ids = firstMatch(html, "ids=(\\d+)");
                    }
                    if (ids != null) {
                        android.util.Log.d("EAMS_HTTP", "found ids=" + ids + ", fetching timetable data");
                        html = httpGet(TIMETABLE_URL + "!courseTable.action?setting.kind=std&ids=" + ids);
                        android.util.Log.d("EAMS_HTTP", "second-GET len=" + html.length()
                                + " hasTaskActivity=" + html.contains("new TaskActivity("));
                    }
                }
                final String page = html;
                runOnUiThread(() -> {
                    binding.progress.setVisibility(View.GONE);
                    if (page.contains("new TaskActivity(")) {
                        parseAndReturn(page);
                    } else {
                        toast("未获取到课表数据（" + (page.contains("统一身份认证") ? "未登录" : "无课表") + "），请确认已登录后再「抓取」");
                    }
                });
            } catch (Exception e) {
                android.util.Log.d("EAMS_HTTP", "exception: " + e);
                runOnUiThread(() -> {
                    binding.progress.setVisibility(View.GONE);
                    toast("获取课表失败: " + e.getMessage());
                });
            }
        }).start();
    }

    /** First regex group-1 match in a string, or null. */
    private static String firstMatch(String s, String pattern) {
        if (s == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(s);
        return m.find() ? m.group(1) : null;
    }

    /** Extract name=value pairs from all &lt;input&gt; tags (for POSTing the selector form). */
    private static String extractFormParams(String html) {
        StringBuilder sb = new StringBuilder();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("<input[^>]*>").matcher(html);
        boolean first = true;
        while (m.find()) {
            String tag = m.group();
            String nm = firstMatch(tag, "name=\"([^\"]*)\"");
            if (nm == null) continue;
            String vl = firstMatch(tag, "value=\"([^\"]*)\"");
            if (vl == null) vl = "";
            try {
                if (!first) sb.append('&');
                sb.append(java.net.URLEncoder.encode(nm, "UTF-8"))
                        .append('=')
                        .append(java.net.URLEncoder.encode(vl, "UTF-8"));
                first = false;
            } catch (java.io.UnsupportedEncodingException ignored) {}
        }
        return sb.toString();
    }

    /** HTTP POST (application/x-www-form-urlencoded) with per-URL cookies + redirect follow. */
    private String httpPost(String url, String body) throws java.io.IOException {
        int redirects = 0;
        while (redirects < 10) {
            String cookies = CookieManager.getInstance().getCookie(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Cookie", cookies == null ? "" : cookies);
            conn.setRequestProperty("User-Agent", DESKTOP_UA);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.getOutputStream().write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            int code = conn.getResponseCode();
            if (code == java.net.HttpURLConnection.HTTP_MOVED_PERM
                    || code == java.net.HttpURLConnection.HTTP_MOVED_TEMP
                    || code == java.net.HttpURLConnection.HTTP_SEE_OTHER) {
                String loc = conn.getHeaderField("Location");
                conn.disconnect();
                if (loc == null) return "";
                url = loc.startsWith("http") ? loc
                        : new java.net.URL(new java.net.URL(url), loc).toString();
                android.util.Log.d("EAMS_HTTP", "POST redirect -> " + url);
                redirects++;
                continue;
            }
            try (java.io.InputStream stream = conn.getInputStream()) {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append('\n');
                return sb.toString();
            } finally {
                conn.disconnect();
            }
        }
        return "";
    }

    /** HTTP GET returning the full response body as a UTF-8 string. Cookies are pulled
     *  per-URL from the WebView's CookieManager (so CAS SSO redirects get the right
     *  domain's cookies — the TGT lives on the CAS domain, the session on eams). */
    private String httpGet(String url) throws java.io.IOException {
        int redirects = 0;
        while (redirects < 10) {
            String cookies = CookieManager.getInstance().getCookie(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Cookie", cookies == null ? "" : cookies);
            conn.setRequestProperty("User-Agent", DESKTOP_UA);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            int code = conn.getResponseCode();
            if (code == java.net.HttpURLConnection.HTTP_MOVED_PERM
                    || code == java.net.HttpURLConnection.HTTP_MOVED_TEMP
                    || code == java.net.HttpURLConnection.HTTP_SEE_OTHER) {
                String loc = conn.getHeaderField("Location");
                conn.disconnect();
                if (loc == null) return "";
                url = loc.startsWith("http") ? loc
                        : new java.net.URL(new java.net.URL(url), loc).toString();
                android.util.Log.d("EAMS_HTTP", "redirect -> " + url);
                redirects++;
                continue;
            }
            try (java.io.InputStream stream = conn.getInputStream()) {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append('\n');
                return sb.toString();
            } finally {
                conn.disconnect();
            }
        }
        return "";
    }

    private void parseAndReturn(String page) {
        if (captured) return;
        new Thread(() -> {
            try {
                List<ParsedCourse> result = new NuaaEamsParser(page).fetch();
                runOnUiThread(() -> {
                    if (result.isEmpty()) {
                        toast(R.string.err_import_parse);
                    } else {
                        captured = true;
                        Intent data = new Intent();
                        data.putExtra(EXTRA_HTML, page);
                        setResult(RESULT_OK, data);
                        finish();
                    }
                });
            } catch (ImportException e) {
                runOnUiThread(() -> toast(e.getMessage()));
            }
        }).start();
    }

    /** evaluateJavascript returns the string JSON-quoted ("...\\n..."); strip that. */
    private static String unescapeJsString(String js) {
        if (js == null) return "";
        if (js.length() >= 2 && js.startsWith("\"") && js.endsWith("\"")) {
            js = js.substring(1, js.length() - 1);
        }
        return js.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\/", "/")
                .replace("\\\\", "\\");
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void toast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_LONG).show();
    }
}
