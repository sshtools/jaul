package com.sshtools.jaul;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;

public interface AppDef {
	
	String getId();
	
	URL getDescriptor();
	
	String getRawDescriptorURL();

	String getName();

	String getVersion();

	Optional<String> getIcon();

	String getPublisher();

	Optional<URL> getUrl();
	
	AppCategory getCategory();
	
	<A extends AppDef> A forBranch(Optional<String> branch) throws IOException;
	
	default Optional<String> getBranch() {
		try {
			var arr = getRawDescriptorURL().split("/");
			var l = new ArrayList<String>();
			var f = false;
			for(var i = 0 ; i < arr.length; i++) {
				if(arr[i].equals("branch") && !f && l.isEmpty()) {
					f = true;
				}
				else if(arr[i].equals("${phase}")) {
					f  = false;
				}
				if(f)
					l.add(arr[i]);
			}
			if(l.isEmpty())
				throw new IllegalArgumentException("Not branch");
			return Optional.of(String.join("/", l));
		}
		catch(Exception e) {
			return Optional.empty();
		}
	}

}
