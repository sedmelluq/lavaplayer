package com.sedmelluq.discord.lavaplayer.node;

import com.sedmelluq.discord.lavaplayer.node.message.MessageHandlerRegistry;
import com.sedmelluq.discord.lavaplayer.node.message.MessageOutput;
import com.sedmelluq.discord.lavaplayer.remote.message.RemoteMessage;
import com.sedmelluq.discord.lavaplayer.remote.message.RemoteMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@RestController
public class NodeController {
  private final MessageHandlerRegistry messageHandlerRegistry;
  private final StatisticsManager statisticsManager;
  private final RemoteMessageMapper mapper;

  @Autowired
  public NodeController(MessageHandlerRegistry messageHandlerRegistry, StatisticsManager statisticsManager) {
    this.messageHandlerRegistry = messageHandlerRegistry;
    this.statisticsManager = statisticsManager;
    this.mapper = new RemoteMessageMapper();
  }

  @RequestMapping("/tick")
  public void handeTick(HttpServletRequest request, HttpServletResponse response) throws IOException {
    DataInputStream input = new DataInputStream(request.getInputStream());
    DataOutputStream output = new DataOutputStream(response.getOutputStream());
    MessageOutput messageOutput = new MessageOutput(mapper, output);
    RemoteMessage message;

    while ((message = mapper.decode(input)) != null) {
      messageHandlerRegistry.processMessage(message, messageOutput);
    }

    messageOutput.send(statisticsManager.getStatistics());
    mapper.endOutput(output);
  }
}
