package org.mitre.eyesfirst.solr;

import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleSolrService implements SolrService {
	private final String solrUpdateURL;
	private final Logger log = LoggerFactory.getLogger(getClass());

	public SimpleSolrService(String solrUpdateURL) {
		if (solrUpdateURL == null)
			throw new NullPointerException();
		this.solrUpdateURL = solrUpdateURL;
	}

	@Override
	public String updateSolr() throws Exception {
		log.info("Sending SOLR update to {}...", solrUpdateURL);
		URL url = new URL(solrUpdateURL);
		URLConnection connection = url.openConnection();
		connection.connect();
		// Java still provides no way to get the charset from a URL, so pretend
		// everything is always UTF-8.
		InputStreamReader in = new InputStreamReader(connection.getInputStream(), "UTF-8");
		StringBuilder result = new StringBuilder();
		char[] buf = new char[1024];
		while (true) {
			int r = in.read(buf);
			if (r < 0)
				break;
			result.append(buf, 0, r);
		}
		return result.toString();
	}

}
