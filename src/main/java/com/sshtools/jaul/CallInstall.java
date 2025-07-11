package com.sshtools.jaul;

import static java.lang.Thread.sleep;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.ProcessBuilder.Redirect;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.install4j.api.Util;
import com.install4j.api.context.ProgressInterface;
import com.install4j.api.context.RemoteCallable;
import com.install4j.runtime.installer.helper.Logger;

@SuppressWarnings("serial")
public final class CallInstall implements RemoteCallable {
	
	private final ProgressInterface prg;
	private String urlText;
	private String installPath;
	private boolean unattended;
	private File installerFile;
	private boolean gui;
	private boolean debug;
	
	public CallInstall() {
		prg = null;
	}

	public CallInstall(ProgressInterface prg, String urlText, String installPath, boolean unattended, File installerFile, boolean gui, boolean debug) {
		this.prg = prg;
		this.debug = debug;
		this.urlText = urlText;
		this.installPath = installPath;
		this.unattended = unattended;
		this.installerFile = installerFile;
		this.gui = gui;
	}

	@Override
	public Serializable execute() {
		try {
			if(installerFile == null)
				downloadAndInstallApp( installPath, unattended, prg, urlText, gui);
			else
				runInstaller(installPath, unattended, prg, installerFile, gui);
		} catch (IOException | InterruptedException e) {
			throw new IllegalStateException("Failed elevated action.", e);
		}
		return "";
	}
	
	private void debug(String text) {
		if(debug) {
			Logger.getInstance().info(this, text);
		}
	}
	
	private void downloadAndInstallApp(String installDirPath, boolean unattended, ProgressInterface progress, String urlText, boolean gui)
			throws IOException, FileNotFoundException, InterruptedException {
		var url = new URL(urlText);
		
		debug("Download from " + url);
		debug("Install dir path is " + (installDirPath == null ? "<unset>" : installDirPath));
		
		var installDir = installDirPath == null ? null : new File(installDirPath);
		var filename = url.getPath();
		var idx = filename.lastIndexOf('/');
		if (idx != -1) {
			filename = filename.substring(idx + 1);
		}
		
		var outDir = installDir == null ? new File(System.getProperty("user.dir")) : installDir;
		File outFile;
		if(outDir.canWrite()) {
			outFile = new File(outDir, filename);
		}
		else {
			outFile = File.createTempFile("jaul", filename);
			debug("Cannot write to " + outDir + ", so using tmpfile at " + outFile);
		}
		debug("Will save to " + outFile);

		/* Download the installer file */
		var inConx = url.openConnection();
		var sz = inConx.getContentLength();
		if(!outFile.exists() || outFile.length() != sz) {
			debug("Output does not exist or differs in size, so downloading.");
			var buf = new byte[65536];
			try (var out = new FileOutputStream(outFile)) {
				try(var inStream = inConx.getInputStream()) {
					if(progress != null)
						progress.setStatusMessage("Downloading " + filename);
					int r;
					int t = 0;
					while ((r = inStream.read(buf)) != -1) {
						debug("Read " + r + " bytes");
						out.write(buf, 0, r);
						debug("Written " + r + " bytes");
						t += r;
						if(progress != null)
							progress.setPercentCompleted((int) (((double) t / (double) sz) * 100.0));
					}
				}
			}
			debug("Downloaded.");
		}

		runInstaller(installDirPath, unattended, progress, outFile, gui);
	}

	private void runInstaller(String installDirPath, boolean unattended, ProgressInterface progress, File outFile, boolean gui)
			throws IOException, InterruptedException {
		
		var installDir = installDirPath == null ? null : new File(installDirPath);
		outFile.setExecutable(true, false);

		debug("Running installer at " + outFile);

		if (Util.isMacOS()) {
			debug("Detected Mac OS, jumping through hoops");
			
			/* Mac is special */
			var volId = "v" + System.currentTimeMillis();
			var volPath = "/Volumes/" + volId;
			var exec = outFile.toString();
			try {
				
				debug("Mounting volume " + volPath + " to archive " + exec);
				if(progress != null) {
					progress.setStatusMessage("Mounting archive");
				}
				var cmds = Arrays.asList("/usr/bin/hdiutil", "mount", "-mountpoint", volPath, exec);
				debug("Command: " + String.join(" ", cmds));

				if(new File("/tmp/debug-installer").exists()) {
					try(var out = new PrintWriter(new FileOutputStream(new File("/tmp/mount-installer.sh")), true)) {
						out.println(String.join(" ", cmds));
					}
				}
				
				var p = new ProcessBuilder(cmds)
						.redirectError(Redirect.INHERIT).redirectInput(Redirect.INHERIT)
						.redirectOutput(Redirect.INHERIT).start();
				if (p.waitFor() != 0) {
					debug("Mount failed with " + p.exitValue());
					throw new IOException("Installer exited with error code " + p.exitValue());
				}
				
				debug("Mounted archive, calling installer");
				if(progress != null) {
					progress.setStatusMessage("Executing installer");
				}
				var dir = new File(volPath).listFiles((f) -> !f.isHidden());
				if (dir == null || dir.length == 0)
					throw new IOException("No installer found in volume " + volPath);

				var inst = new File(new File(new File(dir[0], "Contents"), "MacOS"), "JavaApplicationStub");
				runInstallerExecutable(inst, installDir, unattended, progress, gui);
			} finally {
				debug("Ejecting installer");
				if(new File("/tmp/debug-installer").exists()) {
					debug("Sleeping for 5 mins");
					Thread.sleep(Duration.ofMinutes(5).toMillis());
				}
				if(progress != null) {
					progress.setStatusMessage("Unmounting archive");
				}
				sleep(1000);
				new ProcessBuilder("hdiutil", "eject", volPath).redirectError(Redirect.INHERIT)
						.redirectInput(Redirect.INHERIT).redirectOutput(Redirect.INHERIT).start().waitFor();

				debug("Ejected installer");
			}
		} else {
			if(progress != null) {
				progress.setStatusMessage("Executing installer");
			}
			runInstallerExecutable(outFile, installDir, unattended, progress, gui);
		}
	}

	private void runInstallerExecutable(File exec, File installDir, boolean unattended, ProgressInterface progress, boolean gui)
			throws IOException, InterruptedException {

		debug("Running installer " + exec + " unattended: " + unattended);
		
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
			if (unattended && gui) {
				args.add("-alerts");
				args.add("-splash");
				args.add("Installing");
			}
		}
		addStandardInstall4JArguments(args);
		if(progress != null)
			progress.setStatusMessage("Running companion installer");
		
		debug("Full command is : " + String.join(" ", args));

		if(new File("/tmp/debug-installer").exists()) {
			try(var out = new PrintWriter(new FileOutputStream(new File("/tmp/run-installer.sh")), true)) {
				out.println(String.join(" ", args));
			}
		}
		
		var p = new ProcessBuilder(args);
		p.redirectErrorStream(true);
		var prc = p.start();
		var out = new ByteArrayOutputStream();
		try(InputStream in = prc.getInputStream()) {
				in.transferTo(out);
		}
		debug("Installer output: " + new String(out.toByteArray()));
		if (prc.waitFor() != 0) {
			debug("Installed Exited with non zero code " + prc.exitValue());
			throw new IOException("Installer exited with error code " + prc.exitValue());
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