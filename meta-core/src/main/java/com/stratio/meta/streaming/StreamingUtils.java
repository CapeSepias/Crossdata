/*
 * Stratio Meta
 *
 * Copyright (c) 2014, Stratio, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */

package com.stratio.meta.streaming;

import com.stratio.streaming.api.IStratioStreamingAPI;
import com.stratio.streaming.commons.constants.ColumnType;
import com.stratio.streaming.messaging.ColumnNameValue;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;

public class StreamingUtils {

  /**
   * Class logger.
   */
  private static final Logger LOG = Logger.getLogger(StreamingUtils.class);

  public static ColumnType metaToStreamingType(String value) {
    ColumnType type = null;
    if (value.equalsIgnoreCase("varchar") || value.equalsIgnoreCase("text") || value.equalsIgnoreCase("uuid")
        || value.equalsIgnoreCase("timestamp") || value.equalsIgnoreCase("timeuuid")){
      type=ColumnType.STRING;
    }
    else if (value.equalsIgnoreCase("boolean")){
      type=ColumnType.BOOLEAN;
    }
    else if (value.equalsIgnoreCase("double")){
      type=ColumnType.DOUBLE;
    }
    else if (value.equalsIgnoreCase("float")){
      type=ColumnType.FLOAT;
    }
    else if (value.equalsIgnoreCase("integer") || value.equalsIgnoreCase("int")){
      type=ColumnType.INTEGER;
    }
    else if (value.equalsIgnoreCase("long") || value.equalsIgnoreCase("counter")){
      type=ColumnType.LONG;
    } else {
      type = ColumnType.valueOf(value);
    }
    return type;
  }

  public static void insertRandomData(final IStratioStreamingAPI stratioStreamingAPI, final String streamName, final long duration) {
    // Insert random data
    Thread randomThread = new Thread(){
      public void run(){
        try {
          Thread.sleep((long) (duration/1.9));
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        for(int i=0; i<1; i++){
          LOG.debug("Inserting data");
          for(int j=0; j<4; j++){
            insertRandomData(stratioStreamingAPI, streamName);
          }
          LOG.debug("Data inserted");
          try {
            Thread.sleep(duration);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    };
    randomThread.start();
  }

  private static void insertRandomData(IStratioStreamingAPI stratioStreamingAPI, String streamName) {
    double randomDouble = Math.random()*100;
    int randomInt = (int) (randomDouble*Math.random()*2);
    StringBuilder sb = new StringBuilder(String.valueOf(randomDouble));
    sb.append(randomInt);
    String str = convertRandomNumberToString(sb.toString());
    ColumnNameValue firstColumnValue = new ColumnNameValue("name", str);
    ColumnNameValue secondColumnValue = new ColumnNameValue("age", new Integer(randomInt));
    ColumnNameValue thirdColumnValue = new ColumnNameValue("rating", new Double(randomDouble));
    ColumnNameValue fourthColumnValue = new ColumnNameValue("member", new Boolean((randomInt % 2) == 0));
    List<ColumnNameValue> streamData = Arrays
        .asList(firstColumnValue, secondColumnValue, thirdColumnValue, fourthColumnValue);
    try {
      stratioStreamingAPI.insertData(streamName, streamData);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  public static String convertRandomNumberToString(String str){
    return str.replace('0', 'o').replace('1', 'i').replace('2', 'u').replace('3', 'e').replace('4', 'a').
        replace('5', 'b').replace('6', 'c').replace('7', 'd').replace('8', 'f').replace('9', 'g').replace('.', 'm');
  }

  public static int findFreePort() {
    ServerSocket socket = null;
    try {
      socket = new ServerSocket(0);
      socket.setReuseAddress(true);
      int port = socket.getLocalPort();
      socket.close();
      return port;
    } catch (IOException e) {
      throw new IllegalStateException("Could not find a free TCP/IP port to start embedded Jetty HTTP Server on");
    }
  }

}
