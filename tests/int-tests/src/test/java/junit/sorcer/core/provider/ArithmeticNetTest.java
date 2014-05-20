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
package junit.sorcer.core.provider;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import sorcer.core.provider.jobber.ServiceJobber;
import sorcer.junit.ExportCodebase;
import sorcer.junit.SorcerClient;
import sorcer.junit.SorcerRunner;
import sorcer.junit.SorcerServiceConfiguration;
import sorcer.service.Context;
import sorcer.service.Job;
import sorcer.service.Signature;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Flow;
import sorcer.service.Strategy.Wait;
import sorcer.service.Task;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.from;
import static sorcer.eo.operator.*;


/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerRunner.class)
@Category(SorcerClient.class)
@ExportCodebase({"org.sorcersoft.sorcer:sorcer-api",
        "org.sorcersoft.sorcer:ju-arithmetic-api"})
@SorcerServiceConfiguration(":ju-arithmetic-cfg-all")
public class ArithmeticNetTest {

	private final static Logger logger = Logger
			.getLogger(ArithmeticNetTest.class.getName());

	@Test
	public void arithmeticNodeTest() throws Exception {
		
		Task t5 = task(


				"t5",
				sig("add", Adder.class),
				context("add", in("arg, x1", 20.0),
						in("arg, x2", 80.0), result("result, y")));
		
		t5 = exert(t5);
		//logger.info("t5 dataContext: " + dataContext(t5));
		//logger.info("t5 value: " + get(t5));
		assertEquals("Wrong value for 100.0", 100.0, value(t5));
	}
	
	@Test
	public void arithmeticMultiServiceTest() throws Exception {
		
		Task t5 = task(
				"t5",
				sig("add", Arithmetic.class),
				context("add", in("arg, x1", 20.0),
						in("arg, x2", 80.0), result("result, y")));
		
		t5 = exert(t5);
		//logger.info("t5 dataContext: " + dataContext(t5));
		logger.info("t5 value: " + get(t5));
		assertEquals("Wrong value for 100.0", 100.0, get(t5));
	}
	
	@Test
	public void arithmeticSpaceTaskTest() throws Exception {
		Task t5 = task(
				"t5",
				sig("add", Adder.class),
				context("add", in("arg/x1", 20.0),
						in("arg/x2", 80.0), out("result/y", null)),
				strategy(Access.PULL, Wait.YES));
		
		logger.info("t5 init dataContext: " + context(t5));
		
		t5 = exert(t5);
		logger.info("t5 dataContext: " + context(t5));
		logger.info("t5 value: " + get(t5, "result/y"));
		assertEquals("Wrong result", 100.0, get(t5, "result/y"));
	}
	
	@Test
	public void exertJobPullParTest() throws Exception {
		Job job = createJob(Flow.PAR);
		job = exert(job);
		//logger.info("job j1: " + job);
		//logger.info("job j1 job dataContext: " + dataContext(job));
		logger.info("job j1 job dataContext: " + jobContext(job));
		//logger.info("job j1 value @ j1/t3/result/y = " + get(job, "j1/t3/result/y"));
		assertEquals(400.00, get(job, "j1/t3/result/y"));
	}
	
	@Test
	public void exertJobPullSeqTest() throws Exception {
		Job job = createJob(Flow.SEQ);
		job = exert(job);
		//logger.info("job j1: " + job);
		//logger.info("job j1 job dataContext: " + dataContext(job));
		logger.info("job j1 job dataContext: " + jobContext(job));
		//logger.info("job j1 value @ j1/t3/result/y = " + get(job, "j1/t3/result/y"));
		assertEquals(400.00, get(job, "j1/t3/result/y"));
	}
	
	// two level job composition with PULL and PAR execution
	private Job createJob(Flow flow) throws Exception {
		Task t3 = task("t3", sig("subtract", Subtractor.class), 
				context("subtract", in("arg/x1", null), in("arg/x2", null),
						out("result/y", null)));

		Task t4 = task("t4", sig("multiply", Multiplier.class), 
				context("multiply", in("arg/x1", 10.0), in("arg/x2", 50.0),
						out("result/y", null)));

		Task t5 = task("t5", sig("add", Adder.class), 
				context("add", in("arg/x1", 20.0), in("arg/x2", 80.0),
						out("result/y", null)));

		// Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
		//Job job = job("j1",
		return job("j1", //sig("service", RemoteJobber.class),
				//job("j2", t4, t5),
				job("j2", t4, t5, strategy(flow, Access.PULL)),
				t3,
				pipe(out(t4, "result/y"), in(t3, "arg/x1")),
				pipe(out(t5, "result/y"), in(t3, "arg/x2")));
	}

    // TODO - Problem with context value mappings
    //@Ignore
    @Test
    public void contexterTest() throws Exception {
        Task cxtt = task("addContext", sig("getContext", createContext()),
                context("add", input("arg/x1"), input("arg/x2")));

        Context result = context(exert(cxtt));
//		logger.info("contexter context: " + result);
        assertEquals(get(result, "arg/x1"), 20.0);
        assertEquals(get(result, "arg/x2"), 80.0);

    }

    // TODO - Problem with context value mappings
    //@Ignore
    @Test
    public void objectContexterTaskTest() throws Exception {
        Task t5 = task("t5", sig("add", AdderImpl.class),
                type(sig("getContext", createContext()), Signature.APD),
                context("add", in("arg/x1"), in("arg/x2"),
                        result("result/y")));

        Context result = context(exert(t5));
//		logger.info("task context: " + result);
        assertEquals(get(result, "result/y"), 100.0);
    }

    public static Context createContext() throws Exception {
        Context cxt = context("add", input("arg/x1", 20.0), input("arg/x2", 80.0));
        return  cxt;
    }

}
