package com.sshtools.jaul;

import java.io.Serializable;

import com.install4j.api.context.RemoteCallable;

@SuppressWarnings("serial")
public final class CallDeregister implements RemoteCallable {

	private String jaulAppId;
	
	public CallDeregister() {
	}

	CallDeregister(String jaulAppId) {
		super();
		this.jaulAppId = jaulAppId;
	}

	@Override
	public Serializable execute() {
		AppRegistry.get().deregister(jaulAppId);
		return "";
	}
}