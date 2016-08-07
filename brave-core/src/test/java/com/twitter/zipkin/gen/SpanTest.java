package com.twitter.zipkin.gen;

import org.junit.Test;
import zipkin.Constants;

import static org.junit.Assert.assertEquals;

public class SpanTest {
  @Test
  public void testNameLowercase() {
    assertEquals("spanname", new Span().setName("SpanName").getName());
  }

  @Test
  public void toStringIsJson() {
    long traceId = -692101025335252320L;
    Span span = new Span()
        .setTrace_id(traceId)
        .setName("get")
        .setId(traceId)
        .setTimestamp(1444438900939000L)
        .setDuration(376000L);

    assertEquals("{\"traceId\":\"f66529c8cc356aa0\",\"name\":\"get\",\"id\":\"f66529c8cc356aa0\",\"timestamp\":1444438900939000,\"duration\":376000,\"annotations\":[],\"binaryAnnotations\":[]}", span.toString());
  }

  @Test
  public void canStoreNanoTimeForDurationCalculation() {
    Span span = new Span();
    span.startTick = System.nanoTime();
  }
}
