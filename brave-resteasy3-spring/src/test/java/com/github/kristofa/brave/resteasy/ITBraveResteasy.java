package com.github.kristofa.brave.resteasy;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.EndPointSubmitter;
import com.twitter.zipkin.gen.Span;

public class ITBraveResteasy {

    private Server server;

    @Before
    public void setup() {
        server = new Server();

        final SocketConnector connector = new SocketConnector();

        connector.setMaxIdleTime(1000 * 60 * 60);
        connector.setPort(8080);
        server.setConnectors(new Connector[] {connector});

        final WebAppContext context = new WebAppContext();
        context.setServer(server);
        context.setContextPath("/BraveRestEasyIntegration");
        context.setWar("src/test/webapp");

        server.setHandler(context);

        try {
            server.start();
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to start server.", e);
        }
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
        server.join();
    }

    @Test
    public void test() throws ClientProtocolException, IOException, InterruptedException {
        // We need to set up our endpoint first because we start a client request from
        // in our test so the brave preprocessor did not set up end point yet.
        final EndPointSubmitter endPointSubmitter = Brave.getEndPointSubmitter();
        endPointSubmitter.submit("127.0.0.1", 8080, "BraveRestEasyIntegration");

        // this initialization only needs to be done once per VM
        RegisterBuiltin.register(ResteasyProviderFactory.getInstance());

        // Create our client. The client will be configured using BraveClientExecutionInterceptor because
        // we Spring will scan com.github.kristofa.brave package. This is the package containing our client interceptor
        // in module brave-resteasy-spring-module which is on our class path.
        final BraveRestEasyResource client =
            ProxyFactory.create(BraveRestEasyResource.class, "http://localhost:8080/BraveRestEasyIntegration");

        @SuppressWarnings("unchecked")
        final ClientResponse<Void> response = (ClientResponse<Void>)client.a();
        try {
            assertEquals(200, response.getStatus());
            final List<Span> collectedSpans = SpanCollectorForTesting.getInstance().getCollectedSpans();
            assertEquals(2, collectedSpans.size());
            assertEquals("Expected trace id's to be equal", collectedSpans.get(0).getTrace_id(), collectedSpans.get(1)
                .getTrace_id());
            assertEquals("Expected span id's to be equal", collectedSpans.get(0).getId(), collectedSpans.get(1).getId());
            assertEquals("Expected parent span id's to be equal", collectedSpans.get(0).getParent_id(), collectedSpans
                .get(1).getParent_id());
            assertEquals("Span names of client and server should be equal.", collectedSpans.get(0).getName(), collectedSpans
                .get(1).getName());
            assertEquals("Expect 2 annotations.", 2, collectedSpans.get(0).getAnnotations().size());
            assertEquals("Expect 2 annotations.", 2, collectedSpans.get(1).getAnnotations().size());

        } finally {
            response.releaseConnection();
        }
    }

}
