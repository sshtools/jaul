package com.sshtools.jaul;

import java.io.IOException;

public interface UpdateService {

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
	
	void addDownloadListener(DownloadListener listener);

	void removeDownloadListener(DownloadListener listener);

	boolean isNeedsUpdating();

	boolean isUpdating();

	Phase[] getPhases();

	String getAvailableVersion();

	void deferUpdate();

	boolean isUpdatesEnabled();

	void checkForUpdate() throws IOException;

	void update() throws IOException;

	void shutdown();
	
	default void rescheduleCheck() {
	}
}
