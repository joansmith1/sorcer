package sorcer.ex2.requestor;

import java.net.InetAddress;
import java.rmi.RMISecurityManager;
import java.util.logging.Logger;

import sorcer.core.context.ServiceContext;
import sorcer.core.exertion.NetTask;
import sorcer.core.signature.NetSignature;
import sorcer.service.Context;
import sorcer.service.Exertion;
import sorcer.service.Signature;
import sorcer.service.Task;
import sorcer.util.Log;
import sorcer.util.Sorcer;

public class WorkerTaskRequestor {

	private static Logger logger = Log.getTestLog();

	public static void main(String[] args) throws Exception {
		System.setSecurityManager(new RMISecurityManager());
		// initialize system properties
		Sorcer.getEnvProperties();

		// get the queried provider name from the command line
		String pn = null;
		if (args.length == 1)
			pn = Sorcer.getSuffixedName(args[0]);

		logger.info("Provider name: " + pn);

		Exertion exertion = new WorkerTaskRequestor().getExertion(pn);
		Exertion result = exertion.exert();
		logger.info("Output context: \n" + result.getContext());
	}

	private Exertion getExertion(String pn) throws Exception {
		String hostname = InetAddress.getLocalHost().getHostName();

		Context context = new ServiceContext("work");
		context.putValue("requstor/name", hostname);
		context.putValue("requestor/operand/1", 1);
		context.putValue("requestor/operand/2", 1);
		context.putValue("to/provider/name", pn);

		NetSignature signature = new NetSignature("doWork",
				sorcer.ex2.provider.Worker.class, pn);

		Task task = new NetTask("work", signature, context);

		return task;
	}
}