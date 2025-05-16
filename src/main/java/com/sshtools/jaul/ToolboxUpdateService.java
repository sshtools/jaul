package com.sshtools.jaul;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.sshtools.jaul.AppRegistry.App;

public class ToolboxUpdateService extends AbstractUpdateService {
	
	private final App app;

	public ToolboxUpdateService(UpdateableAppContext context, App app) {
		super(context);
		
		this.app = app;
	}

	@Override
	protected String doUpdate(boolean check) throws IOException {
		if(check) {
			var pb = new ProcessBuilder("jaul-do", "update", "-l", app.getId());
			pb.redirectError(Redirect.INHERIT);
			pb.redirectOutput(Redirect.INHERIT);
			var prc = pb.start();
			var versions = new ArrayList<String>();
			try(var in = new BufferedReader(new InputStreamReader(prc.getInputStream()))) {
				String line;
				while( ( line = in.readLine() ) != null) {
					versions.add(line);
				}
			}
			
			try {
				if(prc.waitFor(1, TimeUnit.MINUTES)) {
					if(prc.exitValue() == 0 && !versions.isEmpty()) {
						return versions.get(0);
					}
					else
						return null;
				}
				else {
					prc.destroy();
					return null;
				}
			}
			catch(InterruptedException ie) {
				throw new IOException(ie);
			}
		}
		else {
			var pb = new ProcessBuilder("jaul-do", "update", app.getId(), "--unattended");
			pb.redirectError(Redirect.INHERIT);
			pb.redirectOutput(Redirect.INHERIT);
			pb.redirectInput(Redirect.INHERIT);
			var prc = pb.start();
			try {
				if(prc.waitFor(5, TimeUnit.MINUTES)) {
					if(prc.exitValue() == 0) { 
						Logging.warn("Updater exited with success state, but we are still running. This is unexpected.");
					}
					else 
						Logging.error("Updater exited with error state, check for above output for possible reasons.");
				}
				else {
					prc.destroy();
				}
				return null;
			}
			catch(InterruptedException ie) {
				throw new IOException(ie);
			}
		}
	}

}
