package com.sshtools.jaul;

import java.text.MessageFormat;

import com.install4j.api.Util;
import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;
import com.install4j.runtime.installer.helper.Logger;
import com.sshtools.jaul.AppRegistry.App;
import com.sshtools.jaul.UpdateDescriptor.MediaType;

@SuppressWarnings("serial")
public class RegisterJaulAppAction extends AbstractInstallAction {

	private static final String PREVIOUS_JAUL_REGISTRATION = "previousJaulRegistration";
	private String updatesBase = "${compiler:install4j.updatesBase}";
	private String updaterId = "${compiler:install4j.jaulUpdaterId}";
	private String jaulAppId = "${compiler:install4j.jaulAppId}";
	private AppCategory appCategory;

	@Override
	public boolean install(InstallerContext context) throws UserCanceledException {
		try {
			context.setVariable(PREVIOUS_JAUL_REGISTRATION, getApp(context, getJaulAppId()));

			Logger.getInstance().info(this, "Full: " + Util.hasFullAdminRights() + " Elev: " + context.hasBeenElevated());
			Logger.getInstance().info(this, "Registering with Jaul as admin user");
			
//			if(Util.hasFullAdminRights() || context.hasBeenElevated()) {
//				Logger.getInstance().info(this, "Registering with Jaul as admin user");
				new CallRegister(getUpdatesBase() + "/${phase}/updates.xml", getJaulAppId(), getAppCategory(), Integer.parseInt(getUpdaterId()), MediaType.INSTALLER, context.getInstallationDirectory().getAbsolutePath()).execute();
//				Logger.getInstance().info(this, "Registered with Jaul as admin user");
//			}
//			else {
//				Logger.getInstance().info(this, "Registering with Jaul as elevated user");
//				context.runElevated(new CallRegister(getUpdatesBase() + "/${phase}/updates.xml", getJaulAppId(), getAppCategory(), Integer.parseInt(getUpdaterId()), MediaType.INSTALLER, context.getInstallationDirectory().getAbsolutePath()), true);
//				Logger.getInstance().info(this, "Registered with Jaul as elevated user");
//			}
			return true;
		} catch (Exception e) {
			Logger.getInstance().error(this, e.getMessage());
			context.getProgressInterface().showFailure(MessageFormat.format("Failed to register application with Jaul, updates may not work. {0}", e.getMessage()));
		}
		return false;
	}

    @Override
    public void rollback(InstallerContext context) {
    	App was = (App)context.getVariable(PREVIOUS_JAUL_REGISTRATION);
    	if(was == null) {
    		if(Util.hasFullAdminRights()) {
				new CallDeregister(getJaulAppId()).execute();
			}
			else {
				context.runElevated(new CallDeregister(getJaulAppId()), true);
			}
    	}
    	else {
    		if(Util.hasFullAdminRights()) {
				new CallRegister(was.getUpdatesUrl().get(), getJaulAppId(), 
						was.getCategory(), Integer.parseInt(was.getLauncherId()), MediaType.INSTALLER, 
						was.getDir().toString()).execute();
			}
			else {
				context.runElevated(new CallRegister(getUpdatesBase() + "/${phase}/updates.xml", getJaulAppId(), getAppCategory(), Integer.parseInt(getUpdaterId()), MediaType.INSTALLER, context.getInstallationDirectory().getAbsolutePath()), true);
			}
    	}
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

	public String getUpdatesBase() {
		return replaceWithTextOverride("updatesBase", replaceVariables(this.updatesBase), String.class);
	}

	public void setUpdatesBase(String updatesBase) {
		this.updatesBase = updatesBase;
	}
	
	private App getApp(InstallerContext context, String id) {
		try {
			if(Util.hasFullAdminRights()) {
				return (App)new CallGet(id).execute();
			}
			else {
				return (App)context.runElevated(new CallGet(id), true);
			}
		}
		catch(Exception e) {
			return null;
		}
	}
}
