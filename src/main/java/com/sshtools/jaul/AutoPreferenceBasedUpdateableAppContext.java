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
		return getPreferences().getBoolean(AppRegistry.KEY_AUTOMATIC_UPDATES, automaticUpdatesDefault);
	}

	@Override
	public void setAutomaticUpdates(boolean automaticUpdates) {
		getPreferences().putBoolean(AppRegistry.KEY_AUTOMATIC_UPDATES, automaticUpdates);
	}

	@Override
	public void setUpdatesDeferredUntil(long timeMs) {
		getPreferences().putLong(AppRegistry.KEY_DEFER, timeMs);
	}

	@Override
	public long getUpdatesDeferredUntil() {
		return getPreferences().getLong(AppRegistry.KEY_DEFER, 0);
	}

}
