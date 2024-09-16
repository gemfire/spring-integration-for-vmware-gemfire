/*
 * Copyright 2023-2024 Broadcom. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.springframework.integration.gemfire.inbound;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.client.internal.ClientRegionFactoryImpl;
import org.apache.geode.cache.client.internal.InternalClientCache;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.gemfire.RegionAttributesFactoryBean;
import org.springframework.data.gemfire.client.ClientCacheFactoryBean;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.client.PoolFactoryBean;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.support.ConnectionEndpoint;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.1
 */
public class CacheListeningMessageProducerTests {

  private static final SpelExpressionParser PARSER = new SpelExpressionParser();

  private static ClientCacheFactoryBean cacheFactoryBean;

  private static ClientRegionFactoryBean<String, String> regionFactoryBean;

  private static Region<String, String> region;

  @BeforeClass
  public static void setup() throws Exception {
    cacheFactoryBean = new ClientCacheFactoryBean();
    cacheFactoryBean.setBeanFactory(mock(BeanFactory.class));


    regionFactoryBean = new ClientRegionFactoryBean<>();
    regionFactoryBean.setShortcut(ClientRegionShortcut.LOCAL);
    regionFactoryBean.setDataPolicy(DataPolicy.NORMAL);
    regionFactoryBean.setName("test.receiveNewValuePayloadForCreateEvent");
    regionFactoryBean.setCache(cacheFactoryBean.getObject());
    setRegionAttributes(regionFactoryBean);
    regionFactoryBean.setBeanFactory(mock(BeanFactory.class));
    regionFactoryBean.afterPropertiesSet();
    region = regionFactoryBean.getObject();
  }

  @AfterClass
  public static void teardown() throws Exception {
    regionFactoryBean.destroy();
//    cacheFactoryBean.destroy();
  }

  @Test
  public void receiveNewValuePayloadForCreateEvent() {
    QueueChannel channel = new QueueChannel();
    CacheListeningMessageProducer producer = new CacheListeningMessageProducer(region);
    producer.setPayloadExpression(PARSER.parseExpression("key + '=' + newValue"));
    producer.setOutputChannel(channel);
    producer.setBeanFactory(mock(BeanFactory.class));
    producer.afterPropertiesSet();
    producer.start();

    assertThat(channel.receive(0)).isNull();
    region.put("x", "abc");
    Message<?> message = channel.receive(0);
    assertThat(message).isNotNull();
    assertThat(message.getPayload()).isEqualTo("x=abc");

    producer.stop();
  }

  @Test
  public void receiveNewValuePayloadForUpdateEvent() {
    QueueChannel channel = new QueueChannel();
    CacheListeningMessageProducer producer = new CacheListeningMessageProducer(region);
    producer.setPayloadExpression(PARSER.parseExpression("newValue"));
    producer.setOutputChannel(channel);
    producer.setBeanFactory(mock(BeanFactory.class));
    producer.afterPropertiesSet();
    producer.start();

    assertThat(channel.receive(0)).isNull();
    region.put("x", "abc");
    Message<?> message1 = channel.receive(0);
    assertThat(message1).isNotNull();
    assertThat(message1.getPayload()).isEqualTo("abc");
    region.put("x", "xyz");
    Message<?> message2 = channel.receive(0);
    assertThat(message2).isNotNull();
    assertThat(message2.getPayload()).isEqualTo("xyz");

    producer.stop();
  }

  @Test
  public void receiveOldValuePayloadForDestroyEvent() {
    QueueChannel channel = new QueueChannel();
    CacheListeningMessageProducer producer = new CacheListeningMessageProducer(region);
    producer.setSupportedEventTypes(EventType.DESTROYED);
    producer.setPayloadExpression(PARSER.parseExpression("oldValue"));
    producer.setOutputChannel(channel);
    producer.setBeanFactory(mock(BeanFactory.class));
    producer.afterPropertiesSet();
    producer.start();

    assertThat(channel.receive(0)).isNull();
    region.put("foo", "abc");
    assertThat(channel.receive(0)).isNull();
    region.destroy("foo");
    Message<?> message2 = channel.receive(0);
    assertThat(message2).isNotNull();
    assertThat(message2.getPayload()).isEqualTo("abc");

    producer.stop();
  }

  @Test
  public void receiveOldValuePayloadForInvalidateEvent() {
    QueueChannel channel = new QueueChannel();
    CacheListeningMessageProducer producer = new CacheListeningMessageProducer(region);
    producer.setSupportedEventTypes(EventType.INVALIDATED);
    producer.setPayloadExpression(PARSER.parseExpression("key + ' was ' + oldValue"));
    producer.setOutputChannel(channel);
    producer.setBeanFactory(mock(BeanFactory.class));
    producer.afterPropertiesSet();
    producer.start();

    assertThat(channel.receive(0)).isNull();
    region.put("foo", "abc");
    assertThat(channel.receive(0)).isNull();
    region.invalidate("foo");
    Message<?> message2 = channel.receive(0);
    assertThat(message2).isNotNull();
    assertThat(message2.getPayload()).isEqualTo("foo was abc");

    producer.stop();
  }

  private static void setRegionAttributes(ClientRegionFactoryBean<String, String> regionFactoryBean)
      throws Exception {

    RegionAttributesFactoryBean<String, String> attributesFactoryBean = new RegionAttributesFactoryBean<>();
    attributesFactoryBean.afterPropertiesSet();
    regionFactoryBean.setAttributes(attributesFactoryBean.getObject());
  }

}
