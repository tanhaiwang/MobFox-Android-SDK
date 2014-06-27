
package com.adsdk.sdk.nativeads;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.adsdk.sdk.Const;
import com.adsdk.sdk.Log;
import com.adsdk.sdk.RequestException;
import com.adsdk.sdk.customevents.CustomEvent;
import com.adsdk.sdk.nativeads.NativeAd.ImageAsset;
import com.adsdk.sdk.nativeads.NativeAd.Tracker;

public class RequestNativeAd {

	public NativeAd sendRequest(NativeAdRequest request) throws RequestException {
		String url = request.toString();
		Log.d("Ad RequestPerform HTTP Get Url: " + url);
		DefaultHttpClient client = new DefaultHttpClient();
		HttpConnectionParams.setSoTimeout(client.getParams(), Const.SOCKET_TIMEOUT);
		HttpConnectionParams.setConnectionTimeout(client.getParams(), Const.CONNECTION_TIMEOUT);
		HttpProtocolParams.setUserAgent(client.getParams(), request.getUserAgent());
		HttpGet get = new HttpGet(url);
		get.setHeader("User-Agent", System.getProperty("http.agent"));
		HttpResponse response;
		try {
			response = client.execute(get);
			int responseCode = response.getStatusLine().getStatusCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				return parse(response.getEntity().getContent(), response.getAllHeaders());
			} else {
				throw new RequestException("Server Error. Response code:" + responseCode);
			}
		} catch (RequestException e) {
			throw e;
		} catch (ClientProtocolException e) {
			throw new RequestException("Error in HTTP request", e);
		} catch (IOException e) {
			throw new RequestException("Error in HTTP request", e);
		} catch (Throwable t) {
			throw new RequestException("Error in HTTP request", t);
		}
	}

	private List<CustomEvent> getCustomEvents(Header[] headers) {
		List<CustomEvent> customEvents = new ArrayList<CustomEvent>();
		if (headers == null) {
			return customEvents;
		}

		for (int i = 0; i < headers.length; i++) {
			if (headers[i].getName().startsWith("X-CustomEvent")) {
				String json = headers[i].getValue();
				JSONObject customEventObject;
				try {
					customEventObject = new JSONObject(json);
					String className = customEventObject.getString("class");
					String parameter = customEventObject.getString("parameter");
					String pixel = customEventObject.getString("pixel");
					CustomEvent event = new CustomEvent(className, parameter, pixel);
					customEvents.add(event);
				} catch (JSONException e) {
					Log.e("Cannot parse json with custom event: " + headers[i].getName());
				}

			}
		}

		return customEvents;
	}

	protected NativeAd parse(final InputStream inputStream, Header[] headers) throws RequestException {

		final NativeAd response = new NativeAd();

		try {
			List<CustomEvent> customEvents = this.getCustomEvents(headers);
			response.setCustomEvents(customEvents);

			BufferedReader reader;
			reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
			StringBuilder sb = new StringBuilder();

			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			String result = sb.toString();
			JSONObject mainObject = new JSONObject(result);
			JSONObject imageAssetsObject = mainObject.optJSONObject("imageassets");
			if (imageAssetsObject != null) {
				@SuppressWarnings("unchecked")
				Iterator<String> keys = imageAssetsObject.keys();

				while (keys.hasNext()) {
					String type = keys.next();
					JSONObject assetObject = imageAssetsObject.getJSONObject(type);
					String url = assetObject.getString("url");
					int width = assetObject.getInt("width");
					int height = assetObject.getInt("height");
					ImageAsset asset = new ImageAsset(url, width, height);
					response.addImageAsset(type, asset);
				}
			}

			JSONObject textAssetsObject = mainObject.optJSONObject("textassets");
			if (textAssetsObject != null) {
				@SuppressWarnings("unchecked")
				Iterator<String> keys = textAssetsObject.keys();
				while (keys.hasNext()) {
					String type = keys.next();
					String text = textAssetsObject.getString(type);
					response.addTextAsset(type, text);
				}
			}

			response.setClickUrl(mainObject.optString("click_url", null));

			JSONArray trackersArray = mainObject.optJSONArray("trackers");
			if (trackersArray != null) {
				for (int i = 0; i < trackersArray.length(); i++) {
					JSONObject trackerObject = trackersArray.optJSONObject(i);
					if (trackerObject != null) {
						String type = trackerObject.getString("type");
						String url = trackerObject.getString("url");
						Tracker tracker = new Tracker(type, url);
						response.getTrackers().add(tracker);
					}
				}
			}

		} catch (UnsupportedEncodingException e) {
			throw new RequestException("Cannot parse Response", e);
		} catch (IOException e) {
			throw new RequestException("Cannot parse Response", e);
		} catch (JSONException e) {
			response.setNativeAdValid(false);
		}
		response.setNativeAdValid(true);
		return response;
	}

}
