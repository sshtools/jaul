module com.sshtools.jaul {
	requires transitive com.install4j.runtime;
	requires transitive org.slf4j;
	requires transitive java.prefs;
	requires java.xml;
	opens com.sshtools.jaul;
	exports com.sshtools.jaul;
}