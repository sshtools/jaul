# JAdaptive Application Update Library

Adds update features to several JAdaptive projects via the Install4J runtime. Install4J itself is optional and only available in the EJ Technologies repository.

## Add Update Support To An Application

There are 4 main areas that will need changing to add multi-channel update support to an application.

### Maven

First add some `<properties/>` tags.

```xml
<build.phase>continuous</build.phase>	
<build.mediaTypes>windows,unixInstaller,macos,macosFolder,windowsArchive,unixArchive,linuxRPM,linuxDeb,macosArchive,macosFolderArchive</build.mediaTypes>
```

Now you'll need a `<dependency/>`.

```xml

<dependency>
	<groupId>com.install4j</groupId>
	<artifactId>install4j-runtime</artifactId>
	<version>10.0.4</version>
	<!-- MUST be provided, the runtime is added by I4J itself -->
	<scope>provided</scope>
</dependency>
```

You'll also *jaul* need itself. If this is a JavaFX application, hopefully you are already using `jajafx`, or if this is a command line tool hopefully you are already using `command-utils`. Either of these handle the bulk of the integration, you just need to provide some details and initiate the update or update check according to your user interface needs.

So, if you are not already using either of the framework libraries, add another dependency.

```xml
<dependency>
	<groupId>com.sshtools</groupId>
	<artifactId>jaul</artifactId>
	<version>0.0.2-SNAPSHOT</version>
</dependency>
```

Now you'll need to add some plugins. First off, copy and paste the following into a `<build>` / `<plugins>` section.

```xml

<plugin>
	<groupId>org.codehaus.mojo</groupId>
	<artifactId>build-helper-maven-plugin</artifactId>
	<executions>
		<!-- The build number. This will be set to ZERO if BUILD_NUMBER is not 
			set. Jenkins will set BUILD_NUMBER, or you can set it in the environment 
			before running maven for build testing. -->
		<execution>
			<id>build-number-property</id>
			<goals>
				<goal>regex-property</goal>
			</goals>
			<phase>initialize</phase>
			<configuration>
				<!-- Set build.number to zero if it is blank. This uses a bit of regular 
					expression trickery. Because Jenkins supplies BUILD_NUMBER as an environment 
					variable, and we also want the build to work outside of Jenkins, then the 
					below is used to set build.number to zero if BUILD_NUMBER is not set. There 
					is no easy way to do this with basic Maven, and build-helper doesn't like 
					empty variables either, so we prefix it with zero, then use capture groups 
					to correct the format for both situations -->
				<name>product.version</name>
				<value>${project.version}</value>
				<regex>^([0-9]+)\.([0-9]+)\.([0-9]+)-([0-9A-Za-z]+)$</regex>
				<replacement>$1.$2.$3</replacement>
				<failIfNoMatch>false</failIfNoMatch>
			</configuration>
		</execution>
		<execution>
			<id>product-version-property</id>
			<goals>
				<goal>regex-property</goal>
			</goals>
			<phase>initialize</phase>
			<configuration>
				<!-- Strip off -SNAPSHOT (or other suffix) -->
				<name>build.number</name>
				<value>0${env.BUILD_NUMBER}</value>
				<regex>^(?:0?)([0-9]+)(?:\$\{env\.BUILD_NUMBER\})?$</regex>
				<replacement>$1</replacement>
				<failIfNoMatch>false</failIfNoMatch>
			</configuration>
		</execution>
	</executions>
</plugin>
```

And then the following into `<build>` / `<pluginManagement>` / `<plugins>`. 

```xml
<plugins>
	<plugin>
		<groupId>org.codehaus.mojo</groupId>
		<artifactId>versions-maven-plugin</artifactId>
		<version>2.15.0</version>
	</plugin>
	<plugin>
		<groupId>org.codehaus.mojo</groupId>
		<artifactId>properties-maven-plugin</artifactId>
		<version>1.0.0</version>
		<executions>
			<execution>
				<phase>initialize</phase>
				<goals>
					<goal>read-project-properties</goal>
				</goals>
				<configuration>
					<quiet>true</quiet>
					<files>
						<file>${basedir}/jadaptive.build.properties</file>
						<file>${user.home}/.jadaptive.build.properties</file>
					</files>
				</configuration>
			</execution>
		</executions>
	</plugin>
	<plugin>
		<groupId>com.install4j</groupId>
		<artifactId>install4j-maven</artifactId>
		<version>9.0.5</version>
	</plugin>
	<plugin>
		<groupId>org.codehaus.mojo</groupId>
		<artifactId>build-helper-maven-plugin</artifactId>
		<version>3.3.0</version>
	</plugin>
</plugins>
```

You'll also need some `<repositories/>` and `<pluginRepositories>`.

```xml
<repositories>
	<repository>
		<id>oss-snapshots</id>
		<url>https://oss.sonatype.org/content/repositories/snapshots</url>
		<snapshots />
		<releases>
			<enabled>false</enabled>
		</releases>
	</repository>
	<repository>
		<id>ej-technologies</id>
		<url>https://maven.ej-technologies.com/repository</url>
	</repository>
</repositories>
<pluginRepositories>
	<pluginRepository>
		<id>ej-technologies</id>
		<url>https://maven.ej-technologies.com/repository</url>
	</pluginRepository>
</pluginRepositories>
```

### Java (ie. the Application itself)

TODO

## Install4J Project

TODO

## Jenkins Build  

TODO