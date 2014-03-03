package com.adsdk.sdk;

import static com.adsdk.sdk.Const.RESPONSE_ENCODING;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.adsdk.sdk.video.ResponseHandler;
import com.adsdk.sdk.video.RichMediaAd;
import com.adsdk.sdk.video.VASTParser;


public class RequestRichMediaAd extends RequestAd<RichMediaAd> {



	public RequestRichMediaAd() {
		is = null;
	}

	public RequestRichMediaAd(InputStream xmlArg) {
		is = xmlArg;
	}

	@Override
	RichMediaAd parseTestString() throws RequestException {
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();

			ResponseHandler myHandler = new ResponseHandler();
			xr.setContentHandler(myHandler);
			//			byte[] bytes = xml.getBytes(RESPONSE_ENCODING);
			//			InputSource src = new InputSource(new ByteArrayInputStream(bytes));
			Reader reader = new InputStreamReader(is,"UTF-8");
			InputSource src = new InputSource(reader);


			xr.parse(src);
			return myHandler.getRichMediaAd();
		} catch (Exception e) {
			throw new RequestException("Cannot parse Response:"
					+ e.getMessage(), e);
		}
	}

	@Override
	RichMediaAd parse(InputStream inputStream)
			throws RequestException {
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			ResponseHandler myHandler = new ResponseHandler();
			xr.setContentHandler(myHandler);
			RichMediaAd richMediaAd;
			if(Log.LOGGING_ENABLED){
				String response = convertStreamToString(inputStream);
				
				
				Log.d("Ad RequestPerform HTTP Response: " + response);
				byte[] bytes = response.getBytes(RESPONSE_ENCODING);
				InputSource src = new InputSource(new ByteArrayInputStream(bytes));
				src.setEncoding(RESPONSE_ENCODING);
				xr.parse(src);
				richMediaAd = myHandler.getRichMediaAd();
				
				richMediaAd.setVideo(VASTParser.fillVideoDataFromVast(richMediaAd.getVideo()));//FIXME: chyba przegiecie->przenie� do VideoData.createFromVAST?
				return richMediaAd;
			}
			else{
				InputSource src = new InputSource(inputStream);
				src.setEncoding(RESPONSE_ENCODING);
				xr.parse(src);
				richMediaAd = myHandler.getRichMediaAd();
				return richMediaAd;
			}
		} catch (Exception e) {
			throw new RequestException("Cannot parse Response:"
					+ e.getMessage(), e);
		}
	}

	private String convertStreamToString(java.io.InputStream is) {
		try {
			return new java.util.Scanner(is).useDelimiter("\\A").next();
		} catch (java.util.NoSuchElementException e) {
			return "";
		}
	}
}