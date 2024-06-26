package com.sshtools.jaul;

import java.io.Serializable;

import com.sshtools.jaul.AppRegistry.Scope;
import com.sshtools.jaul.UpdateDescriptor.MediaType;

import uk.co.bithatch.nativeimage.annotations.Serialization;

@SuppressWarnings("serial")
@Serialization
public class TelemetryEvent implements Serializable {

	@Serialization
	public enum Type {
		LAUNCH, SHUTDOWN, UPDATE_CHECK, UPDATE, REGISTER, DEREGISTER, CUSTOM
	}

	public static class TelemetryEventBuilder {
		private Type type = Type.CUSTOM;
		private MediaType packaging = MediaType.INSTALLER;
		private String description;
		private String appId;
		private String runId;
		private Scope scope;
		
		public TelemetryEventBuilder withType(Type type) {
			this.type = type;
			return this;
		}
		
		public TelemetryEventBuilder withPackaging(MediaType packaging) {
			this.packaging = packaging;
			return this;
		}
		
		public TelemetryEventBuilder withDescription(String description) {
			this.description = description;
			return this;
		}
		
		public TelemetryEventBuilder withAppId(String appId) {
			this.appId = appId;
			return this;
		}
		
		public TelemetryEventBuilder withRunId(String runId) {
			this.runId = runId;
			return this;
		}
		
		public TelemetryEventBuilder withScope(Scope scope) {
			this.scope = scope;
			return this;
		}
		
		public TelemetryEvent build() {
			return new TelemetryEvent(this);
		}
	}
	
	private final String description;
	private final String appId;
	private final String runId;
	private final Type type;
	private final Scope scope;
	private final long timestamp = System.currentTimeMillis();
	private final MediaType packaging;
	
	TelemetryEvent(TelemetryEventBuilder builder) {
		if(builder.runId == null)
			throw new IllegalStateException("Run ID must be set.");
		if(builder.appId == null)
			throw new IllegalStateException("App ID must be set.");
		if(builder.scope == null)
			throw new IllegalStateException("App scope must be set.");
		
		this.description = builder.description;
		this.appId = builder.appId;
		this.runId = builder.runId;
		this.scope = builder.scope;
		this.type = builder.type;
		this.packaging = builder.packaging;
	}
	
	public final MediaType getPackaging() {
		return packaging;
	}

	public final String getDescription() {
		return description;
	}

	public final String getAppId() {
		return appId;
	}

	public final String getRunId() {
		return runId;
	}

	public final Type getType() {
		return type;
	}

	public final Scope getScope() {
		return scope;
	}

	public final long getTimestamp() {
		return timestamp;
	}

}
