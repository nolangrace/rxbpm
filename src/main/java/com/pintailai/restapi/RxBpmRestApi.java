package com.pintailai.restapi;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCode;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.util.Timeout;
import com.pintailai.messages.InstanceStartMessage;
import com.pintailai.messages.InstanceStartedMessage;
import com.pintailai.process.RxBpm;
import org.camunda.bpm.model.bpmn.Bpmn;
import scala.concurrent.Await;
import scala.concurrent.Future;
import akka.http.javadsl.marshallers.jackson.Jackson;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletionStage;



public class RxBpmRestApi extends AllDirectives {
    private ActorSystem system;
    private RxBpm rxbpm;

    public static void startUp(ActorSystem system, RxBpm rxbpm) throws IOException {
        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        //In order to access all directives we need an instance where the routes are define.
        RxBpmRestApi app = new RxBpmRestApi(system, rxbpm);

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost("localhost",8080), materializer);

        System.out.println("Server online at http://localhost:8080/");
//        System.in.read(); // let it run until user presses return
//
//        binding
//                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
//                .thenAccept(unbound -> system.terminate()); // and shutdown when done
    }

    private RxBpmRestApi(ActorSystem system, RxBpm rxbpm){
        this.system = system;
        this.rxbpm = rxbpm;
    }

    private Route createRoute() {
        return route(
                get(()->
                        pathPrefix("instance", () ->
                                route(
                                        path((String instanceId) -> complete("Instance Id: "+instanceId)),
                                        pathPrefix((String instanceId) ->
                                                route(
                                                        pathPrefix("task",() ->
                                                                path((String taskId)->{
                                                                    return complete("Instance Id: "+instanceId+" Task ID: "+taskId);
                                                                })
                                                        )
                                                )
                                        )
                                )
                        )
                ),
                post(() ->
                        pathPrefix("instance", ()->
                            route(
                                    path((String startEventId) ->
                                        entity(Jackson.unmarshaller(Map.class), inputData -> {
                                                Timeout timeout = Timeout.create(Duration.ofSeconds(5));

                                                if(inputData==null)
                                                    inputData = new HashMap();

                                                // create new instance Actor
                                                ActorRef requestActor = system.actorOf(RestRequestActor.props()
                                                        , "RestRequest-"+ UUID.randomUUID().toString());

                                                Future<Object> future = Patterns.ask(requestActor, InstanceStartMessage
                                                        .createMessage(inputData, startEventId,
                                                                Bpmn.convertToString(rxbpm.getModel()), null), timeout);
                                                String result = "";
                                                try {
                                                    InstanceStartedMessage message = (InstanceStartedMessage)
                                                            Await.result(future, timeout.duration());
                                                    result = "Instance "+message.instanceId+" was created successfully";
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                    result = "Request timed out";
                                                }

                                                return complete(result);
                                            }
                                        )
                                    )
                            )
                        )
                )
        );
//        return
//                // here the complete behavior for this server is defined
//
//                // only handle GET requests
//                get(() -> route(
//                        // matches the empty path
//                        pathSingleSlash(() ->
//                                // return a constant string with a certain content type
//                                complete(HttpEntities.create(ContentTypes.TEXT_HTML_UTF8, "<html><body>Hello world!</body></html>"))
//                        ),
//                        path("ping", () ->
//                                // return a simple `text/plain` response
//                                complete("PONG!")
//                        ),
//                        path("hello", () ->
//                                // uses the route defined above
//                                helloRoute
//                        )
//                ));

//        return route(
//                path("hello", () ->
//                        get(() ->
//                                complete("<h1>Say hello to akka-http</h1>"))));
    }
}