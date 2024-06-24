package com.sshtools.jaul;

import java.io.Serializable;

import com.install4j.api.context.RemoteCallable;

@SuppressWarnings("serial")
public final class CallGet implements RemoteCallable {

	private String jaulAppId;
	
	public CallGet() {
	}

	CallGet(String jaulAppId) {
		super();
		this.jaulAppId = jaulAppId;
	}

	@Override
	public Serializable execute() {
		return AppRegistry.get().get(jaulAppId);
	}
}