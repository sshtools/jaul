package com.sshtools.jaul;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.jaul.Install4JUpdater.Install4JUpdaterBuilder;

public class Install4JUpdateService extends AbstractUpdateService {

	static Logger log = LoggerFactory.getLogger(Install4JUpdateService.class);
	
	private final Install4JUpdaterBuilder builder;

	public Install4JUpdateService(UpdateableAppContext context, Install4JUpdaterBuilder builder) {
		super(context);
		this.builder = builder;
	}

	@Override
	protected String doUpdate(boolean checkOnly) throws IOException {
		return builder.withCheckOnly(checkOnly).build().call();

	}

}
