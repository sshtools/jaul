package com.sshtools.jaul;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.install4j.api.context.UserCanceledException;
import com.install4j.api.launcher.ApplicationLauncher;
import com.install4j.api.launcher.ApplicationLauncher.Callback;
import com.install4j.api.launcher.ApplicationLauncher.ProgressListener;
import com.install4j.api.update.ApplicationDisplayMode;
import com.install4j.api.update.UpdateChecker;
import com.install4j.api.update.UpdateDescriptor;

public final class Install4JUpdater implements Callable<String> {

	public final static class Install4JUpdaterBuilder {

		private Optional<String> updateUrl = Optional.empty();
		private boolean consoleMode;
		private boolean checkOnly = true;
		private Optional<String> currentVersion = Optional.empty();
		private Optional<String> launcherId = Optional.empty();
		private Optional<Consumer<Integer>> onExit = Optional.empty();

		public static Install4JUpdaterBuilder builder() {
			return new Install4JUpdaterBuilder();
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

		public Install4JUpdater build() {
			return new Install4JUpdater(this);
		}

	}

	static Logger log = LoggerFactory.getLogger(Install4JUpdateService.class);
	private final String uurl;
	private final boolean consoleMode;
	private final String currentVersion;
	private final String launcherId;
	private final boolean checkOnly;
	private final Optional<Consumer<Integer>> onExit;

	private Install4JUpdater(Install4JUpdaterBuilder builder) {
		this.uurl = builder.updateUrl.orElseThrow(() -> new IllegalStateException("Must provide update URL"));
		this.consoleMode = builder.consoleMode;
		this.currentVersion = builder.currentVersion
				.orElseThrow(() -> new IllegalStateException("Current version must be supplied"));
		this.launcherId = builder.launcherId
				.orElseThrow(() -> new IllegalStateException("Launcher ID must be supplied"));
		this.checkOnly = builder.checkOnly;
		this.onExit = builder.onExit;
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
				return availableVersion;
			} else {
				String[] args;
				if (consoleMode)
					args = new String[] { "-c" };
				else
					args = new String[0];
				ApplicationLauncher.launchApplicationInProcess(launcherId, args, new ApplicationLauncher.Callback() {
					@Override
					public void exited(int exitValue) {
						onExit.ifPresent(oe -> oe.accept(exitValue));
					}

					@Override
					public void prepareShutdown() {
						// TODO add your code here (not invoked on event dispatch thread)
					}

					@Override
					public ProgressListener createProgressListener() {
						// TODO Auto-generated method stub
						return Callback.super.createProgressListener();
					}
				}, ApplicationLauncher.WindowMode.FRAME, null);
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
