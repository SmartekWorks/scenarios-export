package com.swathub.dev;

import org.apache.commons.io.FileUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

public class ScenariosExport {
	private static String apiGet(URIBuilder url, String user, String pass, JSONObject proxy) throws Exception {
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
				new AuthScope(url.getHost(), url.getPort()),
				new UsernamePasswordCredentials(user, pass));
		CloseableHttpClient httpclient = HttpClients.custom()
				.setDefaultCredentialsProvider(credsProvider)
				.build();

		String result = null;
		try {
			HttpGet httpget = new HttpGet(url.build());
			CloseableHttpResponse response = httpclient.execute(httpget);
			try {
				result = EntityUtils.toString(response.getEntity());
			} finally {
				response.close();
			}
		} finally {
			httpclient.close();
		}
		return result;
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("Usage: java -jar ScenarioExport.jar <config file> <target path>");
			return;
		}

		File configFile = new File(args[0]);
		if (!configFile.exists() || configFile.isDirectory()) {
			System.out.println("Config file is not exist.");
			return;
		}

		File targetFolder = new File(args[1]);
		if (!targetFolder.exists() && !targetFolder.mkdirs()) {
			System.out.println("Create target folder error.");
			return;
		}

		JSONObject config = new JSONObject(FileUtils.readFileToString(configFile, "UTF-8"));

		URIBuilder testsetUrl = new URIBuilder(config.getString("serverUrl"));
		testsetUrl.setPath("/api/" + config.getString("workspaceOwner") + "/" +
				config.getString("workspaceName") + "/sets/" + config.getString("setID"));
		System.out.println(testsetUrl.toString());
		String testsetResult = apiGet(testsetUrl, config.getString("username"), config.getString("apiKey"), null);
		if (testsetResult == null) {
			System.out.println("Testset not exists, file will not be created.");
			return;
		}
		System.out.println(testsetResult);
		JSONObject testset = new JSONObject(testsetResult);

		URIBuilder getUrl = new URIBuilder(config.getString("serverUrl"));
		getUrl.setPath("/api/" + config.getString("workspaceOwner") + "/" +
				config.getString("workspaceName") + "/sets/" + config.getString("setID") + "/scenarios");

		String results = apiGet(getUrl, config.getString("username"), config.getString("apiKey"), null);
		if (results == null) {
			System.out.println("Config file is not correct.");
			return;
		}
		JSONArray scenarios = new JSONArray(results);
		String csv = "シナリオ名,# ,名前,タイプ,コメント,パラメータ,バリュー" + System.getProperty("line.separator");
		for (int i = 0; i < scenarios.length(); i++) {
			JSONObject scenario = scenarios.getJSONObject(i);

			// get flow info
			URIBuilder flowUrl = new URIBuilder(config.getString("serverUrl"));
			flowUrl.setPath("/api/" + config.getString("workspaceOwner") + "/" +
					config.getString("workspaceName") + "/flows/" + scenario.getString("code"));
			flowUrl.setParameter("lang", "ja");
			String result = apiGet(flowUrl, config.getString("username"), config.getString("apiKey"), null);
			if (result == null) {
				System.out.println("Flow not exists, file will not be created.");
				System.out.println("");
				continue;
			}

			JSONObject flow = new JSONObject(result);
			for (int j = 0; j < flow.getJSONArray("steps").length(); j++) {
				JSONObject step = flow.getJSONArray("steps").getJSONObject(j);
				JSONObject component = step.getJSONObject("component");
				JSONArray dataList = step.getJSONArray("data");

				JSONObject comment = null;
				for (int k = 0; k < dataList.length(); k++) {
					JSONObject data = dataList.getJSONObject(k);
					if ("comment".equals(data.getString("code"))) {
						comment = data;
						dataList.remove(k);
						break;
					}
				}

				for (int k = 0; k < dataList.length(); k++) {
					JSONObject data = dataList.getJSONObject(k);
					String value = data.getString("value");
					if (data.has("queryMode") && "select".equals(data.getString("queryMode"))) {
						value = data.getString("selection");
					}
					csv += scenario.getString("name") + "," + (j + 1) + "," + component.getString("name") + "," +
							component.get("typeName") + "," + (comment==null?"":comment.getString("value")) + "," +
							data.getString("name") + "," + value + System.getProperty("line.separator");
				}
			}
		}

		File csvFile = new File(targetFolder, testset.getString("name") + ".csv");
		FileUtils.writeStringToFile(csvFile, csv, "utf-8");

		System.out.println(testset.getString("name") + ".csv is created.");
		System.out.println("");
	}
}
