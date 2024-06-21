package com.sshtools.jaul;

import static java.lang.Thread.sleep;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ProcessBuilder.Redirect;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.install4j.api.Util;
import com.install4j.api.context.ProgressInterface;
import com.install4j.api.context.RemoteCallable;

@SuppressWarnings("serial")
public final class CallInstall implements RemoteCallable {
	private final ProgressInterface prg;
	private String urlText;
	private String installPath;
	private boolean unattended;
	
	public CallInstall() {
		prg = null;
	}

	CallInstall(ProgressInterface prg, String urlText, String installPath, boolean unattended) {
		this.prg = prg;
		this.urlText = urlText;
		this.installPath = installPath;
		this.unattended = unattended;
	}

	@Override
	public Serializable execute() {
		try {
			installApp( installPath, unattended, prg, urlText);
		} catch (IOException | InterruptedException e) {
			throw new IllegalStateException("Failed elevated action.", e);
		}
		return "";
	}


	static void installApp(String installDirPath, boolean unattended, ProgressInterface progress, String urlText)
			throws IOException, FileNotFoundException, InterruptedException {
		var url = new URL(urlText);
		var installDir = installDirPath == null ? null : new File(installDirPath);
		var filename = url.getPath();
		var idx = filename.lastIndexOf('/');
		if (idx != -1) {
			filename = filename.substring(idx + 1);
		}
		var outFile = new File(installDir == null ? new File(System.getProperty("user.dir")) : installDir, filename);

		/* Download the installer file */
		var inConx = url.openConnection();
		var sz = inConx.getContentLength();
		if(outFile.exists() && outFile.length() == sz) {
			return;
		}
		try (var out = new FileOutputStream(outFile)) {
			try(var inStream = inConx.getInputStream()) {
				var buf = new byte[65536];
				if(progress != null)
					progress.setStatusMessage("Downloading " + filename);
				try (var in = inStream) {
					int r;
					int t = 0;
					while ((r = in.read(buf)) != -1) {
						out.write(buf, 0, r);
						t += r;
						if(progress != null)
							progress.setPercentCompleted((int) (((double) t / (double) sz) * 100.0));
					}
					in.transferTo(out);
				}
			}
		}

		outFile.setExecutable(true, false);

		if (Util.isMacOS()) {
			/* Mac is special */
			var volId = "v" + System.currentTimeMillis();
			var volPath = "/Volumes/" + volId;
			var exec = outFile.toString();
			try {
				if(progress != null) {
					progress.setStatusMessage("Mounting archive");
				}
				var p = new ProcessBuilder("hdiutil", "mount", "-mountpoint", volPath, exec)
						.redirectError(Redirect.INHERIT).redirectInput(Redirect.INHERIT)
						.redirectOutput(Redirect.INHERIT).start();
				if (p.waitFor() != 0) {
					throw new IOException("Installer exited with error code " + p.exitValue());
				}

				if(progress != null) {
					progress.setStatusMessage("Executing installer");
				}
				var dir = new File(volPath).listFiles((f) -> !f.isHidden());
				if (dir == null || dir.length == 0)
					throw new IOException("No installer found in volume " + volPath);

				var inst = new File(new File(new File(dir[0], "Contents"), "MacOS"), "JavaApplicationStub");
				runInstallerExecutable(inst, installDir, unattended, progress);
			} finally {
				if(progress != null) {
					progress.setStatusMessage("Unmounting archive");
				}
				sleep(1000);
				new ProcessBuilder("hdiutil", "eject", volPath).redirectError(Redirect.INHERIT)
						.redirectInput(Redirect.INHERIT).redirectOutput(Redirect.INHERIT).start().waitFor();
			}
		} else {
			if(progress != null) {
				progress.setStatusMessage("Executing installer");
			}
			runInstallerExecutable(outFile, installDir, unattended, progress);
		}
	}

	private static void runInstallerExecutable(File exec, File installDir, boolean unattended, ProgressInterface progress)
			throws IOException, InterruptedException {
		var args = new ArrayList<String>();
		args.add(exec.toString());
		if (unattended) {
			args.add("-q");
			args.add("-wait");
			args.add("20");
		} else {
			args.add("-g");
		}
		if (installDir != null) {
			/* Having an install dir means this is actually an upgrade */
			args.add("-dir");
			args.add(installDir.getAbsolutePath().toString());
			if (unattended) {
				args.add("-alerts");
				args.add("-splash");
				args.add("Installing");
			}
		}
		addStandardInstall4JArguments(args);
		if(progress != null)
			progress.setStatusMessage("Running companion installer");
		var p = new ProcessBuilder(args).redirectError(Redirect.INHERIT).redirectInput(Redirect.INHERIT)
				.redirectOutput(Redirect.INHERIT).start();
		if (p.waitFor() != 0) {
			throw new IOException("Installer exited with error code " + p.exitValue());
		}
	}

	static void addStandardInstall4JArguments(List<String> args) {
		args.add("-Vjaul.launchApp=true");
//		args.add("-Dinstall4j.logToStderr=true");
//		args.add("-Dinstall4j.debug=true");

		/*
		 * I get corruption in both swing and javafx when running in a Windows VM guest,
		 * Linux host and 3D acceleration enabled. This is to pass on the system
		 * properties that "fix" the corruption, helpful for development.
		 */
		if ("sw".equals(System.getProperty("prism.order"))) {
			args.add("-Dprism.order=sw");
			args.add("-Dsun.java2d.noddraw=treu");
		}
	}
}