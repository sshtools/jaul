package com.sshtools.jaul;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.prefs.Preferences;

public enum Phase {
	STABLE, EA, CONTINUOUS;
	
	private final static String KEY_CONTINUOUS_ALLOWED = "continuous";
	
	public static Phase getDefaultPhaseForVersion(String... versions) {
		if(versions.length == 0)
			return Phase.STABLE;
		else {
			var appVersion = versions[0];
			return appVersion == null || appVersion.startsWith("0.") || appVersion.equals("DEV_VERSION") || appVersion.contains("SNAPSHOT") ? Phase.CONTINUOUS : Phase.STABLE;
		}
	}
	
	public static Phase[] reversePhases() {
		var l = new ArrayList<Phase>(Arrays.asList(values()));
		Collections.reverse(l);
		return l.toArray(new Phase[0]);
	}
	
	public static Phase[] getAvailablePhases() {
		if(isContinuousAllowed())
			return Phase.values();
		else
			return new Phase[] { Phase.STABLE, Phase.EA };
	}
	
	public static boolean isContinuousAllowed() {
		 if(Boolean.getBoolean("jaul.noContinuous"))
			 return false;
		return ArtifactVersion.isDeveloperWorkspace() || 
			   Boolean.getBoolean("jaul.continuous") || 
			   Boolean.getBoolean("jadaptive.development") ||
		       Preferences.userNodeForPackage(Phase.class).getBoolean(KEY_CONTINUOUS_ALLOWED, false) || 
		       Files.exists(AppRegistry.getUserData().resolve("continuous"));
	}
}
