This is a test for VFS update.

The test xlet (VFSUpdateXlet) downloading a new 00000.jar to replace itself after the update.

To try this test:

1. Before building, adjust the variables in build.properties.
2. Open src/VFSUpdateXlet/hellotvxlet/HelloTVXlet.java and adjust the HOSTDIR value.
3. The disc image is created in the "dist/DiscImage" dir.  

Note that one might need to install jsch.jar 0.1.29 or later (http://www.jcraft.com/jsch/index.html) for the ant's scp task.  
For more information, see http://ant.apache.org/manual/OptionalTasks/scp.html.