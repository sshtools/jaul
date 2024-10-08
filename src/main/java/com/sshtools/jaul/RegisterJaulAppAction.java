package com.sshtools.jaul;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;

import com.install4j.api.Util;
import com.install4j.api.actions.AbstractInstallAction;
import com.install4j.api.context.InstallerContext;
import com.install4j.api.context.UserCanceledException;
import com.install4j.runtime.installer.helper.Logger;
import com.sshtools.jaul.AppRegistry.App;
import com.sshtools.jaul.UpdateDescriptor.MediaType;

@SuppressWarnings("serial")
public class RegisterJaulAppAction extends AbstractInstallAction implements JaulI4JAction {

	private static final String PREVIOUS_JAUL_REGISTRATION = "previousJaulRegistration";
	private String updatesBase = "${compiler:install4j.updatesBase}";
	private String updaterId = "${compiler:install4j.jaulUpdaterId}";
	private String jaulAppId = "${compiler:install4j.jaulAppId}";
	private String branches = "${compiler:install4j.jaulBranches}";
	private AppCategory appCategory;

	@Override
	public boolean install(InstallerContext context) throws UserCanceledException {
		return callSilent(Logger.getInstance(), () ->  {
		
			try {
				try {
					var app = getApp(context, getJaulAppId());
					if(app == null)
						Logger.getInstance().info(this, "No existing Jaul app with ID " + getJaulAppId());
					else
						context.setVariable(PREVIOUS_JAUL_REGISTRATION, app);
				}
				catch(Exception e) {
					Logger.getInstance().info(this, "Failed to get previous installation, assuming there wasn't one.");
				}
	
				Logger.getInstance().info(this, "Registering with Jaul (admin = " + Util.hasFullAdminRights() + "). Java home " + System.getProperty("java.home") );
				
					var callRegister = new CallRegister(getUpdatesBase() + "/${phase}/updates.xml", 
	                    getJaulAppId(), 
	                    getAppCategory(), 
	                    Integer.parseInt(getUpdaterId()), MediaType.INSTALLER, 
	                    context.getInstallationDirectory().getAbsolutePath(),
	                    false, /*!Util.hasFullAdminRights(), */
	                    AppRegistry.parseBranches(getBranches()));
					
//					if(Util.hasFullAdminRights()) {
		                callRegister.execute();
//		            }
//		            else {
//		                context.runElevated(callRegister, true);
//		            }
					
					Logger.getInstance().info(this, "Registered: with Jaul");
					
					
					if(!Util.isWindows()) {
						 fixPrefs(this);
					}
					
				return true;
			} catch (Exception e) {
				Logger.getInstance().error(this, e.getMessage());
				context.getProgressInterface().showFailure(MessageFormat.format("Failed to register application with Jaul, updates may not work. {0}", e.getMessage()));
			}
			return false;
		});
	}

	public static void fixPrefs(Object loggerSource) {
		/* Weird issue where the first install of ANY application results
		 * in no registration happening. E.g. when building a VM, the first
		 * Jaul installer in the list is never registered.
		 * 
		 * This is because on this first install, the embedded JDK is used, which
		 * is actually writing preferences to ... 
		 * 
		 * $JDK_HOME/jre/.systemPrefs/.....
		 */
		try {
			Path javahome = Paths.get(System.getProperty("java.home"));
			Path sysprefs = javahome.resolve(".systemPrefs");
			Path regprefs = sysprefs.resolve("com/sshtools/jaul/registry");
			Path etcprefs = Paths.get("/etc/.java/.systemPrefs/com/sshtools/jaul/registry");
			if(Files.exists(regprefs) && !Files.exists(etcprefs)) {
				Logger.getInstance().info(loggerSource, "Activating preferences work-around, copying " + regprefs + " to /etc/.java");
				Files.createDirectories(etcprefs);
				Files.walkFileTree(regprefs, new SimpleFileVisitor<Path>() {
			        @Override
			        public FileVisitResult preVisitDirectory(final Path dir,
			                final BasicFileAttributes attrs) throws IOException {
			            Files.createDirectories(etcprefs.resolve(regprefs
			                    .relativize(dir)));
			            return FileVisitResult.CONTINUE;
			        }
	
			        @Override
			        public FileVisitResult visitFile(final Path file,
			                final BasicFileAttributes attrs) throws IOException {
			            Files.copy(file,
			            		etcprefs.resolve(regprefs.relativize(file)));
			            return FileVisitResult.CONTINUE;
			        }
			    });
				Logger.getInstance().info(loggerSource, "Activated preferences work-around, copying " + regprefs + " to /etc/.java");
			}
		}
		catch(Exception e) {
			Logger.getInstance().error(loggerSource, "Failed preferences work-around. " + e.getMessage());
		}
	}

	@Override
    public void rollback(InstallerContext context) {
    	App was = (App)context.getVariable(PREVIOUS_JAUL_REGISTRATION);
    	if(was == null) {
//    		if(Util.hasFullAdminRights()) {
				new CallDeregister(getJaulAppId()).execute();
//			}
//			else {
//				context.runElevated(new CallDeregister(getJaulAppId()), true);
//			}
    	}
    	else {
//    		if(Util.hasFullAdminRights()) {
				new CallRegister(was.getUpdatesUrl().get(), getJaulAppId(), 
						was.getCategory(), Integer.parseInt(was.getLauncherId()), MediaType.INSTALLER, 
						was.getDir().toString(), false).execute();
//			}
//			else {
//				context.runElevated(new CallRegister(getUpdatesBase() + "/${phase}/updates.xml", getJaulAppId(), getAppCategory(), Integer.parseInt(getUpdaterId()), MediaType.INSTALLER, context.getInstallationDirectory().getAbsolutePath(), true), true);
//			}
    	}
    }

	public String getUpdaterId() {
		return replaceWithTextOverride("updaterId", replaceVariables(this.updaterId), String.class);
	}

	public void setUpdaterId(String updaterId) {
		this.updaterId = updaterId;
	}

	public String getJaulAppId() {
		return replaceWithTextOverride("jaulAppId", replaceVariables(this.jaulAppId), String.class);
	}

	public void setJaulAppId(String jaulAppId) {
		this.jaulAppId = jaulAppId;
	}

	public String getBranches() {
		return replaceWithTextOverride("branches", replaceVariables(this.branches), String.class);
	}

	public void setBranches(String branches) {
		this.branches = branches;
	}

	public AppCategory getAppCategory() {
		return appCategory;
	}

	public void setAppCategory(AppCategory appCategory) {
		this.appCategory = appCategory;
	}

	public String getUpdatesBase() {
		return replaceWithTextOverride("updatesBase", replaceVariables(this.updatesBase), String.class);
	}

	public void setUpdatesBase(String updatesBase) {
		this.updatesBase = updatesBase;
	}
	
	private App getApp(InstallerContext context, String id) {
		try {
//			if(Util.hasFullAdminRights()) {
//				Logger.getInstance().info(this, "Getting app directly, we are already admin");
				return (App)new CallGet(id).execute();
//			}
//			else {
//				Logger.getInstance().info(this, "Elevating to get app, we are not admin");
//				return (App)context.runElevated(new CallGet(id), true);
//			}
		}
		catch(Exception e) {
			return null;
		}
	}
}
