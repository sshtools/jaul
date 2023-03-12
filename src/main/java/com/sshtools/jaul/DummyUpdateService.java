package com.sshtools.jaul;

import java.io.IOException;

import com.sshtools.jaul.DummyUpdater.DummyUpdaterBuilder;


public class DummyUpdateService extends AbstractUpdateService {

	private final DummyUpdaterBuilder builder;

	public DummyUpdateService(UpdateableAppContext context, DummyUpdaterBuilder builder, String currentVersion) {
		super(context, currentVersion);
		this.builder = builder;
	}

	@Override
	public String doUpdate(boolean check) throws IOException {
		return builder.withCheckOnly(check).build().call();
	}

}
