package com.sshtools.jaul;

public enum Phase {
	STABLE, EA, CONTINUOUS;
	
	public static Phase getDefaultPhaseForVersion(String... versions) {
		if(versions.length == 0)
			return Phase.STABLE;
		else {
			var appVersion = versions[0];
			return appVersion.startsWith("0.") || appVersion.contains("SNAPSHOT") ? Phase.CONTINUOUS : Phase.STABLE;
		}
	}
}
