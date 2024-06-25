module com.sshtools.jaul {
	requires static transitive com.install4j.runtime;
	requires transitive java.prefs;
	requires java.xml;
	requires java.net.http;
	requires java.logging;
	opens com.sshtools.jaul;
	exports com.sshtools.jaul;
}