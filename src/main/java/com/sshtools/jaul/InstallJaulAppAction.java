package com.sshtools.jaul;

import static java.lang.Thread.sleep;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ProcessBuilder.Redirect;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.install4j.api.Util;
import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.RemoteCallable;
import com.install4j.api.context.UserCanceledException;
import com.sshtools.jaul.UpdateDescriptor.MediaKey;

@SuppressWarnings("serial")
public class InstallJaulAppAction extends AbstractInstallAction {

	private String updatesXmlLocation;
	private File installDir;
	private boolean unattended = true;

	@Override
	public boolean install(InstallerContext context) throws UserCanceledException {
		try {
			var desc = UpdateDescriptor.get(URI.create(updatesXmlLocation));
			var key = MediaKey.get();
			var mediaOr = desc.find(key);
			if(mediaOr.isPresent()) {
				var media = mediaOr.get();
				var url = media.url();
				context.runElevated(new RemoteCallable() {
					@Override
					public Serializable execute() {
						try {
							installApp(context, installDir, unattended, url);
						} catch (IOException | InterruptedException e) {
							throw new IllegalStateException("Failed elevated action.", e);
						}
						return "";
					}
				}, true);
				return true;
			}
			else {
				throw new IOException(MessageFormat.format("Did not find any media for {0}", key));
			}
		} catch (Exception e) {
			e.printStackTrace();
			context.getProgressInterface().showFailure("Failed to install companion application. " + e.getMessage());
		}
		return false;
	}

	private static void installApp(InstallerContext context, File installDir, boolean unattended, URL url)
			throws IOException, FileNotFoundException, InterruptedException {
		var filename = url.getPath();
		var idx = filename.lastIndexOf('/');
		if (idx != -1) {
			filename = filename.substring(idx + 1);
		}
		var outFile = new File(context.getInstallationDirectory(), filename);
		
		/* Download the installer file */
		try (var out = new FileOutputStream(outFile)) {
			var inConx = url.openConnection();
			var inStream = inConx.getInputStream();
			var buf = new byte[65536];
			var sz = inConx.getContentLength();
			context.getProgressInterface().setStatusMessage("Downloading " + filename);
			try (var in = inStream) {
				int r;
				int t = 0;
				while ((r = in.read(buf)) != -1) {
					out.write(buf, 0, r);
					t += r;
					context.getProgressInterface().setPercentCompleted((int) (((double) t / (double) sz) * 100.0));
				}
				in.transferTo(out);
			}
		}
		
		outFile.setExecutable(true, false);

		if (Util.isMacOS()) {
			/* Mac is special */
			var volId = "v" + System.currentTimeMillis();
			var volPath = "/Volumes/" + volId;
			var exec = outFile.toString();
			try {
				context.getProgressInterface().setStatusMessage("Mounting archive");
				var p = new ProcessBuilder("hdiutil", "mount", "-mountpoint", volPath, exec)
						.redirectError(Redirect.INHERIT).redirectInput(Redirect.INHERIT)
						.redirectOutput(Redirect.INHERIT).start();
				if (p.waitFor() != 0) {
					throw new IOException("Installer exited with error code " + p.exitValue());
				}

				context.getProgressInterface().setStatusMessage("Executing installer");
				var dir = new File(volPath).listFiles((f) -> !f.isHidden());
				if (dir == null || dir.length == 0)
					throw new IOException("No installer found in volume " + volPath);

				var inst = new File(new File(new File(dir[0], "Contents"), "MacOS"), "JavaApplicationStub");
				runInstallerExecutable(inst, installDir, unattended, context);
			} finally {
				context.getProgressInterface().setStatusMessage("Unmounting archive");
				sleep(1000);
				new ProcessBuilder("hdiutil", "eject", volPath).redirectError(Redirect.INHERIT)
						.redirectInput(Redirect.INHERIT).redirectOutput(Redirect.INHERIT).start().waitFor();
			}
		} else {
			context.getProgressInterface().setStatusMessage("Executing installer");
			runInstallerExecutable(outFile, installDir, unattended, context);
		}
	}

	public File getInstallDir() {
		return installDir;
	}

	public void setInstallDir(File installDir) {
		this.installDir = installDir;
	}

	public boolean isUnattended() {
		return unattended;
	}

	public void setUnattended(boolean unattended) {
		this.unattended = unattended;
	}

	public String getUpdatesXmlLocation() {
		return updatesXmlLocation;
	}

	public void setUpdatesXmlLocation(String updatesXmlLocation) {
		this.updatesXmlLocation = updatesXmlLocation;
	}

	private static void runInstallerExecutable(File exec, File installDir, boolean unattended, InstallerContext context) throws IOException, InterruptedException {
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
		context.getProgressInterface().setStatusMessage("Running companion installer");
		var p = new ProcessBuilder(args).redirectError(Redirect.INHERIT).redirectInput(Redirect.INHERIT)
				.redirectOutput(Redirect.INHERIT).start();
		if (p.waitFor() != 0) {
			throw new IOException("Installer exited with error code " + p.exitValue());
		}
	}

	static void addStandardInstall4JArguments(List<String> args) {
		args.add("-Vjaul.launchApp=true");
		args.add("-Dinstall4j.logToStderr=true");
		args.add("-Dinstall4j.debug=true");

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
