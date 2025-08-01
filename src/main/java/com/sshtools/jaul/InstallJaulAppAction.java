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
import com.install4j.api.update.UpdateChecker;
import com.install4j.runtime.installer.helper.Logger;
import com.sshtools.jaul.UpdateDescriptor.Media;
import com.sshtools.jaul.UpdateDescriptor.MediaKey;

@SuppressWarnings("serial")
public class InstallJaulAppAction extends AbstractInstallAction implements JaulI4JAction  {

	private String updatesXmlLocation;
	private String jaulAppId;
	private File installDir;
	private boolean unattended = true;
	private boolean forceReinstall = false;
    private boolean updateIfNewerAvailable = true;
	private boolean debug = true;

	@Override
	public boolean install(InstallerContext context) throws UserCanceledException {
		return callSilent(Logger.getInstance(), () ->  {
			try {
				/* First try to see if the app is already installed, and get what version
				 * it is at if it is.
				 */
				Optional<String> installedVersion = Optional.empty();
				
				var actualJaulAppId = replaceVariables(jaulAppId);
				try {
					var app= AppRegistry.get().get(actualJaulAppId);
					var appDef  = new LocalAppDef(app);
					Logger.getInstance().info(this, MessageFormat.format("{0} is installed, version {1}.", actualJaulAppId, appDef.getVersion()));
					installedVersion = Optional.of(appDef.getVersion());
				}
				catch(IllegalStateException | IllegalArgumentException iae) {
					/* Not installed */
				    Logger.getInstance().log(iae);
					Logger.getInstance().info(this, MessageFormat.format("{0} is not installed, will try to download.", actualJaulAppId));
				}
				
				var uri = URI.create(replaceVariables(updatesXmlLocation));
				Logger.getInstance().info(this, MessageFormat.format("Getting {0} from {1}", actualJaulAppId, uri));
				var desc = UpdateDescriptor.get(uri);
				Logger.getInstance().info(this, MessageFormat.format("Got {0} from {1}", actualJaulAppId, uri));
				var key = MediaKey.get();
				var mediaOr = desc.find(key);
				if(mediaOr.isPresent()) {
					var media = mediaOr.get();
					if(installedVersion.isEmpty()) {
						Logger.getInstance().info(this, MessageFormat.format("{0} is being installed because it is not installed.", actualJaulAppId));
						doInstall(context, media);
					}
					else if(forceReinstall) {
						Logger.getInstance().info(this, MessageFormat.format("{0} is being installed because forceReinstall was set.", actualJaulAppId));
						doInstall(context, media);
					}
                    else if(updateIfNewerAvailable && UpdateChecker.isVersionGreaterThan(media.version(), installedVersion.get())) {
						Logger.getInstance().info(this, MessageFormat.format("{0} is being installed because version {1} is lower than available version {2}.", actualJaulAppId, installedVersion.get(), media.version()));
						doInstall(context, media);
					}
					else {
						Logger.getInstance().error(this, MessageFormat.format("Available version {0} is same as installed {1}, ignoring.", key, installedVersion.orElse("<none>"), media.version()));
					}
					return true;
				}
				else {
					throw new IOException(MessageFormat.format("Did not find any media for {0}", key));
				}
			} catch (Exception e) {
				Logger.getInstance().error(this, e.getMessage());
				context.getProgressInterface().showFailure(MessageFormat.format("Failed to install companion application. {0}", e.getMessage()));
			}
			return false;
		});
	}

	protected void doInstall(InstallerContext context, Media media) {
		var url = media.url();
		if(Util.hasFullAdminRights() || Util.isAdminGroup()) {
			new CallInstall(context.getProgressInterface(), url.toExternalForm(), installDir == null ? null : installDir.getAbsolutePath().toString(), unattended, null, true, debug).execute();
		}
		else {
			context.runElevated(new CallInstall(null, url.toExternalForm(), installDir == null ? null : installDir.getAbsolutePath().toString(), unattended, null, true, debug), true);
		}
	}

	public boolean isUpdateIfNewerAvailable() {
		return replaceWithTextOverride("updateIfNewerAvailable", this.updateIfNewerAvailable);
	}

    public boolean isForceReinstall() {
        return replaceWithTextOverride("forceReinstall", this.forceReinstall);
    }

	public void setForceReinstall(boolean forceReinstall) {
		this.forceReinstall = forceReinstall;
	}

	public void setUpdateIfNewerAvailable(boolean updateIfNewerAvailable) {
        this.updateIfNewerAvailable = updateIfNewerAvailable;
    }

    public String getJaulAppId() {
		return replaceWithTextOverride("jaulAppId", replaceVariables(this.jaulAppId), String.class);
	}

	public void setJaulAppId(String jaulAppId) {
		this.jaulAppId = jaulAppId;
	}

	public File getInstallDir() {
		return replaceWithTextOverride("installDir", replaceVariables(this.installDir), File.class);
	}

	public void setInstallDir(File installDir) {
		this.installDir = installDir;
	}

	public boolean isUnattended() {
		return replaceWithTextOverride("unattended", this.unattended);
	}

	public void setUnattended(boolean unattended) {
		this.unattended = unattended;
	}

	public boolean isDebug() {
		return replaceWithTextOverride("debug", this.unattended);
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public String getUpdatesXmlLocation() {
		return replaceWithTextOverride("updatesXmlLocation", replaceVariables(this.updatesXmlLocation), String.class);
	}

	public void setUpdatesXmlLocation(String updatesXmlLocation) {
		this.updatesXmlLocation = updatesXmlLocation;
	}
}
