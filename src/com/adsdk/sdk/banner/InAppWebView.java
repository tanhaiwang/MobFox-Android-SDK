package com.adsdk.sdk.banner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import com.adsdk.sdk.Const;
import com.adsdk.sdk.Log;

public class InAppWebView extends Activity {

	public static final String URL_EXTRA = "extra_url";

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);

		final Intent intent = this.getIntent();

		initializeWebView(intent);

	}

	@SuppressLint("SetJavaScriptEnabled")
	private void initializeWebView(Intent intent) {
		WebView webView = new WebView(this);
		this.setContentView(webView);
		WebSettings webSettings = webView.getSettings();

		webSettings.setJavaScriptEnabled(true);

		webSettings.setSupportZoom(true);
		webSettings.setBuiltInZoomControls(true);
		webSettings.setUseWideViewPort(true);
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				Activity a = (Activity) view.getContext();
				Toast.makeText(a, "MRAID error: " + description, Toast.LENGTH_SHORT).show();
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (url == null)
					return false;

				Uri uri = Uri.parse(url);
				String host = uri.getHost();

				if ((url.startsWith("http:") || url.startsWith("https:")) && !"play.google.com".equals(host) && !"market.android.com".equals(host) && !url.endsWith(".apk")) {
					view.loadUrl(url);
					return true;
				}

				try {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
				} catch (ActivityNotFoundException exception) {
					Log.w("MoPub", "Unable to start activity for " + url + ". " + "Ensure that your phone can handle this intent.");
				}

				finish();
				return true;

			}

		});

		webView.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				Activity a = (Activity) view.getContext();
				a.setTitle("Loading...");
				a.setProgress(progress * 100);
				if (progress == 100)
					a.setTitle(view.getUrl());
			}
		});
		webView.loadUrl(intent.getStringExtra(Const.REDIRECT_URI));
	}

}
