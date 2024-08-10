package com.sshtools.jaul;

import java.util.concurrent.Callable;

import com.sshtools.jaul.Logging.Logger;


public interface JaulI4JAction {
	
	default void run(com.install4j.runtime.installer.helper.Logger del,  Runnable r) {
		callSilent(del, () -> { 
			r.run();
			return null;
		});
	}

	default <R> R callSilent(com.install4j.runtime.installer.helper.Logger del,  Callable<R> r) {
		try {
			return call(del, r);
		} catch(RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	default <R> R call(com.install4j.runtime.installer.helper.Logger del,  Callable<R> r) throws Exception {
		Logging.setLogger(new Logger() {
			@Override
			public void warning(String msg) {
				del.info(this, "[WARMNING] " + msg);
			}
			
			@Override
			public boolean isDebug() {
				return true;
			}
			
			@Override
			public void info(String msg) {
				del.info(this, msg);
			}
			
			@Override
			public void error(String msg) {
				del.error(this, msg);
			}
			
			@Override
			public void debug(String msg) {
				del.info(this, "[DEBUG] " + msg);
				
			}
		});
		return r.call();
	}
}
