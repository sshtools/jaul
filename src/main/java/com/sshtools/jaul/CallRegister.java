package com.sshtools.jaul;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.install4j.api.context.RemoteCallable;
import com.install4j.runtime.installer.helper.Logger;
import com.sshtools.jaul.UpdateDescriptor.MediaType;

@SuppressWarnings("serial")
public final class CallRegister implements RemoteCallable {

	private String updatesXmlLocation;
	private String jaulAppId;
	private AppCategory appCategory;
	private int updaterId;
	private MediaType mediaType;
	private String installDir;
	private String[] branches;
	private boolean forceUser;
	
	public CallRegister() {
	}

	CallRegister(String updatesXmlLocation, String jaulAppId, AppCategory appCategory,
			int updaterId, MediaType mediaType, String installDir, boolean forceUser, String... branches) {
		super();
		this.updatesXmlLocation = updatesXmlLocation;
		this.jaulAppId = jaulAppId;
		this.appCategory = appCategory;
		this.updaterId = updaterId;
		this.mediaType = mediaType;
		this.installDir = installDir;
		this.branches = branches;
		this.forceUser = forceUser;
	}

	@Override
	public Serializable execute() {
		var nastyDebug = Files.exists(Paths.get("/tmp/debug-jaul"));
		var appReg = JaulAppProvider.fromStatic(jaulAppId, appCategory, updatesXmlLocation, String.valueOf(updaterId), branches);
		var was = System.getProperty("install4j.installationDir");
		var wasForceUser = System.getProperty("jaul.forceUserRegistration");
		try {
			System.setProperty("install4j.installationDir", installDir);
			System.setProperty("jaul.forceUserRegistration", String.valueOf(forceUser));
			if(nastyDebug) {
				Logger.getInstance().info(this, "-------------------------------------------");
				Logger.getInstance().info(this, "DEBUG A - WAITING UNTIL /tmp/next exists");
				Logger.getInstance().info(this, "-------------------------------------------");
				while(!Files.exists(Paths.get("/tmp/next"))) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						throw new IllegalStateException(e);
					}
				}
				try {
					Files.deleteIfExists(Paths.get("/tmp/next"));
				}
				catch(IOException ioe) {}
			}
			AppRegistry.get().register(appReg, mediaType);
			

			if(nastyDebug) {
				Logger.getInstance().info(this, "-------------------------------------------");
				Logger.getInstance().info(this, "DEBUG B - WAITING UNTIL /tmp/next exists");
				Logger.getInstance().info(this, "-------------------------------------------");
				while(!Files.exists(Paths.get("/tmp/next"))) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						throw new IllegalStateException(e);
					}
				}
				try {
					Files.deleteIfExists(Paths.get("/tmp/next"));
				}
				catch(IOException ioe) {}
			}
			
			RegisterJaulAppAction.fixPrefs(this);
		}
		finally {
			if(was == null)
				System.getProperties().remove("install4j.installationDir");
			else
				System.setProperty("install4j.installationDir", was);
			if(wasForceUser == null)
				System.getProperties().remove("jaul.forceUserRegistratio");
			else
				System.setProperty("jaul.forceUserRegistratio", wasForceUser);
		}
		return "";
	}
}