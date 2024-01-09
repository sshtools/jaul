package com.sshtools.jaul;

import java.text.MessageFormat;

public interface JaulAppProvider {

	String id();
	
	AppCategory category();
	
	String updatesUrl();
	
	String updaterId();
	
	static JaulAppProvider fromStatic(String id, AppCategory category, String updatesUrl, String updaterId) {
		return new JaulAppProvider() {
			
			@Override
			public String updatesUrl() {
				return id;
			}
			
			@Override
			public String updaterId() {
				return updaterId;
			}
			
			@Override
			public String id() {
				return id;
			}
			
			@Override
			public AppCategory category() {
				return category;
			}
		};
	}

	static JaulAppProvider fromClass(Class<?> clazz) {
		var jaulApp = clazz.getAnnotation(JaulApp.class);
		if (jaulApp == null)
			throw new IllegalArgumentException(
					MessageFormat.format("A registrable app must use the {0} annotation on the class {1}",
							JaulApp.class.getName(), clazz.getName()));
		return new JaulAppProvider() {
			
			@Override
			public String updatesUrl() {
				return jaulApp.updatesUrl();
			}
			
			@Override
			public String updaterId() {
				return jaulApp.updaterId();
			}
			
			@Override
			public String id() {
				return jaulApp.id();
			}
			
			@Override
			public AppCategory category() {
				return jaulApp.category();
			}
		};
	}
}
