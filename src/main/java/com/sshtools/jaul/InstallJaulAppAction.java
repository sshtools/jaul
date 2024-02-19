package com.sshtools.jaul;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Optional;

import com.install4j.api.Util;
import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;
import com.sshtools.jaul.UpdateDescriptor.MediaKey;

@SuppressWarnings("serial")
public class InstallJaulAppAction extends AbstractInstallAction {

	private String updatesXmlLocation;
	private String jaulAppId;
	private File installDir;
	private boolean unattended = true;

	@Override
	public boolean install(InstallerContext context) throws UserCanceledException {
		try {
			/* First try to see if the app is already installed, and get what version
			 * it is at if it is.
			 */
			Optional<String> installedVersion = Optional.empty();
			
			try {
				var app= AppRegistry.get().get(jaulAppId);
				var appDef  = new LocalAppDef(app);
				com.install4j.runtime.installer.helper.Logger
				.getInstance().info(this, MessageFormat.format("{0} is installed, version {1}.", jaulAppId, appDef.getVersion()));
				installedVersion = Optional.of(appDef.getVersion());
			}
			catch(IllegalStateException iae) {
				iae.printStackTrace();
				/* Not installed */
				com.install4j.runtime.installer.helper.Logger
				.getInstance().info(this, MessageFormat.format("{0} is not installed, will try to download.", jaulAppId));
			}
			
			var desc = UpdateDescriptor.get(URI.create(updatesXmlLocation));
			var key = MediaKey.get();
			var mediaOr = desc.find(key);
			if(mediaOr.isPresent()) {
				var media = mediaOr.get();
				if(installedVersion.isEmpty() || !installedVersion.get().equals(media.version())) {
					var url = media.url();
					if(Util.hasFullAdminRights()) {
						new CallInstall(context.getProgressInterface(), url.toExternalForm(), installDir == null ? null : installDir.getAbsolutePath().toString(), unattended).execute();
					}
					else {
						context.runElevated(new CallInstall(null, url.toExternalForm(), installDir == null ? null : installDir.getAbsolutePath().toString(), unattended), true);
					}
				}
				else {
					com.install4j.runtime.installer.helper.Logger
					.getInstance().error(this, MessageFormat.format("Available version {0} is same as installed {1}, ignoring.", key, installedVersion.orElse("<none>"), media.version()));
				}
				return true;
			}
			else {
				throw new IOException(MessageFormat.format("Did not find any media for {0}", key));
			}
		} catch (Exception e) {
			e.printStackTrace();
			com.install4j.runtime.installer.helper.Logger
			.getInstance().error(this, e.getMessage());
			context.getProgressInterface().showFailure(MessageFormat.format("Failed to install companion application. {0}", e.getMessage()));
		}
		return false;
	}

	public String getJaulAppId() {
		return jaulAppId;
	}

	public void setJaulAppId(String jaulAppId) {
		this.jaulAppId = jaulAppId;
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
}
