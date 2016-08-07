package com.github.kristofa.brave;

import com.twitter.zipkin.gen.Endpoint;

/**
 * Keeps track of common trace/span state information.
 * <p>
 * Should be thread aware since we can have multiple parallel request which means multiple trace/spans.
 * </p>
 * 
 * @author kristof
 */
public interface CommonSpanState {

    /**
     * Indicates if we should sample current request.
     * <p/>
     * Should be thread-aware to support multiple parallel requests.
     * 
     * @return <code>null</code> in case there is no indication if we should sample or not. <code>true</code> in case we got
     *         the indication we should sample current request, <code>false</code> in case we should not sample the current
     *         request.
     */
    Boolean sample();

    /**
     * Gets the Endpoint (ip, port, service name) for this service.
     *
     * @return Endpoint for this service.
     */
    Endpoint endpoint();

}
