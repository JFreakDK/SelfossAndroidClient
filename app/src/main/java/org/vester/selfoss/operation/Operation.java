package org.vester.selfoss.operation;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;

public interface Operation {

	void setURL(String url);

	URL createURL() throws MalformedURLException;

	String getRequestMethod();

	void processResponse(InputStream inputStream) throws JSONException, IOException;

	void writeOutput(HttpURLConnection con) throws IOException;

	void setLogin(String login);

	int getOperationTitle();

}
