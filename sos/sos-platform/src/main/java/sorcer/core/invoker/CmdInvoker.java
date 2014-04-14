/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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

package sorcer.core.invoker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.Arrays;

import sorcer.core.context.model.par.Par;
import sorcer.service.Arg;
import sorcer.service.ArgSet;
import sorcer.service.ContextException;
import sorcer.service.EvaluationException;
import sorcer.util.exec.ExecUtils;
import sorcer.util.exec.ExecUtils.CmdResult;
import sorcer.util.exec.NullInputStream;

/**
 * @author Mike Sobolewski
 */

@SuppressWarnings({ "rawtypes", "unchecked" })
public class CmdInvoker extends Invoker implements CmdInvoking {

	private static final long serialVersionUID = -4035189000192693692L;
	
	private String cmd;
	private String[] cmdarray;
	private File scriptFile;
	private File logFile;
	private InputStream stdin;
	private boolean background = false;

	{
		defaultName = "cmdInvoker-";
	}
	
	public CmdInvoker(String cmd) {
		super();
		this.cmd = cmd;
	}
	
	public CmdInvoker(String name, String cmd, Par... pars) {
		super(name);
		this.cmd = cmd;
		this.pars = new ArgSet(pars);
	}

	public CmdInvoker(String[] cmdarray, Par... pars) {
		this.cmdarray = cmdarray;
		this.pars = new ArgSet(pars);
	}

	public CmdInvoker(String shcmd, File scriptFile, boolean background,
			File logFile, Par... pars) throws EvaluationException {
		cmd = shcmd;
		this.scriptFile = scriptFile;
		this.logFile = logFile;
		this.background = background;
		this.pars = new ArgSet(pars);
		if (!scriptFile.canExecute()) {
			scriptFile.setExecutable(true);
			logger.warning("script exec permission corrected for: " + scriptFile);
		}
	}
		
	/**
	 * Creates and executed the script in the file <code>scriptFile</code>
	 * 
	 * @param cmdarray
	 *            For example new String[] { "csh", "-f", "-c" };
	 * @param script
	 *            The content of the script to be executed
	 * @param background
	 *            true if the script to be run in background
	 * @param stdin
	 *            The standard input for the system process
	 * @param logFile
	 *            The standard output for the system process
	 * @throws EvaluationException
	 */
	public CmdInvoker(String argarray[], File script, boolean background,
			InputStream stdin, File logFile, Par... pars) throws EvaluationException {
		cmdarray = new String[argarray.length + 1];
		this.scriptFile = script;
		this.pars = new ArgSet(pars);
		if (!scriptFile.canExecute()) {
			scriptFile.setExecutable(true);
			logger.warning("script exec permission corrected for: " + scriptFile);
		}

		System.arraycopy(argarray, 0, cmdarray, 0, argarray.length);

		cmdarray[cmdarray.length - 1] = scriptFile.getAbsolutePath();
		this.stdin = stdin;
		this.logFile = logFile;
		this.background = background;

	}

	/**
	 * Creates and executed the script in the file <code>scriptFile</code>
	 * 
	 * @param cmdarray
	 *            For example new String[] { "csh", "-f", "-c" };
	 * @param script
	 *            The content of the script to be executed
	 * @param background
	 *            true if the script to be run in background
	 * @param logFile
	 *            The standard output for the system process
	 * @throws EvaluationException
	 */
	public CmdInvoker(String argarray[], File scriptFile, boolean background,
			File logFile) throws EvaluationException {
		this(argarray, scriptFile, background, null, logFile);
	}
	
	/**
	 * Feed specified standard input to the command executing process.
	 */
	public CmdInvoker(String cmd, InputStream stdin)
			throws EvaluationException {
		this(cmd);
		this.stdin = stdin;
	}


	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#getValue(sorcer.service.Arg[])
	 */
	@Override
	public CmdResult getValue(Arg... entries) throws EvaluationException,
			RemoteException {
		CmdResult out = null;
		if (scriptFile != null) {
			try {
				return execScript();
			} catch (Exception se) {
				throw new EvaluationException("Script invocation failed: "
						+ cmd);
			}
		} else {
			try {
				if (cmd == null && cmdarray == null)
					throw new EvaluationException("No args for CmdEvaluator!");

				if (cmd != null) {
					if (stdin != null) {
						out = ExecUtils.execCommand(Runtime.getRuntime().exec(
								cmd), stdin);      
					} else {
						out = ExecUtils.execCommand(cmd);
					}
				} else if (cmdarray != null) {
					if (stdin != null) {
						out = ExecUtils.execCommand(Runtime.getRuntime().exec(
								cmdarray), stdin);
					} else {
						out = ExecUtils.execCommand(cmdarray);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new EvaluationException("Command invocation failed: "
						+ Arrays.toString(cmdarray));
			}
		}
		return out;
	}

	public CmdResult execScript() throws IOException, InterruptedException, ContextException {
		if (cmdarray != null) {
			StringBuilder sb = new StringBuilder(cmdarray[0]);
			for (int i = 1; i < cmdarray.length; i++)
				sb.append(" ").append(cmdarray[i]);
			if (background)
				sb.append(" &");
			cmd = sb.toString();
		} else {
			cmd = cmd + " " + scriptFile.getAbsolutePath();
			if (background)
				cmd = cmd + " &";
		}

		logger.info("executing script: " + cmd);

		final Process process = Runtime.getRuntime().exec(cmd);				
		final PrintWriter logOut = new PrintWriter(logFile);
		Thread scriptLogger = new Thread(new Runnable() {
			public void run() {
				String line;
				BufferedReader in = new BufferedReader(new InputStreamReader(
						process.getInputStream()));
				try {
					while ((line = in.readLine()) != null) {
						logOut.println(line);
						if (logOut.checkError()) {
							System.err
									.println("scipt exec log file encountered check error"
											+ logFile);
						}
						logOut.flush();
					}
				} catch (IOException e) {
					System.err
							.println("scipt exec log file encountered IO error"
									+ logFile);
				}
			}
		}, Thread.currentThread().getName()+"-STDIN-"+process.toString());
		scriptLogger.start();
		CmdResult result = null;
		if (stdin != null)
			result = ExecUtils.execCommand(process, stdin, true);
		else
			result = ExecUtils
					.execCommand(process, new NullInputStream(), true);

		logOut.close();
		logger.info(Arrays.toString(cmdarray) + " completed with status = "
				+ process.exitValue());
		return result;
	}

}
