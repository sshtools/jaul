package com.sshtools.jaul;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public class Logging {

	static Logger log = System.getLogger(JaulApp.class.getName());

	public static void info(String pattern, Object... args) {
		log.log(Level.INFO, pattern, args);
	}
	
	public static void debug(String pattern, Object... args) {
		log.log(Level.DEBUG, pattern, args);
	}
	
	public static void warn(String pattern, Object... args) {
		log.log(Level.WARNING, pattern, args);
	}
	
	public static void error(String pattern, Object... args) {
		log.log(Level.ERROR, pattern, args);
	}

	public static boolean isDebugEnabled() {
		return log.isLoggable(Level.DEBUG);
	}
	
	
}
