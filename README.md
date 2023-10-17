## Spring Integration for VMware GemFire

Spring Integration provides support for VMware GemFire.

You need to include this dependency into your project:


**Maven**
```xml
<dependency>
    <groupId>org.springframework.integration</groupId>
    <artifactId>spring-integration-gemfire</artifactId>
    <version>{project-version}</version>
</dependency>
```

**Gradle**
```groovy
compile "org.springframework.integration:spring-integration-gemfire:{project-version}"
```

GemFire is a distributed data management platform that provides a key-value data grid along with advanced distributed system features, such as event processing, continuous querying, and remote function execution.
This guide assumes some familiarity with the commercial [VMware GemFire](https://www.vmware.com/products/gemfire.html).

Spring Integration provides support for GemFire by implementing inbound adapters for entry and continuous query events, an outbound adapter to write entries to the cache, and message and metadata stores and `GemfireLockRegistry` implementations.
Spring integration leverages the [Spring Data for VMware GemFire](https://docs.vmware.com/en/Spring-Data-for-VMware-GemFire/index.html) project, providing a thin wrapper over its components.

To configure the 'int-gfe' namespace, include the following elements within the headers of your XML configuration file:

```xml
xmlns:int-gfe="http://www.springframework.org/schema/integration/gemfire"
xsi:schemaLocation="http://www.springframework.org/schema/integration/gemfire
https://www.springframework.org/schema/integration/gemfire/spring-integration-gemfire.xsd"
```

### <a id="gemfire-inbound"></a>Inbound Channel Adapter

The inbound channel adapter produces messages on a channel when triggered by a GemFire `EntryEvent`.
GemFire generates events whenever an entry is `CREATED`, `UPDATED`, `DESTROYED`, or `INVALIDATED` in the associated region.
The inbound channel adapter lets you filter on a subset of these events.
For example, you may want to produce messages only in response to an entry being created.
In addition, the inbound channel adapter can evaluate a SpEL expression if, for example, you want your message payload to contain an event property such as the new entry value.
The following example shows how to configure an inbound channel adapter with a SpEL language (in the `expression` attribute):

```xml
<gfe:cache/>
<gfe:replicated-region id="region"/>
<int-gfe:inbound-channel-adapter id="inputChannel" region="region"
cache-events="CREATED" expression="newValue"/>
```

The preceding configuration creates a GemFire `Cache` and `Region` by using Spring GemFire's 'gfe' namespace.
The `inbound-channel-adapter` element requires a reference to the GemFire region on which the adapter listens for events.
Optional attributes include `cache-events`, which can contain a comma-separated list of event types for which a message is produced on the input channel.
By default, `CREATED` and `UPDATED` are enabled.
If no `channel` attribute is provided, the channel is created from the `id` attribute.
This adapter also supports an `error-channel`.
The GemFire [`EntryEvent`](https://geode.apache.org/releases/latest/javadoc/org/apache/geode/cache/EntryEvent.html) is the `#root` object of the `expression` evaluation.
The following example shows an expression that replaces a value for a key:

```
expression="new something.MyEvent(key, oldValue, newValue)"
```

If the `expression` attribute is not provided, the message payload is the GemFire `EntryEvent` itself.

NOTE: This adapter conforms to Spring Integration conventions.

### <a id="gemfire-cq"></a>Continuous Query Inbound Channel Adapter

The continuous query inbound channel adapter produces messages on a channel when triggered by a GemFire continuous query or `CqEvent` event.
Spring Data introduced continuous query support, including `ContinuousQueryListenerContainer`, which provides a nice abstraction over the GemFire native API.
This adapter requires a reference to a `ContinuousQueryListenerContainer` instance, creates a listener for a given `query`, and executes the query.
The continuous query acts as an event source that fires whenever its result set changes state.

NOTE: GemFire queries are written in OQL and are scoped to the entire cache (not just one region).
Additionally, continuous queries require a remote (that is, running in a separate process or remote host) cache server.
See the [GemFire Continuous Queries](https://docs.vmware.com/en/VMware-GemFire/10.0/gf/developing-continuous_querying-chapter_overview.html) for more information on implementing continuous queries.

The following configuration creates a GemFire client cache (recall that a remote cache server is required for this implementation and its address is configured as a child element of the pool), a client region, and a `ContinuousQueryListenerContainer` that uses Spring Data:

```xml
<gfe:client-cache id="client-cache" pool-name="client-pool"/>

<gfe:pool id="client-pool" subscription-enabled="true" >
<!--configure server or locator here required to address the cache server -->
</gfe:pool>

<gfe:client-region id="test" cache-ref="client-cache" pool-name="client-pool"/>

<gfe:cq-listener-container id="queryListenerContainer" cache="client-cache"
pool-name="client-pool"/>

<int-gfe:cq-inbound-channel-adapter id="inputChannel"
cq-listener-container="queryListenerContainer"
query="select * from /test"/>
```

The continuous query inbound channel adapter requires a `cq-listener-container` attribute, which must contain a reference to the `ContinuousQueryListenerContainer`.
Optionally, it accepts an `expression` attribute that uses SpEL to transform the `CqEvent` or extract an individual property as needed.
The `cq-inbound-channel-adapter` provides a `query-events` attribute that contains a comma-separated list of event types for which a message is produced on the input channel.
The available event types are `CREATED`, `UPDATED`, `DESTROYED`, `REGION_DESTROYED`, and `REGION_INVALIDATED`.
By default, `CREATED` and `UPDATED` are enabled.
Additional optional attributes include `query-name` (which provides an optional query name), `expression` (which works as described in the preceding section), and `durable` (a boolean value indicating if the query is durable -- it is false by default).
If you do not provide a `channel`, the channel is created from the `id` attribute.
This adapter also supports an `error-channel`.

NOTE: This adapter conforms to Spring Integration conventions.

### <a id="gemfire-outbound"></a> Outbound Channel Adapter

The outbound channel adapter writes cache entries that are mapped from the message payload.
In its simplest form, it expects a payload of type `java.util.Map` and puts the map entries into its configured region.
The following example shows how to configure an outbound channel adapter:

```xml
<int-gfe:outbound-channel-adapter id="cacheChannel" region="region"/>
```

Given the preceding configuration, an exception is thrown if the payload is not a `Map`.
Additionally, you can configure the outbound channel adapter to create a map of cache entries by using SpEL.
The following example shows how to do so:

```xml
<int-gfe:outbound-channel-adapter id="cacheChannel" region="region">
    <int-gfe:cache-entries>
        <entry key="payload.toUpperCase()" value="payload.toLowerCase()"/>
        <entry key="'thing1'" value="'thing2'"/>
    </int-gfe:cache-entries>
</int-gfe:outbound-channel-adapter>
```

In the preceding configuration, the inner element (`cache-entries`) is semantically equivalent to a Spring 'map' element.
The adapter interprets the `key` and `value` attributes as SpEL expressions with the message as the evaluation context.
Note that this can contain arbitrary cache entries (not only those derived from the message) and that literal values must be enclosed in single quotes.
In the preceding example, if the message sent to `cacheChannel` has a `String` payload with a value `Hello`, two entries (`[HELLO:hello, thing1:thing2]`) are written (either created or updated) in the cache region.
This adapter also supports the `order` attribute, which may be useful if it is bound to a `PublishSubscribeChannel`.

### <a id="gemfire-message-store"></a> Gemfire Message Store

As described in EIP, a [message store](https://www.enterpriseintegrationpatterns.com/MessageStore.html) lets you persist messages.
This can be useful when dealing with components that have a capability to buffer messages (`QueueChannel`, `Aggregator`, `Resequencer`, and others) if reliability is a concern.
In Spring Integration, the `MessageStore` strategy interface also provides the foundation for the [claim check](https://www.enterpriseintegrationpatterns.com/StoreInLibrary.html) pattern, which is described in EIP as well.

Spring Integration's Gemfire module provides `GemfireMessageStore`, which is an implementation of both the `MessageStore` strategy (mainly used by the `QueueChannel` and `ClaimCheck` patterns) and the `MessageGroupStore` strategy (mainly used by the `Aggregator` and `Resequencer` patterns).

The following example configures the cache and region by using the `spring-gemfire` namespace (not to be confused with the `spring-integration-gemfire` namespace):

```xml
<bean id="gemfireMessageStore" class="o.s.i.gemfire.store.GemfireMessageStore">
    <constructor-arg ref="myRegion"/>
</bean>

<gfe:cache/>

<gfe:replicated-region id="myRegion"/>


<int:channel id="somePersistentQueueChannel">
    <int:queue message-store="gemfireMessageStore"/>
<int:channel>

<int:aggregator input-channel="inputChannel" output-channel="outputChannel"
message-store="gemfireMessageStore"/>
```

Often, it is desirable for the message store to be maintained in one or more remote cache servers in a client-server configuration.
In this case, you should configure a client cache, a client region, and a client pool and inject the region into the `MessageStore`.
The following example shows how to do so:

```xml
<bean id="gemfireMessageStore"
class="org.springframework.integration.gemfire.store.GemfireMessageStore">
    <constructor-arg ref="myRegion"/>
</bean>

<gfe:client-cache/>

<gfe:client-region id="myRegion" shortcut="PROXY" pool-name="messageStorePool"/>

<gfe:pool id="messageStorePool">
    <gfe:server host="localhost" port="40404" />
</gfe:pool>
```

Note that the `pool` element is configured with the address of a cache server (you can substitute a locator here).
The region is configured as a 'PROXY' so that no data is stored locally.
The region's `id` corresponds to a region with the same name in the cache server.

Starting with version 4.3.12, the `GemfireMessageStore` supports the key `prefix` option to allow distinguishing between instances of the store on the same GemFire region.

### <a id="gemfire-lock-registry"></a> Gemfire Lock Registry

Starting with version 4.0, the `GemfireLockRegistry` is available.
Certain components (for example, the aggregator and the resequencer) use a lock obtained from a `LockRegistry` instance to ensure that only one thread is manipulating a group at any given time.
The `DefaultLockRegistry` performs this function within a single component.
You can now configure an external lock registry on these components.
When you use a shared `MessageGroupStore` with the `GemfireLockRegistry`, it can provide this functionality across multiple application instances, such that only one instance can manipulate the group at a time.

NOTE: One of the `GemfireLockRegistry` constructors requires a `Region` as an argument.
It is used to obtain a `Lock` from the `getDistributedLock()` method.
This operation requires `GLOBAL` scope for the `Region`.
Another constructor requires a `Cache`, and the `Region` is created with `GLOBAL` scope and with the name, `LockRegistry`.

### <a id="gemfire-metadata-store"></a> Gemfire Metadata Store

Version 4.0 introduced a new Gemfire-based [`MetadataStore`](https://docs.spring.io/spring-integration/reference/meta-data-store.html) implementation.
You can use the `GemfireMetadataStore` to maintain metadata state across application restarts.
This new `MetadataStore` implementation can be used with adapters such as:

* [Feed Inbound Channel Adapter](https://docs.spring.io/spring-integration/reference/feed.html#feed-inbound-channel-adapter)
* [Reading Files](https://docs.spring.io/spring-integration/reference/file/reading.html)
* [FTP Inbound Channel Adapter](https://docs.spring.io/spring-integration/reference/ftp/inbound.html)
* [SFTP Inbound Channel Adapter](https://docs.spring.io/spring-integration/reference/sftp/inbound.html)

To get these adapters to use the new `GemfireMetadataStore`, declare a Spring bean with a bean name of `metadataStore`.
The feed inbound channel adapter automatically picks up and use the declared `GemfireMetadataStore`.

NOTE: The `GemfireMetadataStore` also implements `ConcurrentMetadataStore`, letting it be reliably shared across multiple application instances, where only one instance can store or modify a key's value.
These methods give various levels of concurrency guarantees based on the scope and data policy of the region.
They are implemented in the peer cache and client-server cache but are disallowed in peer regions that have `NORMAL` or `EMPTY` data policies.

NOTE: Since version 5.0, the `GemfireMetadataStore` also implements `ListenableMetadataStore`, which lets you listen to cache events by providing `MetadataStoreListener` instances to the store, as the following example shows:

```java
GemfireMetadataStore metadataStore = new GemfireMetadataStore(cache);
metadataStore.addListener(new MetadataStoreListenerAdapter() {

    @Override
    public void onAdd(String key, String value) {
         ...
    }

});
```
