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
		return createUpdateService(app, version, new PreferenceBasedUpdateableAppContext(preferences, Optional.of(Phase.STABLE), version, Optional.empty()));
	}

	public static UpdateService deferrableUpdateService(Preferences preferences, Optional<Phase> defaultPhase, Optional<App> app, String version, ScheduledExecutorService scheduler) {
		return createUpdateService(app, version, new PreferenceBasedUpdateableAppContext(preferences, defaultPhase, version, Optional.of(scheduler)));
	}
	
	public static UpdateService autoUpdateService(Preferences preferences, Optional<Phase> defaultPhase, Optional<App> app, String version, ScheduledExecutorService scheduler) {
		return createUpdateService(app, version, new AutoPreferenceBasedUpdateableAppContext(preferences, defaultPhase, version, scheduler, true));
	}

	public static UpdateService createUpdateService(Optional<App> app, String version, UpdateableAppContext ctx) {
		try {
			if ("true".equals(System.getProperty("jaul.dummyUpdates"))) {
				return new DummyUpdateService(ctx, DummyUpdaterBuilder.builder(), version);
			}
			if(app.isPresent()) {
				return Install4JUpdateService.defaultInstall4JUpdateService(ctx, version, app.get());
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

		public DownloadEvent(Type type, long value) {
			super();
			this.type = type;
			this.value = value;
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
