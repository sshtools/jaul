package com.sshtools.jaul;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Logging {

	static Logger log = Logger.getLogger(JaulApp.class.getName());

	public static void info(String pattern, Object... args) {
		log.info(args.length == 0 ? pattern : MessageFormat.format(pattern, args));
	}
	
	public static void debug(String pattern, Object... args) {
		log.fine(args.length == 0 ? pattern : MessageFormat.format(pattern, args));
	}
	
	public static void warn(String pattern, Object... args) {
		log.warning(args.length == 0 ? pattern : MessageFormat.format(pattern, args));
	}
	
	public static void error(String pattern, Object... args) {
		log.severe(args.length == 0 ? pattern : MessageFormat.format(pattern, args));
	}

	public static boolean isDebugEnabled() {
		return log.isLoggable(Level.FINE);
	}
	
	
}
