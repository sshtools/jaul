package com.sshtools.jaul;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DummyUpdater implements Callable<String> {
	
	public final static class DummyUpdaterBuilder {
		
		private long checkPause = 10000;
		private long updatePause = 5000;
		private boolean fail = true;
		private boolean checkOnly = true;

		public static DummyUpdaterBuilder builder() {
			return new DummyUpdaterBuilder();
		}
		
		public DummyUpdaterBuilder withCheckPause(long checkPause) {
			this.checkPause = checkPause;
			return this;
		}
		
		public DummyUpdaterBuilder withUpdatePause(long updatePause) {
			this.updatePause = updatePause;
			return this;
		}
		
		public DummyUpdaterBuilder withoutFail() {
			return withFail(false);
		}
		
		public DummyUpdaterBuilder withFail(boolean fail) {
			this.fail = fail;
			return this;
		}
		
		public DummyUpdaterBuilder withCheckOnly(boolean checkOnly) {
			this.checkOnly = checkOnly;
			return this;
		}
		
		public DummyUpdaterBuilder withUpdate() {
			return withCheckOnly(false);
		}
		
		public DummyUpdaterBuilder withCheckOnly() {
			return withCheckOnly(true);
		}

		public DummyUpdater build() {
			return new DummyUpdater(this);
		}
		
	}

	static Logger log = LoggerFactory.getLogger(Install4JUpdateService.class);

	private final long checkPause;
	private final long updatePause;
	private final boolean fail;
	private final boolean check;

	private DummyUpdater(DummyUpdaterBuilder builder) {
		this.checkPause = builder.checkPause;
		this.updatePause = builder.updatePause;
		this.fail = builder.fail;
		this.check = builder.checkOnly;
	}

	@Override
	public String call() throws IOException {
		var fakeVersion = System.getProperty("jaul.fakeUpdateVersion", "999.999.999");
		if(check) {
			try {
				Thread.sleep(checkPause);
			} catch (InterruptedException e) {
			}
			return fakeVersion.equals("") ? null : fakeVersion;
		}
		else {
			try {
				Thread.sleep(updatePause);
			} catch (InterruptedException e) {
			}
			if(fail)
				throw new IOException("Failed to update.");
			else
				return fakeVersion;
		}

	}
}
