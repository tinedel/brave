package com.github.kristofa.brave;

import com.google.auto.value.AutoValue;

import com.github.kristofa.brave.SpanAndEndpoint.ClientSpanAndEndpoint;
import com.github.kristofa.brave.internal.Nullable;
import com.twitter.zipkin.gen.Endpoint;
import com.twitter.zipkin.gen.Span;
import zipkin.Constants;

import java.util.Random;

/**
 * Low level api that deals with client side of a request:
 *
 * <ol>
 *     <li>Decide on tracing or not (sampling)</li>
 *     <li>Sending client set / client received annotations</li>
 * </ol>
 *
 * It is advised that you use ClientRequestInterceptor and ClientResponseInterceptor which build
 * upon ClientTracer and provide a higher level api.
 *
 * @see ClientRequestInterceptor
 * @see ClientResponseInterceptor
 * @author kristof
 */
@AutoValue
public abstract class ClientTracer extends AnnotationSubmitter {

    public static Builder builder() {
        return new AutoValue_ClientTracer.Builder();
    }

    @Override
    abstract ClientSpanAndEndpoint spanAndEndpoint();
    abstract Random randomGenerator();
    abstract SpanCollector spanCollector();
    abstract Sampler traceSampler();

    @AutoValue.Builder
    public abstract static class Builder {

        public Builder state(ServerClientAndLocalSpanState state) {
            return spanAndEndpoint(ClientSpanAndEndpoint.create(state));
        }

        abstract Builder spanAndEndpoint(ClientSpanAndEndpoint spanAndEndpoint);

        /**
         * Used to generate new trace/span ids.
         */
        public abstract Builder randomGenerator(Random randomGenerator);

        public abstract Builder spanCollector(SpanCollector spanCollector);

        public abstract Builder traceSampler(Sampler sampler);

        public abstract ClientTracer build();
    }

    /**
     * Sets 'client sent' event for current thread.
     */
    public void setClientSent() {
        submitStartAnnotation(Constants.CLIENT_SEND);
    }

    /**
     * Like {@link #setClientSent()}, except you can log the network context of the destination.
     *
     * @param ipv4        ipv4 of the server as an int. Ex for 1.2.3.4, it would be (1 << 24) | (2 << 16) | (3 << 8) | 4
     * @param port        listen port the client is connecting to, or 0 if unknown
     * @param serviceName lowercase {@link Endpoint#service_name name} of the service being called
     *                    or null if unknown
     */
    public void setClientSent(int ipv4, int port, @Nullable String serviceName) {
        submitAddress(Constants.SERVER_ADDR, ipv4, port, serviceName);
        submitStartAnnotation(Constants.CLIENT_SEND);
    }

    /**
     * Sets the 'client received' event for current thread. This will also submit span because setting a client received
     * event means this span is finished.
     */
    public void setClientReceived() {
        if (submitEndAnnotation(Constants.CLIENT_RECV, spanCollector())) {
            spanAndEndpoint().state().setCurrentClientSpan(null);
        }
    }

    /**
     * Start a new span for a new client request that will be bound to current thread. The ClientTracer can decide to return
     * <code>null</code> in case this request should not be traced (eg sampling).
     *
     * @param requestName Request name. Should be lowercase and not <code>null</code> or empty.
     * @return Span id for new request or <code>null</code> in case we should not trace this new client request.
     */
    public SpanId startNewSpan(String requestName) {

        Boolean sample = spanAndEndpoint().state().sample();
        if (Boolean.FALSE.equals(sample)) {
            spanAndEndpoint().state().setCurrentClientSpan(null);
            return null;
        }

        SpanId newSpanId = getNewSpanId();
        if (sample == null) {
            // No sample indication is present.
            if (!traceSampler().isSampled(newSpanId.traceId)) {
                spanAndEndpoint().state().setCurrentClientSpan(null);
                return null;
            }
        }

        Span newSpan = newSpanId.toSpan();
        newSpan.setName(requestName);
        spanAndEndpoint().state().setCurrentClientSpan(newSpan);
        return newSpanId;
    }

    private SpanId getNewSpanId() {
        Span parentSpan = spanAndEndpoint().state().getCurrentLocalSpan();
        if (parentSpan == null) {
            ServerSpan serverSpan = spanAndEndpoint().state().getCurrentServerSpan();
            if (serverSpan != null) {
                parentSpan = serverSpan.getSpan();
            }
        }

        long newSpanId = randomGenerator().nextLong();
        SpanId.Builder builder = SpanId.builder().spanId(newSpanId);
        if (parentSpan == null) return builder.build(); // new trace
        return builder.traceId(parentSpan.getTrace_id()).parentId(parentSpan.getId()).build();
    }

    ClientTracer() {
    }
}
