package com.sshtools.jaul;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.install4j.api.Util;
import com.install4j.api.update.UpdateDescriptorEntry;

public final class HybridInstall4JUpdater extends Install4JUpdater {

	public final static class HybridInstall4JUpdaterBuilder
			extends AbstractInstall4JUpdaterBuilder<HybridInstall4JUpdaterBuilder, HybridInstall4JUpdater> {

		private Optional<Function<String[], Integer>> commandExecutor = Optional.empty();
		private boolean unattended = false;
		private Optional<Path> installDir = Optional.empty();

		public HybridInstall4JUpdaterBuilder withCommandExecutor(Function<String[], Integer> commandExector) {
			this.commandExecutor = Optional.of(commandExector);
			return this;
		}

		public HybridInstall4JUpdaterBuilder withUnattended() {
			return withUnattended(true);
		}

		public HybridInstall4JUpdaterBuilder withInstallDir(Path installDir) {
			this.installDir = Optional.of(installDir);
			return this;
		}

		public HybridInstall4JUpdaterBuilder withUnattended(boolean unattended) {
			this.unattended = unattended;
			return this;
		}

		public static HybridInstall4JUpdaterBuilder builder() {
			return new HybridInstall4JUpdaterBuilder();
		}

		public HybridInstall4JUpdater build() {
			return new HybridInstall4JUpdater(this);
		}

	}

	static Logger log = LoggerFactory.getLogger(Install4JUpdateService.class);

	private final Optional<Function<String[], Integer>> commandExecutor;
	private final boolean unattended;
	private final Optional<Path> installDir;

	private HybridInstall4JUpdater(HybridInstall4JUpdaterBuilder builder) {
		super(builder);
		this.commandExecutor = builder.commandExecutor;
		this.unattended = builder.unattended;
		this.installDir = builder.installDir;
	}

	private int runInstallerExecutable(boolean unattended, Path exec, Optional<Path> installDir)
			throws IOException, InterruptedException {
		var args = new ArrayList<String>();
		args.add(exec.toString());
		if (unattended) {
			args.add("-q");
			args.add("-wait");
			args.add("20");
			if(consoleMode) {
				args.add("-c");
			}
		} 

		if(consoleMode) {
			args.add("-c");
		}
		
		if (installDir.isPresent()) {
			/* Having an install dir means this is actually an upgrade */
			args.add("-dir");
			args.add(installDir.get().toString());
			if (unattended) {
				args.add("-alerts");
				args.add("-splash");
				args.add("Installing");
			}
		}
		log.info("Running installer executable. '{}'", String.join(" ", args));
		if (commandExecutor.isPresent()) {
			return commandExecutor.get().apply(args.toArray(new String[0]));
		} else {
			return new ProcessBuilder(args).redirectError(Redirect.INHERIT).redirectInput(Redirect.INHERIT)
					.redirectOutput(Redirect.INHERIT).start().waitFor();
		}
	}

	@Override
	protected void downloadAndExecuteUpdater(UpdateDescriptorEntry best) throws IOException {
		var downloads = AppRegistry.getUserData().resolve("downloads");
		if (!Files.exists(downloads)) {
			Files.createDirectories(downloads);
		}

		var listener = progressListenerFactory.map(f -> f.get());

		/* Download */
		listener.ifPresent(l -> l.indeterminateProgress(true));

		var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
		var fn = Paths.get(best.getURL().getFile()).getFileName();
		var outFile = downloads.resolve(fn);
		try {
			var request = HttpRequest.newBuilder().uri(best.getURL().toURI()).timeout(Duration.ofMinutes(2)).GET()
					.build();

			listener.ifPresent(l -> l.statusMessage(MessageFormat.format("Locating {}", fn)));
			listener.ifPresent(l -> l.detailMessage(best.getURL().toString()));

			var response = client.send(request, BodyHandlers.ofInputStream());

			if (response.statusCode() == 200) {

				var sz = response.headers().firstValueAsLong("Content-Size").orElse(0);

				listener.ifPresent(l -> l.indeterminateProgress(sz > 0));
				listener.ifPresent(l -> l.statusMessage(MessageFormat.format("Downloading {}", fn)));
				try (var out = Files.newOutputStream(outFile)) {
					try (var in = response.body()) {
						var buf = new byte[65536];
						int r;
						long t = 0;
						while ((r = in.read(buf)) != -1) {
							out.write(buf, 0, r);
							t += r;
							if (sz > 0 && listener.isPresent()) {
								listener.get().percentCompleted((int) (((double) t / (double) sz) * (double) 100));
							}
						}

						in.transferTo(out);
					}
				}
				listener.ifPresent(l -> l.statusMessage(MessageFormat.format("Completed downloading {}", fn)));

			} else {
				throw new IOException("Unexpected response code for " + best.getURL() + ". " + response.statusCode());
			}
		} catch (IOException ioe) {
			listener.ifPresent(
					l -> l.statusMessage(MessageFormat.format("Error downloading {0}. {1}", fn, ioe.getMessage())));
			throw ioe;
		} catch (InterruptedException | URISyntaxException ie) {
			throw new IllegalStateException(ie);
		}

		outFile.toFile().setExecutable(true, false);

		/* Execute */
		int ret = 0;
		try {
			if (Util.isMacOS()) {
				/* Mac is special */
				var volId = "v" + System.currentTimeMillis();
				var volPath = "/Volumes/" + volId;
				var exec = outFile.toString();
				try {
					listener.ifPresent(l -> l.statusMessage("Mounting archive"));
					var p = new ProcessBuilder("hdiutil", "mount", "-mountpoint", volPath, exec)
							.redirectError(Redirect.INHERIT).redirectInput(Redirect.INHERIT)
							.redirectOutput(Redirect.INHERIT).start();
					if (p.waitFor() != 0) {
						throw new IOException("Installer exited with error code " + p.exitValue());
					}

					listener.ifPresent(l -> l.statusMessage("Executing installer"));
					var dir = new File(volPath).listFiles((f) -> !f.isHidden());
					if (dir == null || dir.length == 0)
						throw new IOException("No installer found in volume " + volPath);

					var inst = new File(new File(new File(dir[0], "Contents"), "MacOS"), "JavaApplicationStub");
					ret = runInstallerExecutable(unattended, inst.toPath(), installDir);
				} finally {
					listener.ifPresent(l -> l.statusMessage("Unmounting archive"));
					Thread.sleep(1000);
					ret = 					new ProcessBuilder("hdiutil", "eject", volPath).redirectError(Redirect.INHERIT)
							.redirectInput(Redirect.INHERIT).redirectOutput(Redirect.INHERIT).start().waitFor();
				}
			} else {
				listener.ifPresent(l -> l.statusMessage("Executing installer"));
				ret = runInstallerExecutable(unattended, outFile, installDir);
			}
		} catch (InterruptedException ie) {
			throw new IllegalStateException(ie);
		}

		/* Exit */
		if(onExit.isPresent()) 
			onExit.get().accept(ret);
	}


}
