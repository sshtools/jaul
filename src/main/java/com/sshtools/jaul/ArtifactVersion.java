package com.sshtools.jaul;

import java.io.File;
import java.net.URL;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilderFactory;

public class ArtifactVersion {
	
	public final static class VersionDetails {
		
		private final String version;
		private Optional<String> buildDate;

		private VersionDetails(String version) {
			this(version, (String)null);
		}

		private VersionDetails(String version, String buildDate) {
			this(version, "".equals(buildDate) ? Optional.empty() : Optional.ofNullable(buildDate));
		}

		private VersionDetails(String version, Optional<String> buildDate) {
			this.version = version;
			this.buildDate = buildDate;
		}
		
		public String version() {
			return version;
		}
		
		public Optional<String> buildDate() {
			return buildDate;
		}

		public VersionDetails withBuildDate(String bdate) {
			return new VersionDetails(version, bdate);
		}
	}

	static Map<String, VersionDetails> versions = Collections.synchronizedMap(new HashMap<>());

	public static String getVersion(String groupId, String artifactId) {
		return getVersion("*", groupId, artifactId);
	}
	
	public static boolean isDeveloperWorkspace() {
		return new File("pom.xml").exists();
	}
	
	/**
	 * Use {@link #getVersion(String, String)}.
	 * 
	 * @param installerShortName or '*' to get the first installed application
	 * @param groupId
	 * @param artifactId
	 * @return
	 */
	public static String getVersion(String installerShortName, String groupId, String artifactId) {
		return getVersionDetails(installerShortName, groupId, artifactId).version();
	}

	public static VersionDetails getVersionDetails(String groupId, String artifactId) {
		return getVersionDetails("*", groupId, artifactId);
	}
	
	/**
	 * Use {@link #getVersionDetails(String, String)}.
	 * 
	 * @param installerShortName or '*' to get the first installed application
	 * @param groupId
	 * @param artifactId
	 * @return version details
	 */
	public static VersionDetails getVersionDetails(String installerShortName, String groupId, String artifactId) {
		String fakeVersion = Boolean.getBoolean("jadaptive.development")
				? System.getProperty("jadaptive.development.version", System.getProperty("jadaptive.devVersion"))
				: null;
		
		String fakeBuildDate = Boolean.getBoolean("jadaptive.development")
				? System.getProperty("jadaptive.development.buildDate", System.getProperty("jadaptive.devBuildDate", DateFormat.getDateInstance(DateFormat.SHORT).format(new Date())))
				: null;
		
		if (fakeVersion != null) {
			return new VersionDetails(fakeVersion, fakeBuildDate);
		}

		VersionDetails detectedVersion = versions.getOrDefault(groupId+ ":" + artifactId, null);
		if (detectedVersion != null)
			return detectedVersion;
		
		/* installed apps may have a .install4j/i4jparams.conf. If this XML
		 * file exists, it will contain the full application version which
		 * will have the build number in it too. 
		 */
			try {
				var docBuilderFactory = DocumentBuilderFactory.newInstance();
				var docBuilder = docBuilderFactory.newDocumentBuilder();
				var appDir = new File(System.getProperty("install4j.installationDir", System.getProperty("user.dir")));
				var doc = docBuilder.parse(new File(new File(appDir, ".install4j"),"i4jparams.conf"));

				if(installerShortName != null) {
					var el = doc.getDocumentElement().getElementsByTagName("general").item(0);
					var mediaName = el.getAttributes().getNamedItem("mediaName").getTextContent();
					if("*".equals(installerShortName) || mediaName.startsWith(installerShortName + "-")) {
						detectedVersion = new VersionDetails(el.getAttributes().getNamedItem("applicationVersion").getTextContent());
					}
				}
			} catch (Exception e) {
			}


		if (detectedVersion == null || detectedVersion.buildDate.isEmpty()) {		
	
			// try to load from maven properties first
			try {
				var p = new Properties();
				var url = findMavenMeta(groupId, artifactId);
				if(url != null) {
					if(detectedVersion == null) {
						var is = url.openStream();
						if (is != null) {
							try {
								p.load(is);
								String vstr = p.getProperty("version", "");
								if(!vstr.equals(""))
									detectedVersion = new VersionDetails(vstr);
							} finally {
								is.close();
							}
						}
					}
					
					if(detectedVersion != null) {
						var urlStr = url.toExternalForm();
						var idx = urlStr.lastIndexOf('/', urlStr.lastIndexOf('/', urlStr.lastIndexOf('/', urlStr.lastIndexOf('/') - 1) - 1) - 1);
						var mfUrl= new URL(urlStr.substring(0, idx) + "MANIFEST.MF");
						try(var in = mfUrl.openStream()) {
							var mf = new Manifest(in);
							var bdate = (String)mf.getMainAttributes().getOrDefault("X-Build-Date", null);
							if(bdate != null) {
								detectedVersion = detectedVersion.withBuildDate(bdate);
							}
						}
					}
				}
				
				
			} catch (Exception e) {
				// ignore
			}
		}

		if (detectedVersion == null) {
			try {
				var docBuilderFactory = DocumentBuilderFactory.newInstance();
				var docBuilder = docBuilderFactory.newDocumentBuilder();
				var doc = docBuilder.parse(new File("pom.xml"));
				if(doc.getDocumentElement().getElementsByTagName("name").item(0).getTextContent().equals(artifactId) && doc.getDocumentElement().getElementsByTagName("group").item(0).getTextContent().equals(groupId)) {
					detectedVersion = new VersionDetails(doc.getDocumentElement().getElementsByTagName("version").item(0).getTextContent());
				}
			} catch (Exception e) {
			}

		}

		if (detectedVersion == null) {
			detectedVersion = new VersionDetails("DEV_VERSION");
		}

		/* Treat snapshot versions as build zero */
		if (detectedVersion.version.endsWith("-SNAPSHOT")) {
			detectedVersion = new VersionDetails(detectedVersion.version.substring(0, detectedVersion.version.length() - 9) + "-0", detectedVersion.buildDate);
		}

		versions.put(groupId+ ":" + artifactId, detectedVersion);

		return detectedVersion;
	}

	private static URL findMavenMeta(String groupId, String artifactId) {
		URL is = null;
		var cldr = Thread.currentThread().getContextClassLoader();
		if(cldr != null) {
			is = cldr.getResource("META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties");
		}
		if(is == null) {
			is = ArtifactVersion.class.getClassLoader()
					.getResource("META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties");
		}
		if (is == null) {
			is = ArtifactVersion.class
					.getResource("/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties");
		}
		return is;
	}
}
