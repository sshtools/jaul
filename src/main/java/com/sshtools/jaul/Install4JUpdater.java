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

import com.install4j.api.context.UserCanceledException;
import com.install4j.api.launcher.ApplicationLauncher;
import com.install4j.api.launcher.ApplicationLauncher.Callback;
import com.install4j.api.launcher.ApplicationLauncher.ProgressListener;
import com.install4j.api.update.ApplicationDisplayMode;
import com.install4j.api.update.UpdateChecker;
import com.install4j.api.update.UpdateDescriptor;
import com.install4j.api.update.UpdateDescriptorEntry;
import com.sshtools.jaul.AppRegistry.App;

public class Install4JUpdater implements Callable<String> {
	
	public interface IORunnable {
		void run() throws IOException;
	}
	
	public static Path bestRuntimePath(Path path) {
//		if(Util.isWindows()) {
			return path.resolve(".install4j");
//		}
//		return path;
	}

	public static void runWithBestRuntimeDir(IORunnable runnable) throws IOException {
		runWithRuntimeDir(runnable, Paths.get(System.getProperty("user.dir")));
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

	public static <T> T callWithRuntimeDir(Callable<T> callable, Path dir) throws Exception {
		if(Files.exists(dir)) {
			var was = System.getProperty("install4j.runtimeDir");
			try {
				System.setProperty("install4j.runtimeDir", dir.toString());
				return callable.call();
			}
			finally {
				if(was == null)
					System.getProperties().remove("install4j.runtimeDir");
				else
					System.setProperty("install4j.runtimeDir", was);
			}
		}
		else
			return callable.call();
	}

	public final static class Install4JUpdaterBuilder extends AbstractInstall4JUpdaterBuilder<Install4JUpdaterBuilder, Install4JUpdater> {

		public static Install4JUpdaterBuilder builder() {
			return new Install4JUpdaterBuilder();
		}
		
		public Install4JUpdater build() {
			return new Install4JUpdater(this);
		}
	}

	public static abstract class AbstractInstall4JUpdaterBuilder<B extends AbstractInstall4JUpdaterBuilder<B, U>, U extends Install4JUpdater> {

		Optional<String> updateUrl = Optional.empty();
		boolean consoleMode;
		boolean checkOnly = true;
		boolean inProcess = true;
		Optional<String> currentVersion = Optional.empty();
		Optional<String> launcherId = Optional.empty();
		Optional<String[]> args = Optional.empty();
		Optional<Consumer<Integer>> onExit = Optional.empty();
		Optional<Supplier<ProgressListener>> progressListenerFactory = Optional.empty();
		Optional<Runnable> onPrepareShutdown = Optional.empty();
		Optional<Path> path = Optional.empty();

		
		@SuppressWarnings("unchecked")
		public B withProgressListenerFactory(Supplier<ProgressListener> progressListenerFactory) {
			this.progressListenerFactory = Optional.of(progressListenerFactory);
			return (B) this;
		}
		
		@SuppressWarnings("unchecked")
		public B withArgs(String... args) {
			this.args = Optional.of(args);
			return (B) this;
		}
		
		public B withArgs(Collection<String> args) {
			return withArgs(args.toArray(new String[0]));
		}
		
		public B withApp(App app, UpdateableAppContext ctx) {
			return withLauncherId(app.getLauncherId()).
				   withCurrentVersion(ctx.getVersion()).
				   withPath(app.getDir()).
				   withUpdateUrl(app.getUpdatesUrl().get().replace("${phase}", ctx.getPhase().name().toLowerCase()));
		}

		public B withoutInProcess() {
			return withInProcess(false);
		}

		@SuppressWarnings("unchecked")
		public B withInProcess(boolean inProcess) {
			this.inProcess = inProcess;
			return (B)this;
		}
		
		@SuppressWarnings("unchecked")
		public B withPath(Path path) {
			this.path = Optional.of(path);
			return (B)this;
		}

		public B withConsoleMode() {
			return withConsoleMode(true);
		}

		@SuppressWarnings("unchecked")
		public B withConsoleMode(boolean consoleMode) {
			this.consoleMode = consoleMode;
			return (B)this;
		}

		public B withUpdate() {
			return withCheckOnly(false);
		}

		public B withCheckOnly() {
			return withCheckOnly(true);
		}

		@SuppressWarnings("unchecked")
		public B withCheckOnly(boolean checkOnly) {
			this.checkOnly = checkOnly;
			return (B)this;
		}

		public B withUpdateUrl(String updateUrl) {
			return withUpdateUrl(Optional.of(updateUrl));
		}

		@SuppressWarnings("unchecked")
		public B withUpdateUrl(Optional<String> updateUrl) {
			this.updateUrl = updateUrl;
			return (B)this;
		}

		public B withCurrentVersion(String currentVersion) {
			return withCurrentVersion(Optional.of(currentVersion));
		}

		@SuppressWarnings("unchecked")
		public B withCurrentVersion(Optional<String> currentVersion) {
			this.currentVersion = currentVersion;
			return (B)this;
		}

		public B withLauncherId(String launcherId) {
			return withLauncherId(Optional.of(launcherId));
		}

		@SuppressWarnings("unchecked")
		public B withLauncherId(Optional<String> launcherId) {
			this.launcherId = launcherId;
			return (B)this;
		}

		@SuppressWarnings("unchecked")
		public B onExit(Consumer<Integer> onExit) {
			this.onExit = Optional.of(onExit);
			return (B)this;
		}

		@SuppressWarnings("unchecked")
		public B onPrepareShutdown(Runnable onPrepareShutdown) {
			this.onPrepareShutdown = Optional.of(onPrepareShutdown);
			return (B)this;
		}

		public abstract U build();

	}

	private final String uurl;
	private final boolean inProcess;
	private final String currentVersion;
	private final String launcherId;
	private final boolean checkOnly;
	
	protected final Optional<Consumer<Integer>> onExit;
	protected final Optional<Runnable> onPrepareShutdown;
	protected final Optional<Path> path;
	protected final boolean consoleMode;
	protected final Optional<String[]> args;
	protected final Optional<Supplier<ProgressListener>> progressListenerFactory;

	protected Install4JUpdater(AbstractInstall4JUpdaterBuilder<?, ?> builder) {
		this.args = builder.args;
		this.path = builder.path;
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
		Logging.info("Check for updates in " + currentVersion + " from " + uurl);

		try {
			return callWithRuntimeDir(() -> {
				UpdateDescriptor update;
				try {
					Logging.info("Instal4j runtime dir is " + System.getProperty("install4j.runtimeDir"));
					update = UpdateChecker.getUpdateDescriptor(uurl,
							consoleMode ? ApplicationDisplayMode.CONSOLE : ApplicationDisplayMode.GUI);
					
					var best = update.getPossibleUpdateEntry();
					if (best == null) {
						Logging.info("No currentVersion available.");
						return System.getProperty("jajafx.fakeUpdateVersion");
					}
	
					var availableVersion = best.getNewVersion();
					Logging.info(availableVersion + " is available.");
	
					/* TODO: This will allow downgrades. */
					if (!availableVersion.equals(currentVersion)) {
						Logging.info("Update available.");
					} else {
						Logging.info("No update needed.");
						return null;
					}
	
					if (checkOnly) {
						Logging.info("Check only complete");
						return availableVersion;
					} else {
						downloadAndExecuteUpdater(best);
					}
				} catch (UserCanceledException e) {
					Logging.info("Cancelled.");
					throw new InterruptedIOException("Cancelled.");
				} catch (Exception e) {
					Logging.info("Failed.", e);
				}
				return null;
			}, bestRuntimePath(path.orElse(Paths.get(System.getProperty("user.dir")))));
		}
		catch(IOException ioe) {
			throw ioe;
		}
		catch(Exception e) {
			throw new IllegalStateException("Failed update.", e);
		}
	}

	protected void downloadAndExecuteUpdater(UpdateDescriptorEntry best) throws IOException {
		var args = new ArrayList<String>();
		if (consoleMode)
			args.add("-c");
		this.args.ifPresent(a -> args.addAll(Arrays.asList(a)));
		args.add("-VupdatesUrl=" + uurl);		

		Logging.info("Updater args are {}", String.join(" ", args));
		
		if(inProcess) {	
			Logging.info("Using in-process updater.");
			
			ApplicationLauncher.launchApplicationInProcess(launcherId, args.toArray(new String[0]), new ApplicationLauncher.Callback() {
				@Override
				public void exited(int exitValue) {
					Logging.info("Internal updater finished.");
					onExit.ifPresent(oe -> oe.accept(exitValue));
				} 

				@Override
				public void prepareShutdown() {
					// not invoked on event dispatch thread)
					Logging.info("Internal updater, prep. shutdown.");
					onPrepareShutdown.ifPresent(oe -> oe.run());
				}

				@Override
				public ProgressListener createProgressListener() {
					return progressListenerFactory.map(p -> p.get()).orElseGet(() -> Callback.super.createProgressListener());
				}
			}, ApplicationLauncher.WindowMode.FRAME, null);
		}
		else {
			Logging.info("Using external updater.");
			
			ApplicationLauncher.launchApplication(launcherId, args.toArray(new String[0]), true, new ApplicationLauncher.Callback() {
				@Override
				public void exited(int exitValue) {
					Logging.info("External updater finished.");
					onExit.ifPresent(oe -> oe.accept(exitValue));
				} 

				@Override
				public void prepareShutdown() {
					// not invoked on event dispatch thread)
					Logging.info("External updater, prep. shutdown.");
					onPrepareShutdown.ifPresent(oe -> oe.run());
				}

				@Override
				public ProgressListener createProgressListener() {
					Logging.info("Creating progress monitor.");
					return progressListenerFactory.map(p -> p.get()).orElseGet(() -> Callback.super.createProgressListener());
				}
			});
		}
	}
}
