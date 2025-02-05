package com.sshtools.jaul;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Optional;

public class Logging {
	
	public interface Logger {
		void info(String msg);

		void debug(String msg);

		void warning(String msg);

		default void error(String msg, Throwable exception) {
			error(msg);
			var sw = new StringWriter();
			exception.printStackTrace(new PrintWriter(sw, true));
			error(sw.toString());
		}

		void error(String msg);

		boolean isDebug();
		
	}
	
	private final static class Defaults {
		private final static Logger DEFAULT = new Logger() {
			@Override
			public void info(String msg) {
			}

			@Override
			public void debug(String msg) {
			}

			@Override
			public void warning(String msg) {
				System.err.println("[WARNING] " + msg);
			}

			@Override
			public void error(String msg) {
				System.err.println("[ERROR] " + msg);
			}

			@Override
			public boolean isDebug() {
				return false;
			}
		};
	}

	private static Optional<Logger> logger = Optional.empty();
	
	public static void setLogger(Logger logger) {
		Logging.logger  = Optional.of(logger);
	}

	private static Logger log() {
		return Logging.logger.orElseGet(() -> Defaults.DEFAULT); 
	}

	public static void info(String pattern, Object... args) {
		log().info(args.length == 0 ? pattern : MessageFormat.format(pattern, args));
	}
	
	public static void debug(String pattern, Object... args) {
		log().debug(args.length == 0 ? pattern : MessageFormat.format(pattern, args));
	}
	
	public static void warn(String pattern, Object... args) {
		log().warning(args.length == 0 ? pattern : MessageFormat.format(pattern, args));
	}
	
	public static void error(String pattern,  Throwable exception, Object... args) {
		log().error(args.length == 0 ? pattern : MessageFormat.format(pattern, args), exception);
	}
	
	public static void error(String pattern, Object... args) {
		log().error(args.length == 0 ? pattern : MessageFormat.format(pattern, args));
	}

	public static boolean isDebugEnabled() {
		return log().isDebug();
	}
	
	
}
