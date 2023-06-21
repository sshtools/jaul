# JADAPTIVE Application Update Library

Adds update features to several Jadaptive projects via the Install4J runtime. Install4J itself is optional and only available in the EJ Technologies repository.

## Add Update Support To An Application

There are 4 main areas that will need changing to add multi-channel update support to an application.

 * Maven
 * Java (ie. the Application itself)
 * Install4J Project
 * Jenkins Build  

### Maven

First add some `<properties/>` tags.

```xml
<build.phase>continuous</build.phase>	
<build.mediaTypes>windows,unixInstaller,macos,macosFolder,windowsArchive,unixArchive,linuxRPM,linuxDeb,macosArchive,macosFolderArchive</build.mediaTypes>
<build.install4j.project>${project.basedir}/installer.install4j</build.install4j.project>
<build.projectProperties>${basedir}/jadaptive.build.properties</build.projectProperties>
<build.userProperties>${user.home}/.jadaptive.build.properties</build.userProperties>
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

You'll also need *jaul* itself. If this is a JavaFX application, hopefully you are already using `jajafx`, or if this is a command line tool hopefully you are already using `command-utils`. Either of these handle the bulk of the integration, you just need to provide some details and initiate the update or update check according to your user interface needs.

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
	<artifactId>properties-maven-plugin</artifactId>
</plugin>
<plugin>
	<groupId>org.codehaus.mojo</groupId>
	<artifactId>build-helper-maven-plugin</artifactId>
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
						<file>${build.projectProperties}</file>
						<file>${build.userProperties}</file>
					</files>
				</configuration>
			</execution>
		</executions>
	</plugin>
	<plugin>
		<groupId>org.codehaus.mojo</groupId>
		<artifactId>build-helper-maven-plugin</artifactId>
		<version>3.3.0</version>
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
	<plugin>
		<groupId>com.install4j</groupId>
		<artifactId>install4j-maven</artifactId>
		<version>10.0.4</version>
	</plugin>
	<plugin>
		<groupId>org.apache.maven.plugins</groupId>
		<artifactId>maven-antrun-plugin</artifactId>
		<version>1.8</version>
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
		<releases />
		<snapshots>
			<enabled>false</enabled>
		</snapshots>
	</pluginRepository>
</pluginRepositories>
```

Now add a new `<profile>` that is activated by the `buildInstaller` system property.

```xml
<profile>
	<id>install4j-installers</id>
	<activation>
		<property>
			<name>buildInstaller</name>
			<value>true</value>
		</property>
	</activation>
	<build>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-antrun-plugin</artifactId>
				<executions>
					<execution>
						<phase>package</phase>
						<configuration>
							<target>
								<copy file="${project.build.directory}/${project.artifactId}-${project.version}.jar" tofile="${project.build.directory}/${project.artifactId}.jar" />
							</target>
						</configuration>
						<goals>
							<goal>run</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-dependency-plugin</artifactId>
				<configuration>
					<outputDirectory>${project.build.directory}/dependencies</outputDirectory>
				</configuration>
				<executions>
					<execution>
						<id>copy-dependencies</id>
						<phase>package</phase>
						<goals>
							<goal>copy-dependencies</goal>
						</goals>
						<configuration>
							<stripVersion>true</stripVersion>
							<outputDirectory>${project.build.directory}/dependencies/common</outputDirectory>
							<stripVersion>true</stripVersion>
						</configuration>
					</execution>
				</executions>
			</plugin>
			<plugin>
				<groupId>com.install4j</groupId>
				<artifactId>install4j-maven</artifactId>
				<executions>
					<execution>
						<id>compile-installers</id>
						<phase>package</phase>
						<goals>
							<goal>compile</goal>
						</goals>
						<configuration>
							<variables>
								<build.phase>${build.phase}</build.phase>
							</variables>
							<release>${product.version}-${build.number}</release>
							<mediaTypes>${build.mediaTypes}</mediaTypes>
							<projectFile>${build.install4j.project}</projectFile>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
</profile>
```

### Java (ie. the Application itself)

How to do this will depend on the type of application. There are currently 3 major classes of application that can be integrated.

 * Command Line Application
 * JavaFX GUI Application
 * Everything else (e.g. SWT application). 
 
** Note, you will find the property `install4j.runtimeDir` useful. In a development environment (e.g. Eclipse), you can set this
property on a launcher to test update functionality. It must be the path to a real installation of the app, suffixed by `.install4j`, e.g.
`/opt/push-sftp/.install4j`. **
 
#### Command Line Application

Assuming you are using the `command-utils` module, you will be provide t

#### JavaFX GUI Application

TODO

#### Everything else

TODO

## Install4J Project

The Install4J project should be setup in the normal way, with the following additions.

 1. Create a *Compiler Variable* named `build.phase`. Give it a default value of `continuous`.
 1. You will need a launcher that can accept `--jaul-register` or `--jaul-deregister` arguments. You should have a `main()` class that can do this if you followed the above integration instructions. It is usually fine to re-use the launcher for the application, but some circumstances may require a dedicated lancher (e.g. service without a console mode). 
 1. The launcher should also have a *VM Parameter* configured. Add `-Dinstall4j.installationDir=${installer:sys.installationDir}`.  
 1. In *Screens and Actions* add a new *Run executable or batch file* **Action** at the very *end* of the *Installer*. Have it call the above the launcher, and pass the `--jaul-register` as an argument.
 1. In *Screens and Actions* add a new *Run executable or batch file* **Action** at the very *start* of the *Uninstaller*. Have it call the above the launcher, and pass the `--jaul-deregister` as an argument.
 1. In *Screens and Actions* add a new *Application*,  and choose **Standalond update donwloader**.
 1. Configure this application to have an *Executable Name* of `updater`. Change *Default execution mode* to *Unattended with progress dialog* and the title for the progress dialog to suit your needs.
 1. Take a note of the *ID* of the Standalone update download (you can turn on showing IDs in the *Project* menubar menu.
 1. Go to *Auto-Update Options* and add the *URL for updates XML*. This is the final public location where the `updates.xml` will be uploaded to and made available. It will contain the `build.phase  variable. For example, https://sshtools-public.s3.eu-west-1.amazonaws.com/push-sftp-gui/${compiler:build.phase}/updates.xml
 1. Select *Base URL for installers* and add the same URL, but without the `update.xml` and phase parts on the end, but instead the full version. For example, https://sshtools-public.s3.eu-west-1.amazonaws.com/push-sftp-gui/${compiler:sys,version}/. This URL *must* have a trailing `/`.
 
Save the project. Now add the ID you noted to the Java application's `@JaulApp` annotation.

You can now test a build locally.

```
mvn clean package -DbuildInstaller=true
```


## Jenkins Build  

TODO
