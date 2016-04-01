package com.ibm.zos.svt.steps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.List;

import com.ibm.zos.svt.Logger;
import com.ibm.zos.svt.units.Unit;

/**
 * Step of execute executable file
 * @author Stone WANG
 *
 */
public class Acl extends AbstractStep{
	private String[] parms = null;

	/**
	 * Create a new Acl
	 * @param location	The step location
	 * @param parent	The parent unit
	 */
	public Acl(String location, Unit parent) {
		logger = Logger.getInstance(location);
	}
	
	/**
	 * Run the step
	 * @param params The parameters of the step
	 * @return True if success, otherwise false
	 */
	public boolean run(String params) {
		String[] parms = params.split(" +");
		
		if((parms == null) || (parms.length < 2)) {
			logger.error("Invalid parameters for Exec Step:" + params + ".");
			logger.error("Executable file path must be provided.");
			return false;
		}
		this.parms = parms;
		String fileName = parms[0];
		String userName = parms[1];
		
		Path path = new File(fileName).toPath();
		UserPrincipal guest;
		try {
			guest = path.getFileSystem().getUserPrincipalLookupService()
					.lookupPrincipalByName(userName);
		
			// get view
			AclFileAttributeView view = Files.getFileAttributeView(path, AclFileAttributeView.class);
			// create ACE to give "joe" read access
			AclEntry entry = AclEntry.newBuilder()
					.setType(AclEntryType.ALLOW)
					.setPrincipal(guest)
					.setPermissions(AclEntryPermission.READ_DATA, AclEntryPermission.READ_ATTRIBUTES)
					.build();
			// read ACL, insert ACE, re-write ACL
			List<AclEntry> acl = view.getAcl();
			acl.add(0, entry);   // insert before any DENY entries
			view.setAcl(acl);
			System.out.println(acl.get(1).type().toString());
			System.out.println(acl.get(1).permissions().toArray()[0].toString());
			System.out.println(acl.get(1).permissions().toArray()[1].toString());
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
		return true;
	}

	/**
	 * Validate the step
	 * @return True if success, otherwise false
	 */
	public boolean validate() {
		logger.norm("Acl run successfully.");
		return true;
	}
}
