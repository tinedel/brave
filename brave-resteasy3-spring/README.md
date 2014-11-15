# brave-resteasy3-spring #

Latest release available in Maven central:

    <dependency>
        <groupId>com.github.kristofa</groupId>
        <artifactId>brave-resteasy3-spring</artifactId>
        <version>2.2.0</version>
    </dependency>


The brave-resteasy3-spring module has RESTEasy 3 client and server support which makes the
usage of Brave transparent for your application. This module contains:

*   `BraveClientExecutionInterceptor` can be configured to be used with the RestEasy
Client Framework and intercepts every client request made.  It will use `ClientTracer` to
decide if request needs to be traced and if so it will submit span to SpanCollector containing
`client send` and `client received` annotations.  It will also detect failed requests and in that
case add some additional annotations like for example `http.responsecode` and `failure` annotation. 
*   `BravePreProcessInterceptor` and `BravePostProcessInterceptor` will intercept requests at the
server side and will use `ServerTracer` to deal with tracing.
  
brave-resteasy3-spring puts the Spring and RESTEasy dependencies to scope provided which means you are free to choose the
versions yourself and add the dependencies to your own application. 

For other details please refer to brave-resteasy-spring documentation.