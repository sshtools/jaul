package com.sshtools.jaul;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.install4j.api.Util;
import com.install4j.api.update.ApplicationDisplayMode;
import com.sshtools.jaul.Telemetry.TelemetryBuilder;
import com.sshtools.jaul.TelemetryEvent.Type;

public class AppRegistry {
	@SuppressWarnings("serial")
	public final static class App implements Serializable {
		private final String id;
		private final String dir;
		private final Scope scope;
		private final String updatesUrl;
		private final String launcherId;
		private final AppCategory category;
		private final String appPreferences;

		App(Scope scope, Preferences node) {
			this.scope = scope;
			id = node.get("id", "unknown");
			if (id.equals(""))
				throw new IllegalArgumentException("Invalid app data, missing ID.");
			this.appPreferences = id.replace('.', '/');
			var dirPath = node.get("appDir", "");
			if (dirPath.equals(""))
				throw new IllegalArgumentException("Invalid app data, missing directory.");
			dir = dirPath;
			launcherId = node.get("launcherId", "");
			if (launcherId.equals(""))
				throw new IllegalArgumentException("Invalid app data, missing launcherId.");
			var descriptorStr = node.get("updatesUrl", "");
			updatesUrl = descriptorStr.equals("") ? null : descriptorStr;
			category = AppCategory.valueOf(node.get("category", ApplicationDisplayMode.GUI.name()));
		}

		public final Preferences getAppPreferences() {
			return Preferences.userRoot().node(appPreferences);
		}

		public final Phase getPhase() {
			return Phase.valueOf(getAppPreferences().get(AppRegistry.KEY_PHASE, Phase.STABLE.name()));
		}

		public final AppCategory getCategory() {
			return category;
		}

		public final Optional<String> getUpdatesUrl() {
			return Optional.ofNullable(updatesUrl);
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
			return Paths.get(dir);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Objects.hash(id, scope);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			App other = (App) obj;
			return Objects.equals(id, other.id) && scope == other.scope;
		}


	}

	public final static String KEY_PHASE = "phase";
	public final static String KEY_AUTOMATIC_UPDATES = "automaticUpdates";
	public final static String KEY_DEFER = "updatesDeferredUntil";

	public enum Scope {
		USER, SYSTEM
	}

	static Logger log = LoggerFactory.getLogger(AppRegistry.class);

	private static AppRegistry instance;

//	private Optional<Preferences> systemPreferences = Optional.empty();
//	private Optional<Preferences> userPreferences = Optional.empty();

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
	}

	public Preferences getSystemPreferences() {
		return Preferences.systemNodeForPackage(AppRegistry.class).node("registry");
	}

	public Preferences getUserPreferences() {
		return Preferences.userNodeForPackage(AppRegistry.class).node("registry");
	}
	
	public List<App> getApps() {
		return getApps(Optional.empty());
	}

	public List<App> getApps(Optional<Scope> scope) {
		var l = new ArrayList<App>();
		if(scope.isEmpty() || scope.get().equals(Scope.USER)) {
			log.info("Retrieving user applications.");
			var p = getUserPreferences();
			try {
				p.sync();
			} catch (BackingStoreException e) {
			}
			try {
				for (var k : p.childrenNames()) {
					try {
						var node = p.node(k);
						log.info("    {}", k);
						l.add(checkApp(new App(Scope.USER, node), node));
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
		}
		if((Util.hasFullAdminRights() && scope.isEmpty()) || (!scope.isEmpty() && scope.get().equals(Scope.SYSTEM))) {
			var s = getSystemPreferences();
			try {
				s.sync();
			} catch (BackingStoreException e) {
			}
			log.info("Retrieving system applications.");
			try {
				for (var k : s.childrenNames()) {
					try {
						var node = s.node(k);
						log.info("    {}", k);
						if (contains(k, l)) {
							log.warn("Already installed as user app, that will take precedence.");
						} else
							l.add(checkApp(new App(Scope.SYSTEM, node), node));
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
		}
		return l;
	}
	
	public void deregister(String id) {

		try {
			deregister(get(id));
		}
		catch(IllegalStateException ise) {
			log.warn("Could not deregister.", ise);
		}
	}

	public void deregister(Class<?> clazz) {
		try {
			deregister(get(clazz));
		}
		catch(IllegalStateException ise) {
			log.warn("Could not deregister.", ise);
		}
	}

	private void deregister(App app) {
		var telem = telemetryForApp(app).build();
		telem.event(telem.builder().withType(Type.DEREGISTER).withDescription("Application deregistered.").build());
		telem.sendNow().ifPresent(t -> {
			try {
				t.join(Duration.ofSeconds(20).toMillis());
			} catch (InterruptedException e) {
			}
		});
		if (app.getScope() == Scope.SYSTEM) {
			log.info("De-registering as system wide application.");
			deregister(app, getSystemPreferences());
		} else {
			log.info("De-registering as user application.");
			deregister(app, getUserPreferences());
		}
	}

	private TelemetryBuilder telemetryForApp(App app) {
		return TelemetryBuilder.builder().withApp(app);
	}

	public App get(Class<?> clazz) {
		String id;
		var jaulApp = clazz.getAnnotation(JaulApp.class);
		if(jaulApp == null) {
			var jaulAppComponent = clazz.getAnnotation(JaulAppComponent.class);
			if(jaulAppComponent == null) {
				throw new IllegalArgumentException(
						MessageFormat.format("A registrable app must use the {0} annotation or the {1} annotation on the class {2}",
								JaulApp.class.getName(), JaulAppComponent.class.getName(), clazz.getName()));				
			}
			else {
				id = jaulAppComponent.id();
			}
		}
		else {
			id = jaulApp.id();
		}
		

		return get(id);
	}

	public App get(Path installDir) {
		try {
			log.debug("Retrieving as user application.");
			var userRoot = getUserPreferences();
			for(var userAppId : userRoot.childrenNames()) {
				var app = new App(Scope.USER, userRoot.node(userAppId));
				try {
					if(installDir.toRealPath().equals(app.getDir().toRealPath())) {
						return app;
					}
				}
				catch(Exception ioe) {
					log.debug("Ignoring error looking for registered app give path " + installDir, ioe);
				}
			}
			
			log.debug("Retrieving as system application.");
			var sysRoot = getSystemPreferences();
			for(var systemAppId : sysRoot.childrenNames()) {
				var app = new App(Scope.SYSTEM, sysRoot.node(systemAppId));
				try {
					if(installDir.toRealPath().equals(app.getDir().toRealPath())) {
						return app;
					}
				}
				catch(Exception ioe) {
					log.debug("Ignoring error looking for registered app give path " + installDir, ioe);
				}
			}

			throw new IllegalStateException(
					"Cannot get app, as it has not been registered. This is usually done at installation time using '--jaul-register'. Either this did not happen,  "
							+ "or you are running in a development environment. You can fake an installation by linking '.install4j' directory from a real installation, then running this app with '--jaul-register'.");
		} catch (BackingStoreException bse) {
			throw new IllegalStateException("Failed to query preferences api for application registry details.", bse);
		}
	}

	public App get(String id) {
		try {
			log.debug("Retrieving as user application.");
			var userRoot = getUserPreferences();
			if (Arrays.asList(userRoot.childrenNames()).contains(id)) {
				return new App(Scope.USER, userRoot.node(id));
			}
			log.debug("Retrieving as system application.");
			var sysRoot = getSystemPreferences();
			if (Arrays.asList(sysRoot.childrenNames()).contains(id)) {
				return new App(Scope.SYSTEM, sysRoot.node(id));
			}
			throw new IllegalStateException(
					"Cannot get app, as it has not been registered. This is usually done at installation time using '--jaul-register'. Either this did not happen,  "
							+ "or you are running in a development environment. You can fake an installation by linking '.install4j' directory from a real installation, then running this app with '--jaul-register'.");
		} catch (BackingStoreException bse) {
			throw new IllegalStateException("Failed to query preferences api for application registry details.", bse);
		}
	}

	public App launch(Class<?> clazz) {
		var app = get(clazz);
		var telem = telemetryForApp(app).build();
		telem.event(telem.builder().withType(Type.LAUNCH).withDescription("Application launched.").build());
		Runtime.getRuntime()
				.addShutdownHook(new Thread(() -> telem.event(
						telem.builder().withType(Type.SHUTDOWN).withDescription("Application shutdown.").build()),
						"TelemetryWrite"));
		return app;
	}

	public App register(Class<?> clazz) {
		return register(JaulAppProvider.fromClass(clazz));
	}
	
	public App register(JaulAppProvider jaulApp) {
		var appDir = resolveAppDir();
		var appFile = appDir.resolve(".install4j").resolve("i4jparams.conf");
		if (Files.exists(appFile)) {
			App app;
			if (!Boolean.getBoolean("jaul.forceUserRegistration") && Util.hasFullAdminRights()) {
				log.info("Registering as system wide application.");
				app = new App(Scope.SYSTEM,
						saveToPreferences(jaulApp, appDir, appFile, getSystemPreferences()));
			} else {
				log.info("Registering as user application.");
				app = new App(Scope.USER, saveToPreferences(jaulApp, appDir, appFile, getUserPreferences()));
			}
			var telem = telemetryForApp(app).build();
			telem.event(telem.builder().withType(Type.REGISTER).withDescription("Application registered.").build());
			return app;
		} else {
			throw new IllegalArgumentException("Cannot register app, as system property 'install4j.installationDir' is "
					+ "not set, and the current working directory does not appear to be an "
					+ "installed application either. Either ensure the system property is set (usually as a VM parameter in the launcher in the Install4j project), or ensure the application runs from its installed directory (not usually ideal for command line apps).");
		}
	}

	private boolean contains(String id, List<App> apps) {
		for (var a : apps)
			if (a.getId().equals(id))
				return true;
		return false;
	}

	private App checkApp(App app, Preferences node) throws IOException, BackingStoreException {
		if (Files.exists(app.getDir().resolve(".install4j").resolve("i4jparams.conf"))) {
			return app;
		} else {
			try {
				node.removeNode();
			}
			catch(Exception e) {
			}
			throw new IOException(app.getId() + " has been uninstalled from " + app.getDir());
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

	private void deregister(App app, Preferences p) {
		var appNode = p.node(app.getId());
		try {
			appNode.removeNode();
		} catch (BackingStoreException e) {
			log.error("Failed to de-register application.", e);
		}
	}

	private Preferences saveToPreferences(JaulAppProvider app, Path appDir, Path appFile, Preferences p) {

		log.info("App :");
		log.info("   ID: {}", app.id());
		log.info("   Updates URL: {}", app.updatesUrl());
		log.info("   Launcher ID: {}", app.updaterId());
		log.info("   Category: {}", app.category().name());
		log.info("   Dir: {}", appDir.toAbsolutePath().toString());
		var appNode = p.node(app.id());
		try {
			appNode.put("updatesUrl", app.updatesUrl());
			appNode.put("launcherId", app.updaterId());
			appNode.put("category", app.category().name());
			appNode.put("id", app.id());
			appNode.put("appDir", appDir.toAbsolutePath().toString());
			appNode.flush();
		} catch (Exception ioe) {
			log.warn("Cannot register app.", ioe);
		}
		return appNode;
	}

	public static Preferences getBestAppPreferences(Optional<App> appDef, Object appInstance) {
		if (appDef.isPresent()) {
			return appDef.get().getAppPreferences();
		} else {
			var jaulApp = appInstance.getClass().getAnnotation(JaulApp.class);
			if (jaulApp == null) {
				return Preferences.userNodeForPackage(appInstance.getClass())
						.node(appInstance.getClass().getSimpleName());
			} else {
				return Preferences.userRoot().node(jaulApp.id().replace('.', '/'));
			}
		}
	}
}
