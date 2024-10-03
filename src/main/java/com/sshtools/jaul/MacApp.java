package com.sshtools.jaul;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MacApp {

	private final Path app;
	private final Optional<String> name;
	private final boolean hideDock;
	private final List<String> arguments;

	public final static class Builder {
		
		private final Path app;
		private Optional<String> name = Optional.empty();
		private boolean hideDock;
		private final List<String> arguments  = new ArrayList<String>();
		
		public Builder(Path app) {
			this.app = app;
		}

		public Builder withArguments(String... arguments) {
			return withArguments(Arrays.asList(arguments));
		}
		
		public Builder withArguments(Collection<String> arguments) {
			this.arguments.clear();
			this.arguments.addAll(arguments);
			return this;
		}
		
		
		public Builder withHideDock() {
			this.hideDock = true;
			return this;
		}
		
		public Builder withName(String name) {
			this.name = Optional.of(name);
			return this;
		}
		
		public MacApp build() {
			return new MacApp(this);
		}
		
	}

	private MacApp(Builder builder) {
		this.app = builder.app;
		this.name = builder.name;
		this.hideDock = builder.hideDock;
		this.arguments = Collections.unmodifiableList(new ArrayList<String>(builder.arguments));
	}

	public void write(Path dir) throws IOException {
		var appname = name.orElseGet(() -> stripExtension(app.getFileName().toString()));
		
		var appdir = dir.resolve(appname + ".app");
		var contents = appdir.resolve("Contents");
		var macos = contents.resolve("MacOS");
		
		var script = macos.resolve(appname);
		
		Files.createDirectories(macos);
		
		try(var wtr = new PrintWriter(Files.newBufferedWriter(script), true)) {
			wtr.println("#!/bin/sh");
			wtr.println("#######################################################################");
			wtr.println("cd '" + dir.toAbsolutePath().toString() + "'");
			if(app.isAbsolute()) {
				if(arguments.isEmpty())
					wtr.println(app.toString());
				else
					wtr.println(app.toString() + " " + String.join(" ", arguments.stream().map(this::formatArg).collect(Collectors.toList())));
			}
			else {
				if(arguments.isEmpty())
					wtr.println("./" + app.toString());
				else
					wtr.println("./" + app.toString() + " " + String.join(" ", arguments.stream().map(this::formatArg).collect(Collectors.toList())));
			}
			wtr.println();
		}
		
		script.toFile().setExecutable(true, false);
		

		var plist = contents.resolve("Info.plist");
		try(var wtr = new PrintWriter(Files.newBufferedWriter(plist), true)) {
			wtr.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			wtr.println("<!DOCTYPE plist SYSTEM \"file://localhost/System/Library/DTDs/PropertyList.dtd\">");
			wtr.println("<plist version=\"0.9\">");
			wtr.println("<dict>");

			wtr.println("<key>CGBundleDisplayName</key>");
			wtr.println("<value>" + appname + "</value>");
			if(hideDock) {
				wtr.println("<key>LSUIElement</key>");
				wtr.println("<true/>");
			}
			wtr.println("</dict>");
			wtr.println("</plist>");
		}
		
		
		
		
		
		
		// TODO Auto-generated method stub
		
	}

	private String stripExtension(String arg) {
		int idx = arg.lastIndexOf('.');
		return idx == -1 ? arg : arg.substring(0, idx);
	}
	
	private String formatArg(String arg) {
		return "'" + arg.replace("'", "\\'") + "'";
	}
	
}
