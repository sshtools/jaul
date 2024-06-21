package com.sshtools.jaul;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.LinkedHashSet;
import java.util.Set;

import com.install4j.api.Util;
import com.install4j.api.actions.AbstractUninstallAction;
import com.install4j.api.context.UninstallerContext;
import com.install4j.api.context.UserCanceledException;
import com.install4j.runtime.installer.helper.Logger;

@SuppressWarnings("serial")
public class DeregisterJaulAppAction extends AbstractUninstallAction {

	private String jaulAppId;

	@Override
	public boolean uninstall(UninstallerContext context) throws UserCanceledException {
		try {
			String updaterId = null;
			Set<String> names = new LinkedHashSet<>();
			for(var comp : context.getInstallationComponents()) {
				names.add(comp.getName());
				if(comp.getName().toLowerCase().contains("updater")) {
					updaterId = comp.getId();
				}
			}
			
			if(updaterId == null)
				throw new IOException("Could not find updater ID (" + String.join(", ", names));
			
			doRegister(context, Integer.parseInt(updaterId));
		} catch (Exception e) {
			Logger.getInstance().error(this, e.getMessage());
			context.getProgressInterface().showFailure(MessageFormat.format("Failed to register application with Jaul, updates may not work. {0}", e.getMessage()));
		}
		return false;
	}

	protected void doRegister(UninstallerContext context, int updaterId) {
		if(Util.hasFullAdminRights()) {
			new CallDeregister(jaulAppId).execute();
		}
		else {
			context.runElevated(new CallDeregister(jaulAppId), true);
		}
	}

	public String getJaulAppId() {
		return replaceWithTextOverride("jaulAppId", replaceVariables(this.jaulAppId), String.class);
	}

	public void setJaulAppId(String jaulAppId) {
		this.jaulAppId = jaulAppId;
	}

}
