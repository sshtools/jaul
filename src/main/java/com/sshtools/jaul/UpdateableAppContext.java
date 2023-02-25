package com.sshtools.jaul;

import java.util.concurrent.ScheduledExecutorService;

public interface UpdateableAppContext {
	
	ScheduledExecutorService getScheduler();
	
	boolean isAutomaticUpdates();
	
	void setAutomaticUpdates(boolean automaticUpdates);
	
	Phase getPhase();
	
	void setPhase(Phase phase);

	long getUpdatesDeferredUntil();

	void setUpdatesDeferredUntil(long timeMs);
}
