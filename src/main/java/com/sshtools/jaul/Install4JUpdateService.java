package com.sshtools.jaul;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;

import com.install4j.api.launcher.ApplicationLauncher;
import com.install4j.api.launcher.ApplicationLauncher.ProgressListener;
import com.sshtools.jaul.AppRegistry.App;
import com.sshtools.jaul.Install4JUpdater.AbstractInstall4JUpdaterBuilder;
import com.sshtools.jaul.Install4JUpdater.Install4JUpdaterBuilder;
import com.sshtools.jaul.UpdateService.DownloadEvent.Type;

public class Install4JUpdateService extends AbstractUpdateService {
	@Deprecated
	public static Install4JUpdateService defaultInstall4JUpdateService(UpdateableAppContext context, App app) {
		return defaultInstall4JUpdateService(context, context.getVersion(), app, false);
	}

	@Deprecated
	public static Install4JUpdateService defaultInstall4JUpdateService(UpdateableAppContext context, App app, boolean consoleMode) {
		return defaultInstall4JUpdateService(context, context.getVersion(), app, consoleMode);
	}

	@Deprecated
	public static Install4JUpdateService defaultInstall4JUpdateService(UpdateableAppContext context, String version, App app) {
		return defaultInstall4JUpdateService(context, version, app, false);
	}

	@Deprecated
	public static Install4JUpdateService defaultInstall4JUpdateService(UpdateableAppContext context, String version, App app, boolean consoleMode) {

		/* Force loading of I4J so if it doesn't exist we know earlier */
		ApplicationLauncher.isNewArchiveInstallation();

		return new Install4JUpdateService(context, 
				() -> Install4JUpdaterBuilder.builder().
				withCurrentVersion(version).
				withConsoleMode(consoleMode).
				withLauncherId(app.getLauncherId()).
				withUpdateUrl(app.getUpdatesUrl().get().replace("${phase}", context.getPhase().name().toLowerCase())).
				onExit((e) -> System.exit(e)));

	}

	private final Supplier<? extends AbstractInstall4JUpdaterBuilder<?, ?>> builderFactory;

	public Install4JUpdateService(UpdateableAppContext context, Supplier<? extends AbstractInstall4JUpdaterBuilder<?, ?>> builderFactory) {
		super(context);
		this.builderFactory = builderFactory;
	}
	
	ProgressListener progressListener() {
		return new ProgressListener() {
			
			private String message;
			private String detail;
			private int percent = -1;

			@Override
			public void screenActivated(String id) {
			}

			@Override
			public void actionStarted(String id) {
			}

			@Override
			public void statusMessage(String message) {
				if(!Objects.equals(this.message, message)) {
					Logging.info("Update Message: {0}", message);
					this.message = message;
					fireDownload(new DownloadEvent(Type.PROGRESS, percent, message, detail));
				}
			}

			@Override
			public void detailMessage(String detail) {
				if(!Objects.equals(this.detail, detail)) {
					
					if(Logging.isDebugEnabled())
						Logging.debug("Update Detail: {0}", detail);
					
					this.detail = detail;
					fireDownload(new DownloadEvent(Type.PROGRESS, percent, message, detail));
				}
			}

			@Override
			public void percentCompleted(int value) {
				percent = value;
				fireDownload(new DownloadEvent(Type.PROGRESS, percent, message, detail));
			}

			@Override
			public void secondaryPercentCompleted(int value) {
			}

			@Override
			public void indeterminateProgress(boolean indeterminateProgress) {
				if(indeterminateProgress && percent != -1) {
					percent = -1;
					fireDownload(new DownloadEvent(Type.PROGRESS, percent, message, detail));
				}
				else if(!indeterminateProgress && percent == -1) {
					percent = 0;
					fireDownload(new DownloadEvent(Type.PROGRESS, percent, message, detail));
				}
			}}
		;
	}

	@Override
	protected String doUpdate(boolean checkOnly) throws IOException {
		return builderFactory.get().
				withCheckOnly(checkOnly).
				withCurrentVersion(getContext().getVersion()).
				withProgressListenerFactory(this::progressListener).
				build().
				call();

	}

}
