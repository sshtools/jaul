package com.sshtools.jaul;

import java.text.MessageFormat;

import com.install4j.api.Util;
import com.install4j.api.actions.AbstractUninstallAction;
import com.install4j.api.context.UninstallerContext;
import com.install4j.api.context.UserCanceledException;
import com.install4j.runtime.installer.helper.Logger;

@SuppressWarnings("serial")
public class DeregisterJaulAppAction extends AbstractUninstallAction {

	private String jaulAppId = "${compiler:install4j.jaulAppId}";

	@Override
	public boolean uninstall(UninstallerContext context) throws UserCanceledException {
		try {
			doDeregister(context);
			return true;
		} catch (Exception e) {
			Logger.getInstance().error(this, e.getMessage());
			context.getProgressInterface().showFailure(MessageFormat.format("Failed to deregister application with Jaul, updates may not work. {0}", e.getMessage()));
		}
		return false;
	}

	protected void doDeregister(UninstallerContext context) {
		if(Util.hasFullAdminRights()) {
			new CallDeregister(getJaulAppId()).execute();
		}
		else {
			context.runElevated(new CallDeregister(getJaulAppId()), true);
		}
	}

	public String getJaulAppId() {
		return replaceWithTextOverride("jaulAppId", replaceVariables(this.jaulAppId), String.class);
	}

	public void setJaulAppId(String jaulAppId) {
		this.jaulAppId = jaulAppId;
	}

}
