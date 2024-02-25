package com.sshtools.jaul;

import java.text.MessageFormat;

import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;
import com.install4j.runtime.installer.helper.Logger;

@SuppressWarnings("serial")
public class GetJaulAppInstallDirAction extends AbstractInstallAction {

	private String jaulAppId;
	private String variableName;

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
				Logger.getInstance().info(this, MessageFormat.format("{0} is installed, version {1}. Setting variable {2}", actualJaulAppId, appDef.getVersion(), variableName));
				context.setVariable(getVariableName(), appDef.getVersion());
			}
			catch(IllegalStateException iae) {
				/* Not installed */
				Logger.getInstance().info(this, MessageFormat.format("{0} is not installed, setting variable {1} to empty.", jaulAppId, variableName));
				context.setVariable(getVariableName(), "");
			}
		} catch (Exception e) {
			Logger.getInstance().error(this, e.getMessage() + ". Setting variable " + variableName + " to empty.");
			context.setVariable(getVariableName(), "");
		}
		return false;
	}

	public String getJaulAppId() {
		return replaceWithTextOverride("jaulAppId", replaceVariables(this.jaulAppId), String.class);
	}

	public void setJaulAppId(String jaulAppId) {
		this.jaulAppId = jaulAppId;
	}

	public String getVariableName() {
		return replaceWithTextOverride("variableName", replaceVariables(this.variableName), String.class);
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}
}
