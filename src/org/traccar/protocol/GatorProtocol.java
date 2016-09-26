/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import java.util.List;

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.traccar.BaseProtocol;
import org.traccar.TrackerServer;

public class GatorProtocol extends BaseProtocol {
  
  public GatorProtocol() {
    super("gator");
  }
  
  @Override
  public void initTrackerServers(List<TrackerServer> serverList) {
    serverList.add(new TrackerServer(new ServerBootstrap(), this.getName()) {
      @Override
      protected void addSpecificHandlers(ChannelPipeline pipeline) {
        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024,
            3, 2, 1, 0));
        pipeline.addLast("objectDecoder", new GatorProtocolDecoder(
            GatorProtocol.this));
      }
    });
    serverList.add(new TrackerServer(new ConnectionlessBootstrap(), this
        .getName()) {
      @Override
      protected void addSpecificHandlers(ChannelPipeline pipeline) {
        pipeline.addLast("objectDecoder", new GatorProtocolDecoder(
            GatorProtocol.this));
      }
    });
  }
  
}
