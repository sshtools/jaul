package com.sshtools.jaul;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.install4j.api.Util;
import com.sshtools.jaul.AppRegistry.App;

public class LocalAppDef implements AppDef {

	private final App app;
	private final String name;
	private final String version;
	private final String publisher;
	private final Optional<URL> url;
	private final Optional<String> icon;
	private final Optional<Path> uninstall;
	private final Optional<Path> updater;
	private final String rawDescriptorURL;

	public LocalAppDef(App app) {
		this.app = app;
		
		var appDir = app.getDir();
		var docBuilderFactory = DocumentBuilderFactory.newInstance();
		try {
			var i4jDir = appDir.resolve(".install4j");
			var docBuilder = docBuilderFactory.newDocumentBuilder();
			var appFile = i4jDir.resolve("i4jparams.conf");
			try (var in = Files.newInputStream(appFile)) {
				var doc = docBuilder.parse(in);
				var el = doc.getDocumentElement().getElementsByTagName("general").item(0);
				name = el.getAttributes().getNamedItem("applicationName").getTextContent();
				version = el.getAttributes().getNamedItem("applicationVersion").getTextContent();
				publisher = el.getAttributes().getNamedItem("publisherName").getTextContent();
				rawDescriptorURL= el.getAttributes().getNamedItem("publisherURL").getTextContent();
				url = rawDescriptorURL.equals("") ? Optional.empty() :Optional.of(new URL(rawDescriptorURL));
	
				var launchers = doc.getDocumentElement().getElementsByTagName("launcher");
				icon = findIcon(appDir, name, launchers);
			} 
		}
		catch(IOException | SAXException | ParserConfigurationException murle) {
			throw new IllegalArgumentException("Failed to load local app.", murle);
		}

		uninstall = optionalPath(resolveUninstaller(appDir));
		updater = optionalPath(resolveUpdater(appDir));
	}
	
	@Override
	public <A extends AppDef> A forBranch(Optional<String> branch) {
		throw new UnsupportedOperationException("TODO");
	}
	
	@Override
	public AppCategory getCategory() {
		return app.getCategory();
	}
	
	@Override
	public URL getDescriptor() {
		return app.getUpdatesUrl().map(u -> {
			try {
				return new URL(u.replace("${phase}", app.getPhase().name().toLowerCase()));
			} catch (MalformedURLException e) {
				throw new UncheckedIOException(e);
			}
		}).orElseThrow(() -> new IllegalStateException("No update descriptor set."));
	}

	@Override
	public Optional<String> getIcon() {
		return icon;
	}

	@Override
	public String getId() {
		return app.getId();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getPublisher() {
		return publisher;
	}

	@Override
	public String getRawDescriptorURL() {
		return rawDescriptorURL;
	}

	public App getRegistryDef() {
		return app;
	} 
	
	public final Optional<Path> getUninstall() {
		return uninstall;
	}

	public final Optional<Path> getUpdater() {
		return updater;
	}

	@Override
	public Optional<URL> getUrl() {
		return url;
	}

	@Override
	public String getVersion() {
		return version;
	}

	private static Optional<String> findIcon(Path appDir, String appname, NodeList launchers) {
		
		/* Grrr. This is because Mac doesn't have the icon as a file anyway when installed. 
		 * We have to explicitly install it */
		var defaultIcon = appDir.resolve("icon.png");
		if(Files.exists(defaultIcon)) {
			return Optional.of(defaultIcon.toString());
		}
		else {
			/*  I keept forgetting to add an icon.png, so alertnatively try and use `sips`
			 * to convert the icon for us in a temporary file
			 */
			if(System.getProperty("os.name").toLowerCase().contains("mac")) {
				var path = appDir.resolve(appname + ".app").resolve("Contents").resolve("Resources").resolve("app.icns");
				if(Files.exists(path)) {
					try {
						var tf = Files.createTempFile("appicn", ".png");
						tf.toFile().deleteOnExit();
						var p = new ProcessBuilder("sips", "-s", "format", "png", path.toAbsolutePath().toString(), "--out", tf.toAbsolutePath().toString());
						p.redirectError(Redirect.INHERIT);
						p.redirectInput(Redirect.INHERIT);
						var prc = p.start();
						if(prc.waitFor() != 0)
							throw new IOException("Unexpected exit value " + prc.exitValue());
						return Optional.of(tf.toAbsolutePath().toString());
					}
					catch(Exception e) {
					}	
				}
				
			}
		}

		var i4jDir = appDir.resolve(".install4j");
		for (int i = 0; i < launchers.getLength(); i++) {
			var launcher = launchers.item(i);
			var name = launcher.getAttributes().getNamedItem("name");
			var iconFile = i4jDir.resolve(name.getTextContent() + ".png");
			if (Files.exists(iconFile)) {
				return Optional.of(iconFile.toString());
			}
		}
		
		return Optional.empty();
	}

	private Optional<Path> optionalPath(Path path) {
		return path == null || !Files.exists(path) ? Optional.empty() : Optional.of(path);
	}

	private Path resolveUninstaller(Path appDir) {
		if(Util.isWindows()) {
			return appDir.resolve("uninstall.exe");
		}
		else if(Util.isMacOS()) {
			try(var stream = Files.newDirectoryStream(appDir)) {
				for(var file : stream) {
					if(file.getFileName().toString().contains("Uninstaller.app")) {
						return file.resolve("Contents").resolve("MacOS").resolve("JavaApplicationStub");
					}
				}
			}
			catch(IOException ioe) {
				throw new IllegalStateException("Failed to list application directory.", ioe);
			}
			return null;
		}
		else {
			return appDir.resolve("uninstall");
		}
	}

	private Path resolveUpdater(Path appDir) {
		var i4jDir = appDir.resolve(".install4j");
		if(Util.isWindows()) {
			return i4jDir.resolve("updater.exe");
		}
		else if(Util.isMacOS()) {
			return i4jDir.resolve("updater.app").resolve("Contents").resolve("MacOs").resolve("JavaApplicationStub");
		}
		else {
			return i4jDir.resolve("updater");
		}
	}
}
