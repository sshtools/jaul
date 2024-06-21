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
	
	public CallRegister() {
	}

	CallRegister(String updatesXmlLocation, String jaulAppId, AppCategory appCategory,
			int updaterId, MediaType mediaType) {
		super();
		this.updatesXmlLocation = updatesXmlLocation;
		this.jaulAppId = jaulAppId;
		this.appCategory = appCategory;
		this.updaterId = updaterId;
		this.mediaType = mediaType;
	}

	@Override
	public Serializable execute() {
		var appReg = JaulAppProvider.fromStatic(jaulAppId, appCategory, updatesXmlLocation, String.valueOf(updaterId));
		AppRegistry.get().register(appReg, mediaType);
		return "";
	}
}