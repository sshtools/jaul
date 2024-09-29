package com.sshtools.jaul;

import java.io.Serializable;

import com.install4j.api.context.RemoteCallable;
import com.install4j.runtime.installer.helper.Logger;

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
		try {
			return AppRegistry.get().get(jaulAppId);
		}
		catch(Exception e) {
			Logger.getInstance().error(this, "Failed to get " + jaulAppId + ". " + e.getMessage());
			return null;
		}
	}
}