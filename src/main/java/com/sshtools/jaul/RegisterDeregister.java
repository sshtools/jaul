package com.sshtools.jaul;

import java.util.Optional;
import java.util.concurrent.Callable;

import com.sshtools.jaul.UpdateDescriptor.MediaType;

public class RegisterDeregister implements Callable<Integer> {


	@Override
	public final Integer call() throws Exception {
		throw new IllegalArgumentException("Missing sub-command");
	}

	public final static class Register implements Callable<Integer> {
		
		private Optional<MediaType> packaging;
		private String appId;
		private AppCategory category;
		private String updaterId;
		private String updatesUrl;

		@Override
		public final Integer call() throws Exception {
			var appReg = JaulAppProvider.fromStatic(appId, category, updatesUrl, updaterId);
			if (category == null || updaterId == null || updatesUrl == null) {
				throw new IllegalArgumentException(
						"Category, Updater ID and Updates URL must be provided for registration.");
			}
			AppRegistry.get().register(appReg, packaging.orElse(MediaType.INSTALLER));
			return 0;
		}
	}

	public final static class Deregister implements Callable<Integer> {

		//@Parameters(index = "0", description = "The Jaul application ID, e.g. com.sshtools.MyApp", arity = "1")
		private String appId;

		@Override
		public final Integer call() throws Exception {
			AppRegistry.get().deregister(appId);
			return 0;
		}
	}

	public static void main(String[] args) throws Exception {
		if(args.length == 0 || ( args.length > 0 && (args[0].equals("-h") || args[0].equals("--help")))) {
			printUsage();
			System.exit(1);
		}
		try {
			if(args[0].equals("--jaul-register")) {
				var cmd = new Register();
				if(args.length == 7) {
					if(args[1].equals("--packaging")) {
						cmd.packaging = Optional.of(MediaType.valueOf(args[2].toUpperCase()));
						cmd.appId = args[3];
						cmd.category = AppCategory.valueOf(args[4].toUpperCase());
						cmd.updaterId = args[5];
						cmd.updatesUrl = args[6];
						System.exit(cmd.call());
					}
					else 
						throw new IllegalArgumentException("Invalid option.");
				}
				else if(args.length != 5) {
					throw new IllegalArgumentException("Incorrect number of arguments.");
				}
				else {
					cmd.appId = args[1];
					cmd.category = AppCategory.valueOf(args[2].toUpperCase());
					cmd.updaterId = args[3];
					cmd.updatesUrl = args[4];
					System.exit(cmd.call());
				}
			}
			else if(args[0].equals("--jaul-deregister")) {
				if(args.length != 2) {
					throw new IllegalArgumentException("Incorrect number of arguments.");
				}

				var cmd = new Deregister();
				cmd.appId = args[1];
				System.exit(cmd.call());
			}
			else {
				throw new IllegalArgumentException("Invalid option " + args[0]);
			}
		}
		catch(IllegalArgumentException iae) {
			System.out.println(iae.getMessage());
			System.err.println();
			printUsage();
			System.exit(1);
		}
	}
	
	private static void printUsage() {
		System.err.println("jaul-registration: [--jaul-register [--packaging <packaging>] <appId> <category> <updateId> <updatesUrl> | --jaul-deregister ]");
	}
}
