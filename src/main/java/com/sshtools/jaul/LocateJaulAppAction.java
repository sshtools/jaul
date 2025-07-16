package com.sshtools.jaul;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Optional;

import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;
import com.install4j.runtime.installer.helper.Logger;
import com.sshtools.jaul.UpdateDescriptor.Media;
import com.sshtools.jaul.UpdateDescriptor.MediaKey;

@SuppressWarnings("serial")
public class LocateJaulAppAction extends AbstractInstallAction implements JaulI4JAction  {

	private String updatesXmlLocation;
	private String jaulAppId;
	private boolean debug = true;
	private String versionVariableName = "appVersion";
	private String downloadUrlVariableName = "appUrl";

	@Override
	public boolean install(InstallerContext context) throws UserCanceledException {

		context.setVariable(getVersionVariableName(), "");
		context.setVariable(getDownloadUrlVariableName(), "");
		
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
						doNeedInstall(context, media);
					}
					else if(!installedVersion.get().equals(media.version())) {
						Logger.getInstance().info(this, MessageFormat.format("{0} is being installed because version {1} differs from version {2}.", actualJaulAppId, installedVersion.get(), media.version()));
						doNeedInstall(context, media);
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

	protected void doNeedInstall(InstallerContext context, Media media) {
		context.setVariable(getVersionVariableName(), media.version());
		context.setVariable(getDownloadUrlVariableName(), media.url().toString());
	}

	public String getVersionVariableName() {
		return replaceWithTextOverride("versionVariableName", replaceVariables(this.versionVariableName), String.class);
	}

	public String getDownloadUrlVariableName() {
		return replaceWithTextOverride("downloadUrlVariableName", replaceVariables(this.downloadUrlVariableName), String.class);
	}

	public void setDownloadUrlVariableName(String downloadUrlVariableName) {
		this.downloadUrlVariableName = downloadUrlVariableName;
	}

	public void setVersionVariableName(String variableName) {
		this.versionVariableName = variableName;
	}

	public String getJaulAppId() {
		return replaceWithTextOverride("jaulAppId", replaceVariables(this.jaulAppId), String.class);
	}

	public void setJaulAppId(String jaulAppId) {
		this.jaulAppId = jaulAppId;
	}

	public boolean isDebug() {
		return replaceWithTextOverride("debug", this.debug);
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
