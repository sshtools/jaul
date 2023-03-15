package com.sshtools.jaul;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractUpdateService implements UpdateService {

	static Logger log = LoggerFactory.getLogger(AbstractUpdateService.class);

	private List<DownloadListener> downloadListeners = new ArrayList<>();
	private boolean updating;
	private String availableVersion;
	private ScheduledFuture<?> checkTask;
	private long deferUntil;
	private final UpdateableAppContext context;
	private Optional<Consumer<String>> onAvailableVersion = Optional.empty();
	private Optional<Consumer<Boolean>> onBusy = Optional.empty();
	private final String currentVersion;
	private boolean checkOnly;
	
	protected AbstractUpdateService(UpdateableAppContext context, String currentVersion) {
		this.context = context;
		this.currentVersion = currentVersion;
	}

	public final String getCurrentVersion() {
		return currentVersion;
	}

	@Override
	public final void setOnAvailableVersion(Consumer<String> onAvailableVersion) {
		this.onAvailableVersion = Optional.of(onAvailableVersion);
	}

	@Override
	public final void setOnBusy(Consumer<Boolean> onBusy) {
		this.onBusy = Optional.of(onBusy);
	}

	@Override
	public final void addDownloadListener(DownloadListener listener) {
		downloadListeners.add(listener);
	}

	@Override
	public final UpdateableAppContext getContext() {
		return context;
	}

	@Override
	public final void checkForUpdate() throws IOException {
		setDeferUntil(0);
		log.info("Checking for updates ...");
		update(true);
	}

	@Override
	public final void deferUpdate() {
		setAvailableVersion(null);
		configDeferUpdate();
		context.setUpdatesDeferredUntil(deferUntil);
	}

	@Override
	public String getAvailableVersion() {
		return availableVersion;
	}

	@Override
	public final Phase[] getPhases() {
		return Arrays.asList(Phase.values()).stream()
				.filter(p -> p != Phase.CONTINUOUS || p == context.getPhase() || (p == Phase.CONTINUOUS
						&& (  ArtifactVersion.isDeveloperWorkspace() || Boolean.getBoolean("jaul.continuous") || Boolean.getBoolean("jadaptive.development"))))
				.collect(Collectors.toList()).toArray(new Phase[0]);
	}

	@Override
	public final boolean isNeedsUpdating() {
		return getAvailableVersion() != null;
	}

	@Override
	public boolean isUpdatesEnabled() {
		return "false".equals(System.getProperty("hypersocket.development.noUpdates", "false"));
	}

	@Override
	public boolean isUpdating() {
		return updating;
	}
	
	@Override
	public final void removeDownloadListener(DownloadListener listener) {
		downloadListeners.remove(listener);
	}

	@Override
	public final void rescheduleCheck() {
		cancelTask();
		deferUntil = context.getUpdatesDeferredUntil();
		rescheduleCheck(TimeUnit.SECONDS.toMillis(6));
	}
	
	@Override
	public void shutdown() {
	}
	
	@Override
	public final void update() throws IOException {
		if (!isNeedsUpdating()) {
			throw new IllegalStateException("An update is not required.");
		}
		update(false);
	}

	@Override
	public boolean isCheckOnly() {
		return checkOnly;
	}

	protected final void cancelTask() {
		if (checkTask != null) {
			checkTask.cancel(false);
		}
	}

	protected final void configDeferUpdate() {
		long day = TimeUnit.DAYS.toMillis(1);
		long nowDay = (System.currentTimeMillis() / day) * day;
		long when = nowDay + day + TimeUnit.HOURS.toMillis(12)
				+ (long) (Math.random() * 3.0d * (double) TimeUnit.HOURS.toMillis(3));
		setDeferUntil(when);
		try {
			rescheduleCheck(0);
			log.info("Deferred update for " + DateFormat.getDateTimeInstance().format(new Date(when)) + " days");
		}
		catch(UnsupportedOperationException uoe) {
			log.info("No scheduler, update check will not occur this runtime.");
		}
	}

	protected abstract String doUpdate(boolean check) throws IOException;

	protected final void fireDownload(DownloadEvent event) {
		for (int i = downloadListeners.size() - 1; i >= 0; i--) {
			downloadListeners.get(i).downloadEvent(event);
		}
	}

	protected final long getDeferUntil() {
		return deferUntil;
	}

	protected final void rescheduleCheck(long nonDeferredDelay) {
		cancelTask();
		long defer = getDeferUntil();
		long when = defer == 0 ? 0 : defer - System.currentTimeMillis();
		if (when > 0) {
			log.info(String.format("Scheduling next check for %s",
					DateFormat.getDateTimeInstance().format(new Date(defer))));
			checkTask = context.getScheduler().schedule(() -> timedCheck(), when, TimeUnit.MILLISECONDS);
		} else {
			if (nonDeferredDelay == 0) {
				configDeferUpdate();
			} else
				checkTask = context.getScheduler().schedule(() -> timedCheck(), nonDeferredDelay,
						TimeUnit.MILLISECONDS);
		}
	}

	protected void setAvailableVersion(String availableVersion) {
		this.availableVersion = availableVersion;
		onAvailableVersion.ifPresent(v -> v.accept(availableVersion));
	}

	protected final void setDeferUntil(long deferUntil) {
		this.deferUntil = deferUntil;
	}

	protected void setUpdating(boolean updating, boolean checkOnly) {
		this.checkOnly = checkOnly;
		this.updating = updating;
		onBusy.ifPresent(b -> b.accept(updating));
	}

	protected final void timedCheck() {
		try {
			update(true);
		} catch (Exception e) {
			log.error("Failed to automatically check for updates.", e);
		} finally {
			rescheduleCheck(0);
		}
	}

	protected final void update(boolean check) throws IOException {
		if(isUpdating())
			throw new IllegalStateException("Already updating.");
		if (!isUpdatesEnabled()) {
			log.info("Updates disabled.");
			setAvailableVersion(null);
		} else {
			long defer = getDeferUntil();
			if (!check || defer == 0 || System.currentTimeMillis() >= defer) {
				setDeferUntil(0);
				setUpdating(true, check);
				try {
					setAvailableVersion(doUpdate(check));
				} finally {
					setUpdating(false, check);
					if (check) {
						rescheduleCheck(0);
					}
				}
			} else {
				log.info(String.format("Updates deferred until %s",
						DateFormat.getDateTimeInstance().format(new Date(defer))));
			}
		}
	}

}
