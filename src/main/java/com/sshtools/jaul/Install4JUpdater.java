package com.sshtools.jaul;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.install4j.api.Util;
import com.install4j.api.context.UserCanceledException;
import com.install4j.api.launcher.ApplicationLauncher;
import com.install4j.api.launcher.ApplicationLauncher.Callback;
import com.install4j.api.launcher.ApplicationLauncher.ProgressListener;
import com.install4j.api.update.ApplicationDisplayMode;
import com.install4j.api.update.UpdateChecker;
import com.install4j.api.update.UpdateDescriptor;
import com.sshtools.jaul.AppRegistry.App;

public final class Install4JUpdater implements Callable<String> {
	
	public interface IORunnable {
		void run() throws IOException;
	}

	public static void runWithBestRuntimeDir(IORunnable runnable) throws IOException {
		if(Util.isWindows()) {
			runWithRuntimeDir(runnable, Paths.get(".install4j"));
			return;
		}
		runnable.run();
	}

	public static void runWithRuntimeDir(IORunnable runnable, Path dir) throws IOException {
		if(Files.exists(dir)) {
			var was = System.getProperty("install4j.runtimeDir");
			try {
				System.setProperty("install4j.runtimeDir", dir.toString());
				runnable.run();
			}
			finally {
				if(was == null)
					System.getProperties().remove("install4j.runtimeDir");
				else
					System.setProperty("install4j.runtimeDir", was);
			}
		}
		else
			runnable.run();
	}

	public final static class Install4JUpdaterBuilder {

		private Optional<String> updateUrl = Optional.empty();
		private boolean consoleMode;
		private boolean checkOnly = true;
		private boolean inProcess = true;
		private Optional<String> currentVersion = Optional.empty();
		private Optional<String> launcherId = Optional.empty();
		private Optional<String[]> args = Optional.empty();
		private Optional<Consumer<Integer>> onExit = Optional.empty();
		private Optional<Supplier<ProgressListener>> progressListenerFactory = Optional.empty();
		private Optional<Runnable> onPrepareShutdown = Optional.empty();

		public static Install4JUpdaterBuilder builder() {
			return new Install4JUpdaterBuilder();
		}
		
		public Install4JUpdaterBuilder withProgressListenerFactory(Supplier<ProgressListener> progressListenerFactory) {
			this.progressListenerFactory = Optional.of(progressListenerFactory);
			return this;
		}
		
		public Install4JUpdaterBuilder withArgs(String... args) {
			this.args = Optional.of(args);
			return this;
		}
		
		public Install4JUpdaterBuilder withArgs(Collection<String> args) {
			return withArgs(args.toArray(new String[0]));
		}
		
		public Install4JUpdaterBuilder withApp(App app, UpdateableAppContext ctx) {
			return withLauncherId(app.getLauncherId()).
				   withCurrentVersion(ctx.getVersion()).
				   withUpdateUrl(app.getUpdatesUrl().get().replace("${phase}", ctx.getPhase().name().toLowerCase()));
		}

		public Install4JUpdaterBuilder withoutInProcess() {
			return withInProcess(false);
		}

		public Install4JUpdaterBuilder withInProcess(boolean inProcess) {
			this.inProcess = inProcess;
			return this;
		}

		public Install4JUpdaterBuilder withConsoleMode() {
			return withConsoleMode(true);
		}

		public Install4JUpdaterBuilder withConsoleMode(boolean consoleMode) {
			this.consoleMode = consoleMode;
			return this;
		}

		public Install4JUpdaterBuilder withUpdate() {
			return withCheckOnly(false);
		}

		public Install4JUpdaterBuilder withCheckOnly() {
			return withCheckOnly(true);
		}

		public Install4JUpdaterBuilder withCheckOnly(boolean checkOnly) {
			this.checkOnly = checkOnly;
			return this;
		}

		public Install4JUpdaterBuilder withUpdateUrl(String updateUrl) {
			return withUpdateUrl(Optional.of(updateUrl));
		}

		public Install4JUpdaterBuilder withUpdateUrl(Optional<String> updateUrl) {
			this.updateUrl = updateUrl;
			return this;
		}

		public Install4JUpdaterBuilder withCurrentVersion(String currentVersion) {
			return withCurrentVersion(Optional.of(currentVersion));
		}

		public Install4JUpdaterBuilder withCurrentVersion(Optional<String> currentVersion) {
			this.currentVersion = currentVersion;
			return this;
		}

		public Install4JUpdaterBuilder withLauncherId(String launcherId) {
			return withLauncherId(Optional.of(launcherId));
		}

		public Install4JUpdaterBuilder withLauncherId(Optional<String> launcherId) {
			this.launcherId = launcherId;
			return this;
		}

		public Install4JUpdaterBuilder onExit(Consumer<Integer> onExit) {
			this.onExit = Optional.of(onExit);
			return this;
		}

		public Install4JUpdaterBuilder onPrepareShutdown(Runnable onPrepareShutdown) {
			this.onPrepareShutdown = Optional.of(onPrepareShutdown);
			return this;
		}

		public Install4JUpdater build() {
			return new Install4JUpdater(this);
		}

	}

	static Logger log = LoggerFactory.getLogger(Install4JUpdateService.class);
	private final String uurl;
	private final boolean consoleMode;
	private final boolean inProcess;
	private final String currentVersion;
	private final String launcherId;
	private final boolean checkOnly;
	private final Optional<Consumer<Integer>> onExit;
	private final Optional<Runnable> onPrepareShutdown;
	private final Optional<String[]> args;
	private final Optional<Supplier<ProgressListener>> progressListenerFactory;

	private Install4JUpdater(Install4JUpdaterBuilder builder) {
		this.args = builder.args;
		this.onPrepareShutdown = builder.onPrepareShutdown;
		this.uurl = builder.updateUrl.orElseThrow(() -> new IllegalStateException("Must provide update URL"));
		this.consoleMode = builder.consoleMode;
		this.inProcess = builder.inProcess;
		this.currentVersion = builder.currentVersion
				.orElseThrow(() -> new IllegalStateException("Current version must be supplied"));
		this.launcherId = builder.launcherId
				.orElseThrow(() -> new IllegalStateException("Launcher ID must be supplied"));
		this.checkOnly = builder.checkOnly;
		this.onExit = builder.onExit;
		this.progressListenerFactory = builder.progressListenerFactory;
	}

	@Override
	public String call() throws IOException {
		log.info("Check for updates in " + currentVersion + " from " + uurl);
		UpdateDescriptor update;
		try {
			update = UpdateChecker.getUpdateDescriptor(uurl,
					consoleMode ? ApplicationDisplayMode.CONSOLE : ApplicationDisplayMode.GUI);
			var best = update.getPossibleUpdateEntry();
			if (best == null) {
				log.info("No currentVersion available.");
				return System.getProperty("jajafx.fakeUpdateVersion");
			}

			var availableVersion = best.getNewVersion();
			log.info(availableVersion + " is available.");

			/* TODO: This will allow downgrades. */
			if (!availableVersion.equals(currentVersion)) {
				log.info("Update available.");
			} else {
				log.info("No update needed.");
				return null;
			}

			if (checkOnly) {
				log.info("Check only complete");
				return availableVersion;
			} else {
				var args = new ArrayList<String>();
				if (consoleMode)
					args.add("-c");
				this.args.ifPresent(a -> args.addAll(Arrays.asList(a)));

				log.info("Updater args ar {}", String.join(" ", args));
				
				runWithBestRuntimeDir(() -> {
					if(inProcess) {	
						log.info("Using in-process updater.");
						
						ApplicationLauncher.launchApplicationInProcess(launcherId, args.toArray(new String[0]), new ApplicationLauncher.Callback() {
							@Override
							public void exited(int exitValue) {
								log.info("Internal updater finished.");
								onExit.ifPresent(oe -> oe.accept(exitValue));
							} 
		
							@Override
							public void prepareShutdown() {
								// not invoked on event dispatch thread)
								log.info("Internal updater, prep. shutdown.");
								onPrepareShutdown.ifPresent(oe -> oe.run());
							}
		
							@Override
							public ProgressListener createProgressListener() {
								return progressListenerFactory.map(p -> p.get()).orElseGet(() -> Callback.super.createProgressListener());
							}
						}, ApplicationLauncher.WindowMode.FRAME, null);
					}
					else {
						log.info("Using external updater.");
						
						ApplicationLauncher.launchApplication(launcherId, args.toArray(new String[0]), true, new ApplicationLauncher.Callback() {
							@Override
							public void exited(int exitValue) {
								log.info("External updater finished.");
								onExit.ifPresent(oe -> oe.accept(exitValue));
							} 
		
							@Override
							public void prepareShutdown() {
								// not invoked on event dispatch thread)
								log.info("External updater, prep. shutdown.");
								onPrepareShutdown.ifPresent(oe -> oe.run());
							}
		
							@Override
							public ProgressListener createProgressListener() {
								return progressListenerFactory.map(p -> p.get()).orElseGet(() -> Callback.super.createProgressListener());
							}
						});
					}
				});
			}
		} catch (UserCanceledException e) {
			log.info("Cancelled.");
			throw new InterruptedIOException("Cancelled.");
		} catch (Exception e) {
			log.info("Failed.", e);
		}
		return null;

	}
}
