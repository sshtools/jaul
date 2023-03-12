package com.sshtools.jaul;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.NodeChangeEvent;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.install4j.api.Util;
import com.install4j.api.update.ApplicationDisplayMode;

public class AppRegistry {

	public enum Scope {
		USER, SYSTEM
	}

	public final static class App {
		private final String id;
		private final Path dir;
		private final Scope scope;
		private final Optional<String> updatesUrl;
		private final String launcherId;
		private final AppCategory category;
		private final Preferences appPreferences;

		private App(Scope scope, Preferences node, Preferences preferencesRoot) {
			this.scope = scope;
			id = node.get("id", "unknown");
			if (id.equals(""))
				throw new IllegalArgumentException("Invalid app data, missing ID.");
			this.appPreferences = preferencesRoot.node(id.replace('.', '/'));
			var dirPath = node.get("dir", "");
			if (dirPath.equals(""))
				throw new IllegalArgumentException("Invalid app data, missing directory.");
			dir = Path.of(dirPath);
			launcherId = node.get("launcherId", "");
			if (launcherId.equals(""))
				throw new IllegalArgumentException("Invalid app data, missing launcherId.");
			var descriptorStr = node.get("updatesUrl", "");
			updatesUrl = descriptorStr.equals("") ? Optional.empty() : Optional.ofNullable(descriptorStr);
			category = AppCategory.valueOf(node.get("category", ApplicationDisplayMode.GUI.name()));
		}
		
		public final Preferences getAppPreferences() {
			return appPreferences;
		}

		public final Phase getPhase() {
			return Phase.valueOf(getAppPreferences().get("phase", Phase.STABLE.name()));
		}

		public final AppCategory getCategory() {
			return category;
		}

		public final Optional<String> getUpdatesUrl() {
			return updatesUrl;
		}

		public final String getLauncherId() {
			return launcherId;
		}

		public final Scope getScope() {
			return scope;
		}

		public final String getId() {
			return id;
		}

		public final Path getDir() {
			return dir;
		}

	}

	static Logger log = LoggerFactory.getLogger(AppRegistry.class);

	private static AppRegistry instance;

	private Optional<Preferences> systemPreferences = Optional.empty();
	private Optional<Preferences> userPreferences = Optional.empty();
	private List<Consumer<App>> listeners = new ArrayList<>();

	public static AppRegistry get() {
		if (instance == null) {
			instance = new AppRegistry();
		}
		return instance;
	}

	static URL parseURL(String urlStr) {
		try {
			return new URL(urlStr);
		} catch (Exception e) {
			return null;
		}
	}

	AppRegistry() {
		try {
			var prefs = Preferences.systemNodeForPackage(AppRegistry.class);
			prefs.putBoolean("marker", true);
			prefs.flush();
			prefs.addNodeChangeListener(new NodeChangeListener() {
				@Override
				public void childRemoved(NodeChangeEvent evt) {
				}

				@Override
				public void childAdded(NodeChangeEvent evt) {
				}
			});
			systemPreferences = Optional.of(prefs);
		} catch (Exception e) {
		}

		try {
			var prefs = Preferences.userNodeForPackage(AppRegistry.class);
			prefs.putBoolean("marker", true);
			prefs.flush();
			userPreferences = Optional.of(prefs);
		} catch (Exception e) {
		}
	}

	public List<App> getApps() {
		var l = new ArrayList<App>();
		systemPreferences.ifPresent(p -> {
			try {
				for (String k : p.childrenNames()) {
					try {
						var node = p.node(k);
						l.add(checkApp(new App(Scope.SYSTEM, node, Preferences.systemRoot()), node));
					} catch (Exception e) {
						if (log.isDebugEnabled())
							log.error(MessageFormat.format("Failed to add app {0}.", k), e);
						else
							log.error(MessageFormat.format("Failed to add app {0}. {1}", k, e.getMessage()));
					}
				}
			} catch (BackingStoreException e) {
				log.error("Failed to list system apps.", e);
			}
		});
		userPreferences.ifPresent(p -> {
			try {
				for (String k : p.childrenNames()) {
					try {
						var node = p.node(k);
						l.add(checkApp(new App(Scope.USER, node, Preferences.userRoot()), node));
					} catch (Exception e) {
						if (log.isDebugEnabled())
							log.error(MessageFormat.format("Failed to add app {0}.", k), e);
						else
							log.error(MessageFormat.format("Failed to add app {0}. {1}", k, e.getMessage()));
					}
				}
			} catch (BackingStoreException e) {
				log.error("Failed to list system apps.", e);
			}
		});
		return l;
	}

	public void deregister(Class<?> clazz) {
		if (Util.isAdminGroup()) {
			log.info("De-registering as system wide application.");
			systemPreferences.ifPresent(p -> {
				deregister(clazz, p);
			});
		} else {
			log.info("De-registering as user application.");
			userPreferences.ifPresent(p -> {
				deregister(clazz, p);
			});
		}
	}

	public App get(Class<?> clazz) {
		var appDir = resolveAppDir();
		var appFile = appDir.resolve(".install4j").resolve("i4jparams.conf");
		var jaulApp = clazz.getAnnotation(JaulApp.class);
		if (jaulApp == null)
			throw new IllegalArgumentException(
					MessageFormat.format("A registrable app must use the {0} annotation on the class {1}",
							JaulApp.class.getName(), clazz.getName()));

		var id = jaulApp.id();
		var appNode = id.replace('.', '/');

		if (Files.exists(appFile)) {
			if (Util.isAdminGroup()) {
				if (systemPreferences.isPresent()) {
					return new App(Scope.SYSTEM, systemPreferences.get().node(id), Preferences.systemRoot());
				}
				else
					throw new IllegalArgumentException("Cannot get app as SYSTEM because no system preferences are present.");
			} else {
				if (userPreferences.isPresent()) {
					log.info("Registering as user application.");
					return new App(Scope.USER, userPreferences.get().node(id), Preferences.userRoot());
				}
				else
					throw new IllegalArgumentException("Cannot get app as USER because no user preferences are present.");
			}
		}
		else
		throw new IllegalArgumentException("Cannot get app, as system property 'install4j.installationDir' is "
				+ "not set, and the current working directory does not appear to be an "
				+ "installed application either. Either ensure the system property is set (usually as a VM parameter in the launcher in the Install4j project), or ensure the application runs from its installed directory (not usually ideal for command line apps).");
	}

	public App register(Class<?> clazz) {
		var appDir = resolveAppDir();
		var appFile = appDir.resolve(".install4j").resolve("i4jparams.conf");

		var jaulApp = clazz.getAnnotation(JaulApp.class);
		if (jaulApp == null)
			throw new IllegalArgumentException(
					MessageFormat.format("A registrable app must use the {0} annotation on the class {1}",
							JaulApp.class.getName(), clazz.getName()));

		if (Files.exists(appFile)) {
			if (Util.isAdminGroup()) {
				if (systemPreferences.isPresent()) {
					log.info("Registering as system wide application.");
					return new App(Scope.SYSTEM,
							saveToPreferences(jaulApp, clazz, appDir, appFile, systemPreferences.get()),
							Preferences.systemRoot());
				}
				else
					throw new IllegalArgumentException("Cannot register app as SYSTEM because no system preferences are present.");
			} else {
				if (userPreferences.isPresent()) {
					log.info("Registering as user application.");
					return new App(Scope.USER,
							saveToPreferences(jaulApp, clazz, appDir, appFile, userPreferences.get()),
							Preferences.userRoot());
				}
				else
					throw new IllegalArgumentException("Cannot register app as USER because no user preferences are present.");
			}
		}
		else {
			throw new IllegalArgumentException("Cannot register app, as system property 'install4j.installationDir' is "
					+ "not set, and the current working directory does not appear to be an "
					+ "installed application either. Either ensure the system property is set (usually as a VM parameter in the launcher in the Install4j project), or ensure the application runs from its installed directory (not usually ideal for command line apps).");
		}
	}

	private App checkApp(App app, Preferences node) throws IOException, BackingStoreException {
		if (Files.exists(app.dir.resolve(".install4j").resolve("i4jparams.conf"))) {
			return app;
		} else {
			node.removeNode();
			throw new IOException(app.getId() + " has been uninstalled.");
		}
	}

	private Path resolveAppDir() {
		var appDir = Paths.get(System.getProperty("install4j.installationDir", System.getProperty("user.dir")));
		var appFile = appDir.resolve(".install4j").resolve("i4jparams.conf");

		// in case .install4j is a symbolic link to real installation (used when
		// debugging Jaul)
		try {
			appDir = appFile.toRealPath().getParent().getParent().toAbsolutePath();
		} catch (IOException ioe) {
		}
		return appDir;
	}

	private void deregister(Class<?> clazz, Preferences p) {
		var appNode = p.node(clazz.getName());
		try {
			appNode.removeNode();
		} catch (BackingStoreException e) {
			log.error("Failed to de-register application.", e);
		}
	}

	private Preferences saveToPreferences(JaulApp app, Class<?> clazz, Path appDir, Path appFile, Preferences p) {

		var appNode = p.node(app.id());
		try {
			appNode.put("updatesUrl", app.updatesUrl());
			appNode.put("launcherId", app.updaterId());
			appNode.put("category", app.category().name());
			appNode.put("id", app.id());
			appNode.put("dir", appDir.toAbsolutePath().toString());
		} catch (Exception ioe) {
			log.warn("Cannot register app.", ioe);
		}
		return appNode;
	}
}
