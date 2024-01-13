package com.sshtools.jaul;

import java.io.IOException;
import java.util.function.Supplier;

import com.sshtools.jaul.DummyUpdater.DummyUpdaterBuilder;


public class DummyUpdateService extends AbstractUpdateService {

	private final Supplier<DummyUpdaterBuilder> builder;

	@Deprecated
	public DummyUpdateService(UpdateableAppContext context, DummyUpdaterBuilder builder, String currentVersion) {
		this(context, () -> builder);
	}
	
	public DummyUpdateService(UpdateableAppContext context, Supplier<DummyUpdaterBuilder> builder) {
		super(context);
		this.builder = builder;
	}

	@Override
	public String doUpdate(boolean check) throws IOException {
		DummyUpdaterBuilder bldr = builder.get();
		var dummyUpdater = bldr.
				withCheckOnly(check).
				build();
		return dummyUpdater.call();
	}

}
