package com.sshtools.jaul;

import java.io.IOException;
import java.util.function.Consumer;

public class NoUpdateService implements UpdateService {


	private UpdateableAppContext context;

	public NoUpdateService(UpdateableAppContext context) {
		super();
		this.context = context;
	}

	@Override
	public void addDownloadListener(DownloadListener listener) {
	}

	@Override
	public void removeDownloadListener(DownloadListener listener) {
	}

	@Override
	public Phase[] getPhases() {
		return new Phase[0];
	}

	@Override
	public void deferUpdate() {
	}

	@Override
	public void checkForUpdate() throws IOException {
	}

	@Override
	public void update() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void shutdown() {
	}

	@Override
	public boolean isUpdatesEnabled() {
		return false;
	}

	@Override
	public boolean isNeedsUpdating() {
		return false;
	}

	@Override
	public boolean isUpdating() {
		return false;
	}

	@Override
	public String getAvailableVersion() {
		return null;
	}

	@Override
	public UpdateableAppContext getContext() {
		return context;
	}

	@Override
	public void setOnAvailableVersion(Consumer<String> onAvailableVersion) {
	}

}
