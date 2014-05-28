/**
 *
 * Copyright 2013 the original author or authors.
 * Copyright 2013 Sorcersoft.com S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sorcer.tools.shell.cmds;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import net.jini.admin.Administrable;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.discovery.LookupDiscovery;
import sorcer.tools.shell.INetworkShell;
import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;

import com.sun.jini.admin.DestroyAdmin;
import sorcer.util.WhitespaceTokenizer;

public class StartStopCmd extends ShellCmd {
	{
		COMMAND_NAME = "start";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "start <application name>";
        //or stop <registrar index> | all";

		COMMAND_HELP = "Start application using ant script or shell script. ";
			//+ "\nStop a single lookup service or all lookup services.";
		
	}

	private PrintStream out;

	private String input;

	public StartStopCmd() {
	}

	public void execute() throws Throwable {
		INetworkShell shell = NetworkShell.getInstance();
		out = NetworkShell.getShellOutputStream();
		LookupDiscovery ld = NetworkShell.getDisco();
        WhitespaceTokenizer myTk = NetworkShell.getShellTokenizer();
		input = shell.getCmd();
		if (out == null)
			throw new NullPointerException("Must have an output PrintStream");
		if (input.startsWith("start")) {
			String app = myTk.nextToken();
			if (NetworkShell.getAppMap().containsKey(app)) {
				String path = NetworkShell.getAppMap().get(app);
				NetworkShell.startApplication(path);
			} else {
				out.print("No such application " + app);
			}
		}
	}

	public String getUsage(String subCmd) {
		if (subCmd.equals("start")) {
			return "start <application name>";
		} else if (subCmd.equals("stop")) {
			return "stop <lookup service index> | all";
		} else {
			return COMMAND_USAGE;
		}
	}
}
