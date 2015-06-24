package ca.jarcode.consoles.computer.interpreter.libraries;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

public class NetLibrary {
	public Socket newSocket(String host, Integer port) throws IOException {
		return new Socket(host, port);
	}
	public HTTPResponse httpGET(String url) throws IOException {
		HttpURLConnection con = buildConnection(url);

		int response = con.getResponseCode();
		String data = "";
		HTTPResponse res = new HTTPResponse(response);
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
			data = IOUtils.toString(reader);
			reader.close();
		}
		catch (IOException e) {
			res.exception = e.getClass().getSimpleName() + ": " + e.getMessage();
		}
		finally {
			res.data = data;
		}

		return res;
	}
	private HttpURLConnection buildConnection(String url) throws IOException {

		URL link = new URL(url);
		HttpURLConnection con = (HttpURLConnection) link.openConnection();

		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		return con;
	}
	public HTTPResponse httpPOST(String url, String parameters) throws IOException {

		HttpURLConnection con = buildConnection(url);

		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		con.setDoOutput(true);
		DataOutputStream out = new DataOutputStream(con.getOutputStream());
		out.writeBytes(parameters);
		out.flush();
		out.close();

		int response = con.getResponseCode();
		HTTPResponse res = new HTTPResponse(response);
		String data = "";
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
			data = IOUtils.toString(reader);
			reader.close();
		}
		catch (IOException e) {
			res.exception = e.getClass().getSimpleName() + ": " + e.getMessage();
		}
		finally {
			res.data = data;
		}
		return res;
	}
	public class HTTPResponse {

		private final int response;
		private String data;
		private String exception = null;

		public HTTPResponse(int response) {
			this.response = response;
		}

		public String getData() {
			return data;
		}

		public int getResponse() {
			return response;
		}

		public String getException() {
			return exception;
		}
	}
}
