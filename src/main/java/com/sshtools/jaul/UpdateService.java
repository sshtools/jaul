package com.sshtools.jaul;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import com.sshtools.jaul.AppRegistry.App;
import com.sshtools.jaul.DummyUpdater.DummyUpdaterBuilder;

public interface UpdateService {

	public static UpdateService basicUpdateService(Preferences preferences, Optional<App> app, String version) {
		return createUpdateService(app, version, new PreferenceBasedUpdateableAppContext(preferences, Optional.of(Phase.STABLE), version, Optional.empty()), false);
	}
	
	public static UpdateService basicConsoleUpdateService(Preferences preferences, Optional<App> app, String version) {
		return createUpdateService(app, version, new PreferenceBasedUpdateableAppContext(preferences, Optional.of(Phase.STABLE), version, Optional.empty()), true);
	}

	public static UpdateService deferrableUpdateService(Preferences preferences, Optional<Phase> defaultPhase, Optional<App> app, String version, ScheduledExecutorService scheduler) {
		return createUpdateService(app, version, new PreferenceBasedUpdateableAppContext(preferences, defaultPhase, version, Optional.of(scheduler)), false);
	}

	public static UpdateService deferrableConsoleUpdateService(Preferences preferences, Optional<Phase> defaultPhase, Optional<App> app, String version, ScheduledExecutorService scheduler) {
		return createUpdateService(app, version, new PreferenceBasedUpdateableAppContext(preferences, defaultPhase, version, Optional.of(scheduler)), true);
	}
	
	public static UpdateService autoUpdateService(Preferences preferences, Optional<Phase> defaultPhase, Optional<App> app, String version, ScheduledExecutorService scheduler) {
		return createUpdateService(app, version, new AutoPreferenceBasedUpdateableAppContext(preferences, defaultPhase, version, scheduler, true), false);
	}
	
	public static UpdateService autoConsoleUpdateService(Preferences preferences, Optional<Phase> defaultPhase, Optional<App> app, String version, ScheduledExecutorService scheduler) {
		return createUpdateService(app, version, new AutoPreferenceBasedUpdateableAppContext(preferences, defaultPhase, version, scheduler, true), true);
	}

	public static UpdateService createUpdateService(Optional<App> app, String version, UpdateableAppContext ctx, boolean consoleMode) {
		try {
			if ("true".equals(System.getProperty("jaul.dummyUpdates"))) {
				return new DummyUpdateService(ctx, DummyUpdaterBuilder.builder(), version);
			}
			if(app.isPresent()) {
				return Install4JUpdateService.defaultInstall4JUpdateService(ctx, version, app.get(), consoleMode);
			}
			else {
				return new NoUpdateService(ctx);
			}
		} catch (Throwable t) {
			System.err.println("Failed to create Install4J update service, using dummy service.");
			t.printStackTrace();
			return new NoUpdateService(ctx);
		}
	}

	public static class DownloadEvent {
		public enum Type {
			START, PROGRESS, END;
		}

		private final Type type;
		private final long value;
		private final String message;
		private final String detail;

		public DownloadEvent(Type type, long value, String message, String detail) {
			super();
			this.type = type;
			this.value = value;
			this.message = message;
			this.detail = detail;
		}
		
		public String getMessage() {
			return message;
		}
		
		public String getDetail() {
			return detail;
		}

		public Type getType() {
			return type;
		}

		public long getValue() {
			return value;
		}

	}

	public interface DownloadListener {
		void downloadEvent(DownloadEvent event);
	}
	
	UpdateableAppContext getContext();
	
	void addDownloadListener(DownloadListener listener);

	void removeDownloadListener(DownloadListener listener);

	boolean isNeedsUpdating();

	boolean isUpdating();

	boolean isCheckOnly();

	Phase[] getPhases();

	String getAvailableVersion();

	void deferUpdate();

	boolean isUpdatesEnabled();

	void checkForUpdate() throws IOException;

	void update() throws IOException;

	void shutdown();
	
	default void rescheduleCheck() {
	}

	void setOnAvailableVersion(Consumer<String> onAvailableVersion);

	void setOnBusy(Consumer<Boolean> busy);
}
