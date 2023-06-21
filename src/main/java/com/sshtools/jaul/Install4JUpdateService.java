package com.sshtools.jaul;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.install4j.api.launcher.ApplicationLauncher;
import com.sshtools.jaul.AppRegistry.App;
import com.sshtools.jaul.Install4JUpdater.Install4JUpdaterBuilder;

public class Install4JUpdateService extends AbstractUpdateService {

	public static Install4JUpdateService defaultInstall4JUpdateService(UpdateableAppContext context, String version, App app) {
		return defaultInstall4JUpdateService(context, version, app, false);
	}
	
	public static Install4JUpdateService defaultInstall4JUpdateService(UpdateableAppContext context, String version, App app, boolean consoleMode) {

		/* Force loading of I4J so if it doesn't exist we know earlier */
		ApplicationLauncher.isNewArchiveInstallation();

		return new Install4JUpdateService(context, 
				Install4JUpdaterBuilder.builder().
				withCurrentVersion(version).
				withConsoleMode(consoleMode).
				withLauncherId(app.getLauncherId()).
				withUpdateUrl(app.getUpdatesUrl().get().replace("${phase}", context.getPhase().name().toLowerCase())).
				onExit((e) -> System.exit(e)),
				version);

	}

	static Logger log = LoggerFactory.getLogger(Install4JUpdateService.class);
	
	private final Install4JUpdaterBuilder builder;

	public Install4JUpdateService(UpdateableAppContext context, Install4JUpdaterBuilder builder, String currentVersion) {
		super(context, currentVersion);
		this.builder = builder;
	}

	@Override
	protected String doUpdate(boolean checkOnly) throws IOException {
		return builder.withCheckOnly(checkOnly).build().call();

	}

}
