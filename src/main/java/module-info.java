module com.sshtools.jaul {
	requires transitive com.install4j.runtime;
	requires transitive org.slf4j;
	opens com.sshtools.jaul;
	exports com.sshtools.jaul;
}