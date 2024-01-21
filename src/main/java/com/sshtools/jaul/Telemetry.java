package com.sshtools.jaul;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.UUID;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.jaul.AppRegistry.App;
import com.sshtools.jaul.TelemetryEvent.TelemetryEventBuilder;

public final class Telemetry {
	static Logger log = LoggerFactory.getLogger(Telemetry.class);

	final static String RUN_ID = UUID.randomUUID().toString();
	final static Object LOCK = new Object();

	public final static String KEY_USER_ID = "userId";

	static {
		var node = Preferences.userNodeForPackage(AppRegistry.class);
		var userId = node.get(KEY_USER_ID, "");
		if (userId.equals("")) {
			node.put(KEY_USER_ID, UUID.randomUUID().toString());
		}
	}

	public final static class TelemetryBuilder {
		private App app;

		public static TelemetryBuilder builder() {
			return new TelemetryBuilder();
		}

		public TelemetryBuilder withApp(App app) {
			this.app = app;
			return this;
		}

		public Telemetry build() {
			return new Telemetry(this);
		}
	}

	private final App app;

	Telemetry(TelemetryBuilder telemetryBuilder) {
		if (telemetryBuilder.app == null)
			throw new IllegalStateException("App must be set.");
		this.app = telemetryBuilder.app;
	}

	public TelemetryEventBuilder builder() {
		return new TelemetryEventBuilder().withRunId(RUN_ID).withAppId(app.getId()).withScope(app.getScope());
	}

	public void event(TelemetryEvent build) {
		try {
			synchronized (LOCK) {
				var datPath = resolvePath();
				var exists = Files.exists(datPath);
				try (var channel = FileChannel.open(datPath, StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
					try (var lock = channel.lock()) {
						var out = new ObjectOutputStream(Channels.newOutputStream(channel)) {
							@Override
							protected void writeStreamHeader() throws IOException {
								if (exists)
									reset();
								else
									super.writeStreamHeader();
							}
						};
						out.writeObject(build);
						out.flush();
					}
				}
			}
		} catch (Exception ioe) {
			log.warn("Failed to write telemetry event.", ioe);
		}
	}

	public final String getUserId() {
		return Preferences.userNodeForPackage(AppRegistry.class).get(KEY_USER_ID, "");
	}

	public Optional<Thread> sendNow() {
		synchronized (LOCK) {
			try {
				var path = resolvePath();
				if (Files.exists(path)) {
					var tf = Files.createTempFile("telem", ".tmp");
					Files.move(path, tf, StandardCopyOption.REPLACE_EXISTING);
					var t = new Thread(() -> {
						try {
							// TODO post somewhere
						} finally {
							try {
								Files.deleteIfExists(tf);
							} catch (IOException e) {
							}
						}
					}, "TelemetrySend");
					t.start();
					return Optional.of(t);
				} else {
					log.info("No telemetry data to send.");
					return Optional.empty();
				}
			} catch (Exception e) {
				log.warn("Failed to send prepare telemetry data.", e);
				return Optional.empty();
			}
		}
	}

	Path resolvePath() throws IOException {
		var jaulHome = AppRegistry.getUserData();
		Files.createDirectories(jaulHome);
		var datPath = jaulHome.resolve("telemetry.dat");
		return datPath;
	}

}
