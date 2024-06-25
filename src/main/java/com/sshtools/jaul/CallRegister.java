package com.sshtools.jaul;

import java.io.Serializable;

import com.install4j.api.context.RemoteCallable;
import com.sshtools.jaul.UpdateDescriptor.MediaType;

@SuppressWarnings("serial")
public final class CallRegister implements RemoteCallable {

	private String updatesXmlLocation;
	private String jaulAppId;
	private AppCategory appCategory;
	private int updaterId;
	private MediaType mediaType;
	private String installDir;
	
	public CallRegister() {
	}

	CallRegister(String updatesXmlLocation, String jaulAppId, AppCategory appCategory,
			int updaterId, MediaType mediaType, String installDir) {
		super();
		this.updatesXmlLocation = updatesXmlLocation;
		this.jaulAppId = jaulAppId;
		this.appCategory = appCategory;
		this.updaterId = updaterId;
		this.mediaType = mediaType;
		this.installDir = installDir;
	}

	@Override
	public Serializable execute() {
		var appReg = JaulAppProvider.fromStatic(jaulAppId, appCategory, updatesXmlLocation, String.valueOf(updaterId));
		var was = System.getProperty("install4j.installationDir");
		var wasSysRoot = System.getProperty("java.util.prefs.systemRoot");
		var wasUserRoot = System.getProperty("java.util.prefs.userRoot");
		System.out.println("WAS SYS ROOT " + wasSysRoot);
		System.out.println("WAS USER ROOT " + wasUserRoot);
		try {
			System.setProperty("install4j.installationDir", installDir);
			System.setProperty("java.util.prefs.systemRoot", "/etc/.java");
			System.setProperty("java.util.prefs.userRoot", System.getProperty("user.home"));
			AppRegistry.get().register(appReg, mediaType);
		}
		finally {
			if(was == null)
				System.getProperties().remove("install4j.installationDir");
			else
				System.setProperty("install4j.installationDir", was);
			if(wasSysRoot == null)
				System.getProperties().remove("java.util.prefs.systemRoot");
			else
				System.setProperty("java.util.prefs.systemRoot", wasSysRoot);
			if(wasUserRoot == null)
				System.getProperties().remove("java.util.prefs.userRoot");
			else
				System.setProperty("java.util.prefs.userRoot", wasUserRoot);
		}
		return "";
	}
}