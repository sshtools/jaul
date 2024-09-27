package com.sshtools.jaul;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

public class Lang {
	
	public static String filename(String path) {
		var idx = path.lastIndexOf('/');
		if(idx == -1)
			idx = path.lastIndexOf('\\');
		return idx == -1 ? path  : path.substring(idx + 1);
	}
	
	public static URL resolve(URL baseUrl, String rel) {
		try {
			return baseUrl.toURI().resolve(rel).toURL();
		} catch (MalformedURLException | URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public static Optional<String> optionalText(String txt) {
		return txt.equals("") ? Optional.empty() : Optional.of(txt);
	}

	public static String dirname(String path) {
		var idx = path.lastIndexOf('/');
		if(idx == -1)
			idx = path.lastIndexOf('\\');
		return idx == -1 ? path  : path.substring(0, idx);
	}
}
