package com.sshtools.jaul;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.prefs.Preferences;

public class PreferenceBasedUpdateableAppContext implements UpdateableAppContext {

	private final Preferences preferences;
	private final String version;
	private final Optional<Phase> defaultPhase;
	private final Optional<ScheduledExecutorService> scheduler;

	public PreferenceBasedUpdateableAppContext(Preferences preferences, Optional<Phase> defaultPhase, String version, Optional<ScheduledExecutorService> scheduler) {
		this.preferences = preferences;
		this.defaultPhase = defaultPhase;
		this.version = version;
		this.scheduler = scheduler;
	}

	public final Preferences getPreferences() {
		return preferences;
	}

	@Override
	public ScheduledExecutorService getScheduler() {
		return scheduler.orElseThrow(() -> new UnsupportedOperationException("No scheduler, updates cannot be deferred."));
	}

	@Override
	public boolean isAutomaticUpdates() {
		return false;
	}

	@Override
	public void setAutomaticUpdates(boolean automaticUpdates) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setUpdatesDeferredUntil(long timeMs) {
		getPreferences().putLong(AppRegistry.KEY_DEFER, timeMs);
	}

	@Override
	public long getUpdatesDeferredUntil() {
		return getPreferences().getLong(AppRegistry.KEY_DEFER, 0);
	}

	@Override
	public Phase getPhase() {
		return Phase.valueOf(getPreferences().get(AppRegistry.KEY_PHASE,
				defaultPhase.orElse(Phase.getDefaultPhaseForVersion(version)).name()));
	}

	@Override
	public void setPhase(Phase phase) {
		getPreferences().put(AppRegistry.KEY_PHASE, phase.name());

	}
}
