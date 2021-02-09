package com.vesoft;

import com.vesoft.nebula.client.graph.NebulaPoolConfig;
import com.vesoft.nebula.client.graph.data.HostAddress;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import com.vesoft.nebula.client.graph.net.Session;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.assertions.AssertionResult;


/**
 * LDBC Go Step
 */
public class LdbcGoStep implements JavaSamplerClient {
    private static final Logger log = LoggerFactory.getLogger(LdbcGoStep.class);
    private NebulaPool pool = null;
    private Session session = null;


    @Override
    public Arguments getDefaultParameters() {
        Arguments arguments = new Arguments();
        arguments.addArgument("hosts", "127.0.0.1:3699");
        arguments.addArgument("maxconn", "10");
        arguments.addArgument("user", "root");
        arguments.addArgument("pwd", "nebula");
        arguments.addArgument("space", "");
        arguments.addArgument("nGQL", "");
        arguments.addArgument("person", "");
        return arguments;
    }


    public void initNebulaPool(String hosts, int maxconn, int id) {
        pool = new NebulaPool();
        assert pool != null;
        try {
            List<HostAddress> addresses = new ArrayList();
            NebulaPoolConfig nebulaPoolConfig = new NebulaPoolConfig();
            nebulaPoolConfig.setMaxConnSize(maxconn);
            List<String> host_list = new ArrayList<String>(Arrays.asList(hosts.split(",")));
            assert host_list != null;
            String host = host_list.get(id%host_list.size());
            String[] splits = host.split(":");
            addresses.add(new HostAddress(splits[0], Integer.parseInt(splits[1])));
            boolean init = pool.init(addresses, nebulaPoolConfig);
            assert init == true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void setupTest(JavaSamplerContext javaSamplerContext) {
        System.out.print("Perf thread start:" + Thread.currentThread().getName() + "\n");
        String hosts = javaSamplerContext.getParameter("hosts");
        int maxconn = Integer.parseInt(javaSamplerContext.getParameter("maxconn").trim());
        String user = javaSamplerContext.getParameter("user");
        String pwd = javaSamplerContext.getParameter("pwd");
        String space = javaSamplerContext.getParameter("space");
        assert space != null;
        int id = javaSamplerContext.getJMeterContext().getThreadNum();
        initNebulaPool(hosts, maxconn, id);
        try {
            session = pool.getSession(user, pwd, false);
            assert session != null;
            String use_space = "use " + space + ";";
            ResultSet resp = null;
            resp = session.execute(use_space);
            assert resp.isSucceeded();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
        System.out.print("Perf runTest:" + Thread.currentThread().getName() + "\n");

        SampleResult result = new SampleResult();
        result.setSampleLabel("Java request");
        String person = javaSamplerContext.getParameter("person");
        String nGQL = javaSamplerContext.getParameter("nGQL");
        nGQL = nGQL.replace("replace", person);
        assert person != "";
        assert nGQL != "";
        ResultSet resp = null;

        result.sampleStart();
        try {
            resp = session.execute(nGQL);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assert resp != null;
        result.sampleEnd();
        result.setResponseData("Perf test::", "UTF-8");
        result.setDataEncoding("UTF-8");

        if (!resp.isSucceeded()) {
            result.setSuccessful(false);
            result.setResponseMessage(nGQL + ":" + resp.getErrorMessage());
            log.error(String.format("Execute: `%s', failed: %s",
                    nGQL, resp.getErrorMessage()));
        } else {
            result.setResponseMessage(nGQL);
            log.info(String.format("Execute: `%s', success!",
                    nGQL));
            result.setSuccessful(true);
        }
        return result;
    }


    @Override
    public void teardownTest(JavaSamplerContext javaSamplerContext) {
        System.out.print("Pert test end!\n");
        assert session != null;
        session.release();
        if (pool.getActiveConnNum() == 0) {
            pool.close();
        }
    }

    public static void main(String[] args) {
        System.out.println("Hello World!");
    }
}