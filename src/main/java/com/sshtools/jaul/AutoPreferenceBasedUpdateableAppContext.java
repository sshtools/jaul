package com.sshtools.jaul;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.prefs.Preferences;

public class AutoPreferenceBasedUpdateableAppContext extends PreferenceBasedUpdateableAppContext {

	private boolean automaticUpdatesDefault;

	public AutoPreferenceBasedUpdateableAppContext(Preferences preferences, Optional<Phase> defaultPhase,
			String version, ScheduledExecutorService scheduler, boolean automaticUpdatesDefault) {
		super(preferences, defaultPhase, version, Optional.ofNullable(scheduler));
		this.automaticUpdatesDefault = automaticUpdatesDefault;
	}

	@Override
	public boolean isAutomaticUpdates() {
		return getPreferences().getBoolean("automaticUpdates", automaticUpdatesDefault);
	}

	@Override
	public void setAutomaticUpdates(boolean automaticUpdates) {
		getPreferences().putBoolean("automaticUpdates", automaticUpdates);
	}

	@Override
	public void setUpdatesDeferredUntil(long timeMs) {
		getPreferences().putLong("updatesDeferredUntil", timeMs);
	}

	@Override
	public long getUpdatesDeferredUntil() {
		return getPreferences().getLong("updatesDeferredUntil", 0);
	}

}
