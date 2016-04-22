/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ratpack.zipkin.internal;

import com.github.kristofa.brave.ServerClientAndLocalSpanState;
import com.github.kristofa.brave.ServerSpan;
import com.github.kristofa.brave.internal.Nullable;
import com.twitter.zipkin.gen.Endpoint;
import com.twitter.zipkin.gen.Span;
import ratpack.exec.Execution;
import ratpack.registry.MutableRegistry;


public class RatpackServerClientLocalSpanState implements ServerClientAndLocalSpanState  {
  private final MutableRegistry registry;

  public RatpackServerClientLocalSpanState(final String serviceName, int ip, int port) {
    this(serviceName, ip, port, Execution.current());
  }

  RatpackServerClientLocalSpanState(final String serviceName, int ip, int port, final MutableRegistry registry) {
    this.registry = registry;
    this.registry.add(new ServerEndpointValue(Endpoint.create(serviceName, ip, port)));
  }

  @Override
  public Endpoint getClientEndpoint() {
    return registry
        .maybeGet(ClientEndpointValue.class)
        .orElse(new ClientEndpointValue(null))
        .get();
  }

  @Override
  public void setCurrentClientSpan(final Span span) {
    registry.add(new ClientSpanValue(span));
  }

  @Override
  public Span getCurrentClientSpan() {
    return registry
        .maybeGet(ClientSpanValue.class)
        .orElse(new ClientSpanValue(null))
        .get();
  }

  @Override
  public void setCurrentClientServiceName(@Nullable final String serviceName) {
    if (serviceName == null) {
      registry.add(new ClientEndpointValue(registry.get(ServerEndpointValue.class).get()));
    } else {
      Endpoint serverEndPoint = getServerEndpoint();
      Endpoint endpoint = Endpoint.create(serviceName, serverEndPoint.ipv4, serverEndPoint.port);
      registry.add(new ClientEndpointValue(endpoint));
    }
  }

  @Override
  public Span getCurrentLocalSpan() {
    return registry.get(LocalSpanValue.class).get();
  }

  @Override
  public void setCurrentLocalSpan(final Span span) {
    registry.add(new LocalSpanValue(span));
  }

  @Override
  public ServerSpan getCurrentServerSpan() {
    return registry
        .maybeGet(ServerSpanValue.class)
        .orElse(new ServerSpanValue(ServerSpan.EMPTY))
        .get();
  }

  @Override
  public Endpoint getServerEndpoint() {
    return registry.get(ServerEndpointValue.class).get();
  }

  @Override
  public void setCurrentServerSpan(final ServerSpan span) {
    registry.add(new ServerSpanValue(span));
  }

  @Override
  public Boolean sample() {
    return getCurrentServerSpan().getSample();
  }

  private class LocalSpanValue extends TypedValue<Span> {
    LocalSpanValue(final Span value) {
      super(value);
    }
  }
  private class ClientSpanValue extends TypedValue<Span> {
    ClientSpanValue(final Span value) {
      super(value);
    }
  }
  private class ServerSpanValue extends TypedValue<ServerSpan> {
    ServerSpanValue(final ServerSpan value) {
      super(value);
    }
  }
  private class ServerEndpointValue extends TypedValue<Endpoint> {
    ServerEndpointValue(final Endpoint value) {
      super(value);
    }
  }
  private class ClientEndpointValue extends TypedValue<Endpoint> {
    ClientEndpointValue(final Endpoint value) {
      super(value);
    }
  }

  private abstract class TypedValue<T> {
    private T value;

    TypedValue(final T value) {
      this.value = value;
    }

    T get() {
      return value;
    }
  }

}
