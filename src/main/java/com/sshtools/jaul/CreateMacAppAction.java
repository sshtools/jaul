package com.sshtools.jaul;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;
import com.install4j.runtime.installer.helper.Logger;

@SuppressWarnings("serial")
public class CreateMacAppAction extends AbstractInstallAction {

	private File target;
	private boolean hideDock;
	private String name;
	private String id;
	private String[] arguments;

	@Override
	public boolean install(InstallerContext context) throws UserCanceledException {

		Path dir = Paths.get((String)context.getVariable("sys.installationDir"));
		
		Logger.getInstance().info(this, MessageFormat.format("Creating Mac App from target {0} with name {1} in {2}", target, name, dir));
		
		MacApp.Builder bldr = new MacApp.Builder(target.toPath(), id == null ? name : id);
		if(name != null && name.length() > 0)
			bldr.withName(name);
		if(hideDock)
			bldr.withHideDock();
		if(arguments != null)
			bldr.withArguments(arguments);
		
		MacApp app = bldr.build();
		try {
			app.write(dir);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		
		Logger.getInstance().info(this, MessageFormat.format("Created Mac App from target {0} with name {1} in {2}", target, name, dir));
		
		return true;
	}

	public String[] getArguments() {
		return arguments;
	}

	public void setArguments(String[] arguments) {
		this.arguments = arguments;
	}

	public File getTarget() {
		return target;
	}

	public void setTarget(File target) {
		this.target = target;
	}

	public boolean isHideDock() {
		return hideDock;
	}

	public void setHideDock(boolean hideDock) {
		this.hideDock = hideDock;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
