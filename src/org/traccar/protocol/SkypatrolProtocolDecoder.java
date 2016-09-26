/*
 * Copyright 2012 - 2013 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Log;
import org.traccar.model.Position;

public class SkypatrolProtocolDecoder extends BaseProtocolDecoder {
  
  private final long defaultMask;
  
  public SkypatrolProtocolDecoder(SkypatrolProtocol protocol) {
    super(protocol);
    defaultMask = Context.getConfig().getInteger(getProtocolName() + ".mask");
  }
  
  private static double convertCoordinate(long coordinate) {
    int sign = 1;
    if (coordinate > 0x7fffffffL) {
      sign = -1;
      coordinate = 0xffffffffL - coordinate;
    }
    
    long degrees = coordinate / 1000000;
    double minutes = (coordinate % 1000000) / 10000.0;
    
    return sign * (degrees + minutes / 60);
  }
  
  @Override
  protected Object decode(Channel channel, SocketAddress remoteAddress,
      Object msg) throws Exception {
    
    ChannelBuffer buf = (ChannelBuffer) msg;
    
    int apiNumber = buf.readUnsignedShort();
    int commandType = buf.readUnsignedByte();
    int messageType = BitUtil.from(buf.readUnsignedByte(), 4);
    long mask = defaultMask;
    if (buf.readUnsignedByte() == 4) {
      mask = buf.readUnsignedInt();
    }
    
    // Binary position report
    if (apiNumber == 5 && commandType == 2 && messageType == 1
        && BitUtil.check(mask, 0)) {
      
      Position position = new Position();
      position.setProtocol(getProtocolName());
      
      if (BitUtil.check(mask, 1)) {
        position.set(Position.KEY_STATUS, buf.readUnsignedInt());
      }
      
      String id;
      if (BitUtil.check(mask, 23)) {
        id = buf.toString(buf.readerIndex(), 8, StandardCharsets.US_ASCII)
            .trim();
        buf.skipBytes(8);
      } else if (BitUtil.check(mask, 2)) {
        id = buf.toString(buf.readerIndex(), 22, StandardCharsets.US_ASCII)
            .trim();
        buf.skipBytes(22);
      } else {
        Log.warning("No device id field");
        return null;
      }
      if (!identify(id, channel, remoteAddress)) {
        return null;
      }
      position.setDeviceId(getDeviceId());
      
      if (BitUtil.check(mask, 3)) {
        buf.readUnsignedShort(); // io data
      }
      
      if (BitUtil.check(mask, 4)) {
        buf.readUnsignedShort(); // adc 1
      }
      
      if (BitUtil.check(mask, 5)) {
        buf.readUnsignedShort(); // adc 2
      }
      
      if (BitUtil.check(mask, 7)) {
        buf.readUnsignedByte(); // function category
      }
      
      DateBuilder dateBuilder = new DateBuilder();
      
      if (BitUtil.check(mask, 8)) {
        dateBuilder.setDateReverse(buf.readUnsignedByte(),
            buf.readUnsignedByte(), buf.readUnsignedByte());
      }
      
      if (BitUtil.check(mask, 9)) {
        position.setValid(buf.readUnsignedByte() == 1); // gps status
      }
      
      if (BitUtil.check(mask, 10)) {
        position.setLatitude(convertCoordinate(buf.readUnsignedInt()));
      }
      
      if (BitUtil.check(mask, 11)) {
        position.setLongitude(convertCoordinate(buf.readUnsignedInt()));
      }
      
      if (BitUtil.check(mask, 12)) {
        position.setSpeed(buf.readUnsignedShort() / 10.0);
      }
      
      if (BitUtil.check(mask, 13)) {
        position.setCourse(buf.readUnsignedShort() / 10.0);
      }
      
      if (BitUtil.check(mask, 14)) {
        dateBuilder.setTime(buf.readUnsignedByte(), buf.readUnsignedByte(),
            buf.readUnsignedByte());
      }
      
      position.setTime(dateBuilder.getDate());
      
      if (BitUtil.check(mask, 15)) {
        position.setAltitude(buf.readMedium());
      }
      
      if (BitUtil.check(mask, 16)) {
        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
      }
      
      if (BitUtil.check(mask, 17)) {
        buf.readUnsignedShort(); // battery percentage
      }
      
      if (BitUtil.check(mask, 20)) {
        position.set("trip", buf.readUnsignedInt());
      }
      
      if (BitUtil.check(mask, 21)) {
        position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
      }
      
      if (BitUtil.check(mask, 22)) {
        buf.skipBytes(6); // time of message generation
      }
      
      if (BitUtil.check(mask, 24)) {
        position.set(Position.KEY_POWER, buf.readUnsignedShort() / 1000.0);
      }
      
      if (BitUtil.check(mask, 25)) {
        buf.skipBytes(18); // gps overspeed
      }
      
      if (BitUtil.check(mask, 26)) {
        buf.skipBytes(54); // cell information
      }
      
      if (BitUtil.check(mask, 28)) {
        position.set(Position.KEY_INDEX, buf.readUnsignedShort());
      }
      
      return position;
    }
    
    return null;
  }
  
}
