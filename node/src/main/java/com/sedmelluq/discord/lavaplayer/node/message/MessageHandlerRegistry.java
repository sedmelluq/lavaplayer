package com.sedmelluq.discord.lavaplayer.node.message;

import com.sedmelluq.discord.lavaplayer.remote.message.RemoteMessage;
import com.sedmelluq.discord.lavaplayer.remote.message.RemoteMessageType;
import com.sedmelluq.discord.lavaplayer.remote.message.UnknownMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MessageHandlerRegistry implements BeanPostProcessor {
  private static final Logger log = LoggerFactory.getLogger(MessageHandlerRegistry.class);

  private final Map<Class<?>, List<MethodEntry>> mapping;

  public MessageHandlerRegistry() {
    this.mapping = new IdentityHashMap<>();

    for (RemoteMessageType type : RemoteMessageType.class.getEnumConstants()) {
      mapping.put(type.codec.getMessageClass(), new ArrayList<>());
    }

    mapping.put(UnknownMessage.class, Collections.emptyList());
  }

  public void processMessage(RemoteMessage message, MessageOutput messageOutput) {
    for (MethodEntry entry : mapping.get(message.getClass())) {
      try {
        if (entry.hasOutputParameter) {
          entry.method.invoke(entry.bean, message, messageOutput);
        } else {
          entry.method.invoke(entry.bean, message);
        }
      } catch (ReflectiveOperationException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) {
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
    for (Method method : bean.getClass().getDeclaredMethods()) {
      if (method.getAnnotation(MessageHandler.class) != null) {
        processMethod(bean, method);
      }
    }

    return bean;
  }

  private void processMethod(Object bean, Method method) {
    Parameter[] parameters = method.getParameters();
    boolean hasOutputParameter = false;

    if (parameters.length == 2 && parameters[1].getType() == MessageOutput.class) {
      hasOutputParameter = true;
    } else if (parameters.length != 1) {
      log.error("Class {} method {} has message handler annotation, but unsuitable parameters", bean.getClass().getName(), method.getName());
      return;
    }

    List<MethodEntry> handerList = mapping.get(parameters[0].getType());

    if (handerList != null) {
      method.setAccessible(true);
      handerList.add(new MethodEntry(bean, method, hasOutputParameter));
    } else {
      log.error("Class {} method {} has message handler annotation, but unrecognized event parameter", bean.getClass().getName(), method.getName());
    }
  }

  private static class MethodEntry {
    private final Object bean;
    private final Method method;
    private final boolean hasOutputParameter;

    private MethodEntry(Object bean, Method method, boolean hasOutputParameter) {
      this.bean = bean;
      this.method = method;
      this.hasOutputParameter = hasOutputParameter;
    }
  }
}
