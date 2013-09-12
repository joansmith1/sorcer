package sorcer.ex0;

import static sorcer.eo.operator.context;
import static sorcer.eo.operator.exert;
import static sorcer.eo.operator.in;
import static sorcer.eo.operator.out;
import static sorcer.eo.operator.path;
import static sorcer.eo.operator.sig;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.trace;
import static sorcer.eo.operator.task;

import java.rmi.RMISecurityManager;
import java.util.logging.Logger;

import sorcer.core.SorcerEnv;
import sorcer.core.requestor.ServiceRequestor;
import sorcer.core.SorcerConstants;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.ContextException;
import sorcer.service.SignatureException;
import sorcer.service.Task;
import sorcer.util.Log;

public class HelloWorldTester extends ServiceRequestor {

private static Logger logger = Log.getTestLog();

public Exertion getExertion(String... args) throws ExertionException,
        ContextException, SignatureException {
		System.setSecurityManager(new RMISecurityManager());
		logger.info("Starting HelloWorldTester");
		
		Task t1 = task("hello", sig("sayHelloWorld", HelloWorld.class),
				   context("Hello", in(path("in", "value"), "TESTER"), out(path("out", "value"), null)));
		
		logger.info("Task t1 prepared: " + t1);
		Exertion out = exert(t1);

		logger.info("Got result: " + get(out, "out/value"));
		logger.info("----------------------------------------------------------------");
		logger.info("Task t1 trace: " +  trace(out));
        return out;
	}
}



