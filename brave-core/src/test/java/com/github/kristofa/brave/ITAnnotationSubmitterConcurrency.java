package com.github.kristofa.brave;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.twitter.zipkin.gen.Endpoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.kristofa.brave.SpanAndEndpoint.StaticSpanAndEndpoint;
import com.twitter.zipkin.gen.Span;

/**
 * This isSampled proves that we have proper synchronisation when submitted annotations for the same span. Without proper
 * synchronisation this isSampled fails with {@link ArrayIndexOutOfBoundsException} because adding items to an {@link ArrayList}
 * is not thread-safe. </p> This isSampled proves that we fixed the threading bug.
 * 
 * @author kristof
 */
public class ITAnnotationSubmitterConcurrency {

    private ExecutorService executorService;
    private Span span;
    private Endpoint endpoint = Endpoint.create("foobar", 1 << 24 | 2 << 16 | 3 << 8 | 4, 9999);

    @Before
    public void setup() {
        executorService = Executors.newFixedThreadPool(4);
        span = new Span();
    }

    @After
    public void tearDown() {
        executorService.shutdown();
    }

    @Test
    public void testSubmitAnnotations() throws InterruptedException, ExecutionException {

        final AnnotationSubmitter annotationSubmitter = AnnotationSubmitter.create(StaticSpanAndEndpoint.create(span, endpoint));

        final List<AnnotationSubmitThread> threadList =
            Arrays.asList(new AnnotationSubmitThread(1, 100, annotationSubmitter), new AnnotationSubmitThread(101, 200,
                annotationSubmitter), new AnnotationSubmitThread(201, 300, annotationSubmitter),
                new AnnotationSubmitThread(301, 400, annotationSubmitter));

        final List<Future<?>> resultList = new ArrayList<Future<?>>();

        for (final AnnotationSubmitThread thread : threadList) {
            {
                resultList.add(executorService.submit(thread));
            }
        }

        for (final Future<?> result : resultList) {
            result.get();
        }

        assertEquals(400, span.getAnnotations().size());
        assertEquals(400, span.getBinary_annotations().size());

    }

    private final class AnnotationSubmitThread implements Callable<Void> {

        private final int from;
        private final int to;
        private final AnnotationSubmitter annotationSubmitter;

        public AnnotationSubmitThread(final int from, final int to, final AnnotationSubmitter annotationSubmitter) {
            assertTrue(from <= to);
            this.from = from;
            this.to = to;
            this.annotationSubmitter = annotationSubmitter;
        }

        @Override
        public Void call() throws Exception {
            for (int index = from; index <= to; index++) {
                annotationSubmitter.submitAnnotation("annotation" + index);
                annotationSubmitter.submitBinaryAnnotation("binaryAnnotation" + index, "value");
            }
            return null;
        }

    }
}
