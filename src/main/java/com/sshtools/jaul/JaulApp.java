package com.sshtools.jaul;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JaulApp  {

	String id();
	
	AppCategory category();
	
	String updatesUrl();
	
	String updaterId();
}