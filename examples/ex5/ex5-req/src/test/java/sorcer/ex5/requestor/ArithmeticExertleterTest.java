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
package sorcer.ex5.requestor;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.SorcerEnv;
import sorcer.core.context.PositionalContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.exertion.NetTask;
import sorcer.core.signature.NetSignature;
import sorcer.junit.*;
import sorcer.service.Context;
import sorcer.service.Evaluation;
import sorcer.service.Invocation;
import sorcer.service.Task;
import sorcer.util.StringUtils;
import sorcer.util.exec.ExecUtils;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes" })
@RunWith(SorcerSuite.class)
@Category(SorcerClient.class)
@ExportCodebase({
        "org.sorcersoft.sorcer:sorcer-api",
        "org.sorcersoft.sorcer:ex5-api"
})
@SorcerServiceConfigurations({
        @SorcerServiceConfiguration({
                ":ex5-cfg-adder",
                ":ex5-cfg-multiplier",
                ":ex5-cfg-subtractor",
                ":ex5-cfg-divider",
                ":ex5-job"
        }),
        @SorcerServiceConfiguration({
                ":ex5-cfg-all",
                ":ex5-job"
        }),
        @SorcerServiceConfiguration({
                ":ex5-cfg-one-bean",
                ":ex5-job"
        })
})
public class ArithmeticExertleterTest {

	private final static Logger logger = LoggerFactory
			.getLogger(NetArithmeticReqTest.class);

    @BeforeClass
    public static void before() throws IOException, InterruptedException {
        String baseCmd = new StringBuilder(new java.io.File(SorcerEnv.getHomeDir(),
                "bin"+ java.io.File.separator + "nsh").getCanonicalPath()).toString();
        String[] cmds = new String[] { baseCmd, "-c", "lup", "-s"};

        ExecUtils.CmdResult result = ExecUtils.execCommand(cmds);
        String res =  result.getOut();
        logger.info("Services running:\n" + res);
    }


    public Object exertExertleterReturnValue() throws Exception {

        // invocation with complete contexts
        Context addContext = new PositionalContext("add");
        addContext.putInValue("arg1/value", 90.0);
        addContext.putInValue("arg2/value", 110.0);

        Context multiplyContext = new PositionalContext("multiply");
        multiplyContext.putInValue("arg1/value", 10.0);
        multiplyContext.putInValue("arg2/value", 70.0);

        ServiceContext invokeContext = new ServiceContext("invoke");
        invokeContext.putLink("add", addContext, "");
        invokeContext.putLink("multiply", multiplyContext, "");

        NetSignature signature = new NetSignature("invoke", Invocation.class);

        NetTask task = new NetTask("invoke", signature, invokeContext);
        Task result = (Task) task.exert();
        Context out = result.getContext();

        logger.info("1job1task/subtract/result/value: " + out.getValue("1job1task/subtract/result/value"));
        return out.getValue("1job1task/subtract/result/value");
    }


    @Ignore("Exertleter is not supported - sometimes works sometimes not...")
    @Test
    public void exertExertleterNTimes() throws Exception {
        int n = 5;
        Thread.sleep(3000);
        // invoke exertleter with the current contexts

        for (int i=0;i<n;i++) {
            logger.info("Running " +  i + "/" + n + " time");
            assertEquals(500.0, exertExertleterReturnValue());
            exertArithmeticExertleter();
        }
   }


    //ArithmeticExertleterTest.exertArithmeticExertleter:100 expected:<400.0> but was:<500.0>
    //@Test
    public void exertArithmeticFirstExertleter() throws Exception {

        // invocation with complete contexts
        Context addContext = new PositionalContext("add");
        addContext.putInValue("arg1/value", 90.0);
        addContext.putInValue("arg2/value", 110.0);

        Context multiplyContext = new PositionalContext("multiply");
        multiplyContext.putInValue("arg1/value", 10.0);
        multiplyContext.putInValue("arg2/value", 70.0);

        ServiceContext invokeContext = new ServiceContext("invoke");
        invokeContext.putLink("add", addContext, "");
        invokeContext.putLink("multiply", multiplyContext, "");

        NetSignature signature = new NetSignature("invoke", Invocation.class);

        NetTask task = new NetTask("invoke", signature, invokeContext);
        Task result = (Task) task.exert();
        Context out = result.getContext();
//		logger.info("result context: " + out);

        logger.info("1job1task/subtract/result/value: " + out.getValue("1job1task/subtract/result/value"));
        assertEquals(500.0, out.getValue("1job1task/subtract/result/value"));
    }


    //ArithmeticExertleterTest.exertArithmeticExertleter:100 expected:<400.0> but was:<500.0>
    //@Test
	public void exertArithmeticExertleter() throws Exception {

        // invocation with complete contexts
        Context addContext = new PositionalContext("add");
		addContext.putInValue("arg1/value", 90.0);
		addContext.putInValue("arg2/value", 110.0);
		
		Context multiplyContext = new PositionalContext("multiply");
		multiplyContext.putInValue("arg1/value", 10.0);
		multiplyContext.putInValue("arg2/value", 70.0);

		ServiceContext invokeContext = new ServiceContext("invoke");
		invokeContext.putLink("add", addContext, "");
		invokeContext.putLink("multiply", multiplyContext, "");

        NetSignature signature = new NetSignature("invoke", Invocation.class);
		
	    NetTask task = new NetTask("invoke", signature, invokeContext);
        Task result = (Task)task.exert();
		Context out = result.getContext();
//		logger.info("result context: " + out);

		logger.info("1job1task/subtract/result/value: " + out.getValue("1job1task/subtract/result/value"));
		assertEquals(500.0, out.getValue("1job1task/subtract/result/value"));


        // invocation with subcontexts
        addContext = new PositionalContext("add");
        addContext.putInValue("arg1/value", 80.0);

        multiplyContext = new PositionalContext("multiply");
        multiplyContext.putInValue("arg1/value", 20.0);

        invokeContext = new ServiceContext("invoke");
        invokeContext.putLink("add", addContext, "");
        invokeContext.putLink("multiply", multiplyContext, "");

        signature = new NetSignature("invoke", Invocation.class);

        task = new NetTask("invoke", signature, invokeContext);
        result = (Task)task.exert();
        out = result.getContext();
//		logger.info("result context: " + out);

        logger.info("1job1task/subtract/result/value: " + out.getValue("1job1task/subtract/result/value"));
        assertEquals(1210.0, out.getValue("1job1task/subtract/result/value"));


        // reset the initial context values
        addContext = new PositionalContext("add");
        addContext.putInValue("arg1/value", 20.0);
        addContext.putInValue("arg2/value", 80.0);

        multiplyContext = new PositionalContext("multiply");
        multiplyContext.putInValue("arg1/value", 10.0);
        multiplyContext.putInValue("arg2/value", 50.0);

        invokeContext = new ServiceContext("invoke");
        invokeContext.putLink("add", addContext, "");
        invokeContext.putLink("multiply", multiplyContext, "");

        signature = new NetSignature("invoke", Invocation.class);

        task = new NetTask("invoke", signature, invokeContext);
        result = (Task)task.exert();
        out = result.getContext();
//		logger.info("result context: " + out);

        logger.info("1job1task/subtract/result/value: " + out.getValue("1job1task/subtract/result/value"));
        assertEquals(400.0, out.getValue("1job1task/subtract/result/value"));



        // invoke exertleter with the current contexts
        signature = new NetSignature("getValue", Evaluation.class);
        task = new NetTask("eval", signature);
        result = (Task)task.exert();
        out = (Context)result.getReturnValue();

        logger.info("out context: " + out);

        logger.info("1job1task/subtract/result/value: "
                + out.getValue("1job1task/subtract/result/value"));
        assertEquals(400.0, out.getValue("1job1task/subtract/result/value"));
	}

}
