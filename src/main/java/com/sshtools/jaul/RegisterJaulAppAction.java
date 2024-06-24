package com.sshtools.jaul;

import java.text.MessageFormat;

import com.install4j.api.Util;
import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;
import com.install4j.runtime.installer.helper.Logger;
import com.sshtools.jaul.UpdateDescriptor.MediaType;

@SuppressWarnings("serial")
public class RegisterJaulAppAction extends AbstractInstallAction {

	private String updatesXmlLocation = "${compiler:install4j.updatesBase}/${compiler:install4j.phase}/updates.xml";
	private String updaterId = "${compiler:install4j.jaulUpdaterId}";
	private String jaulAppId = "${compiler:install4j.jaulAppId}";
	private AppCategory appCategory;

	@Override
	public boolean install(InstallerContext context) throws UserCanceledException {
		try {
			if(Util.hasFullAdminRights()) {
				new CallRegister(getUpdatesXmlLocation(), getJaulAppId(), getAppCategory(), Integer.parseInt(getUpdaterId()), MediaType.INSTALLER, context.getInstallationDirectory().getAbsolutePath()).execute();
			}
			else {
				context.runElevated(new CallRegister(getUpdatesXmlLocation(), getJaulAppId(), getAppCategory(), Integer.parseInt(getUpdaterId()), MediaType.INSTALLER, context.getInstallationDirectory().getAbsolutePath()), true);
			}
		} catch (Exception e) {
			Logger.getInstance().error(this, e.getMessage());
			context.getProgressInterface().showFailure(MessageFormat.format("Failed to register application with Jaul, updates may not work. {0}", e.getMessage()));
		}
		return false;
	}


	public String getUpdaterId() {
		return replaceWithTextOverride("updaterId", replaceVariables(this.updaterId), String.class);
	}

	public void setUpdaterId(String updaterId) {
		this.updaterId = updaterId;
	}

	public String getJaulAppId() {
		return replaceWithTextOverride("jaulAppId", replaceVariables(this.jaulAppId), String.class);
	}

	public void setJaulAppId(String jaulAppId) {
		this.jaulAppId = jaulAppId;
	}

	public AppCategory getAppCategory() {
		return appCategory;
	}

	public void setAppCategory(AppCategory appCategory) {
		this.appCategory = appCategory;
	}

	public String getUpdatesXmlLocation() {
		return replaceWithTextOverride("updatesXmlLocation", replaceVariables(this.updatesXmlLocation), String.class);
	}

	public void setUpdatesXmlLocation(String updatesXmlLocation) {
		this.updatesXmlLocation = updatesXmlLocation;
	}
}
