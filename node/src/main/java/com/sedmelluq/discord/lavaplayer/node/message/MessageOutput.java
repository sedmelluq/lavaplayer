package com.sedmelluq.discord.lavaplayer.node.message;

import com.sedmelluq.discord.lavaplayer.remote.message.RemoteMessage;
import com.sedmelluq.discord.lavaplayer.remote.message.RemoteMessageMapper;

import java.io.DataOutputStream;
import java.io.IOException;

public class MessageOutput {
  private final RemoteMessageMapper mapper;
  private final DataOutputStream output;

  public MessageOutput(RemoteMessageMapper mapper, DataOutputStream output) {
    this.mapper = mapper;
    this.output = output;
  }

  public void send(RemoteMessage message) {
    try {
      mapper.encode(output, message);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
