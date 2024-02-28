package com.sshtools.jaul;

import java.text.MessageFormat;

import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;
import com.install4j.runtime.installer.helper.Logger;

@SuppressWarnings("serial")
public class GetJaulAppAction extends AbstractInstallAction {

	private String jaulAppId;
	private String versionVariableName = "appVersion";
	private String installDirectoryVariableName = "appInstallDir";

	@Override
	public boolean install(InstallerContext context) throws UserCanceledException {
		try {
			/* First try to see if the app is already installed, and get what version
			 * it is at if it is.
			 */
			try {
				var actualJaulAppId = getJaulAppId();
				var app= AppRegistry.get().get(actualJaulAppId);
				var appDef  = new LocalAppDef(app);
				var installDir = appDef.getRegistryDef().getDir().toString();
				Logger.getInstance().info(this, MessageFormat.format("{0} is installed, version {1}, install dir {2}.", actualJaulAppId, appDef.getVersion(), installDir));
				context.setVariable(getVersionVariableName(), appDef.getVersion());
				context.setVariable(getInstallDirectoryVariableName(), installDir);
			}
			catch(IllegalStateException iae) {
				/* Not installed */
				Logger.getInstance().info(this, MessageFormat.format("{0} is not installed, setting variables to empty.", jaulAppId));
				context.setVariable(getVersionVariableName(), "");
				context.setVariable(getInstallDirectoryVariableName(), "");
			}
		} catch (Exception e) {
			Logger.getInstance().error(this, e.getMessage() + ". Setting variables to empty.");
			context.setVariable(getVersionVariableName(), "");
			context.setVariable(getInstallDirectoryVariableName(), "");
		}
		return false;
	}

	public String getJaulAppId() {
		return replaceWithTextOverride("jaulAppId", replaceVariables(this.jaulAppId), String.class);
	}

	public void setJaulAppId(String jaulAppId) {
		this.jaulAppId = jaulAppId;
	}

	public String getVersionVariableName() {
		return replaceWithTextOverride("versionVariableName", replaceVariables(this.versionVariableName), String.class);
	}

	public String getInstallDirectoryVariableName() {
		return replaceWithTextOverride("installDirectoryVariableName", replaceVariables(this.installDirectoryVariableName), String.class);
	}

	public void setInstallDirectoryVariableName(String installDirectoryVariableName) {
		this.installDirectoryVariableName = installDirectoryVariableName;
	}

	public void setVersionVariableName(String variableName) {
		this.versionVariableName = variableName;
	}
}
