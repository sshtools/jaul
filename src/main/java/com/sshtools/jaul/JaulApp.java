package com.sshtools.jaul;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JaulApp  {

	String id();
	
	/**
	 * Deprecated. These details should be registered at install time by either
	 * the custom Install4J actions or the Jaul utility classes.
	 * 
	 * @return category
	 */
	@Deprecated(since = "0.9.11")
	AppCategory category();

	/**
	 * Deprecated. These details should be registered at install time by either
	 * the custom Install4J actions or the Jaul utility classes.
	 * 
	 * @return category
	 */
	@Deprecated(since = "0.9.11")
	String updatesUrl();

	/**
	 * Deprecated. These details should be registered at install time by either
	 * the custom Install4J actions or the Jaul utility classes.
	 * 
	 * @return category
	 */
	@Deprecated(since = "0.9.11")
	String updaterId();
}