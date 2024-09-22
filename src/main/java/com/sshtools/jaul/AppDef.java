package com.sshtools.jaul;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public interface AppDef {
	
	String getId();
	
	URL getDescriptor();

	String getName();

	String getVersion();

	Optional<String> getIcon();

	String getPublisher();

	Optional<URL> getUrl();
	
	AppCategory getCategory();
	
	<A extends AppDef> A forBranch(Optional<String> branch) throws IOException;

}
