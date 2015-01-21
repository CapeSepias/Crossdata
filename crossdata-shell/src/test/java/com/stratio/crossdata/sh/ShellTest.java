/*
 * Licensed to STRATIO (C) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.  The STRATIO (C) licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.stratio.crossdata.sh;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.stratio.crossdata.common.data.ResultSet;
import com.stratio.crossdata.common.result.QueryResult;

public class ShellTest {

    @Test
    public void testMain()  {
        Shell.main(new String[]{"--sync", "--script", "/path/file.xdql"});
        assertTrue(true, "testMain failed.");
    }

    @Test
    public void testFlush()  {
        boolean ok=true;
        try {
            Shell shell = new Shell(false);
            shell.flush();
        }catch (Exception ex){
            fail("testFlush failed.");
        }
        assertEquals(true,ok,"testFlush failed.");
    }

    @Test
    public void testSetPrompt()  {
        boolean ok=true;
        try {
            Shell shell = new Shell(false);
            shell.setPrompt("catalogTest");
        }catch (Exception e){
            fail("testSetPrompt failed");
        }
        assertEquals(ok, true, "testSetPrompt failed.");
    }

    @Test
    public void testShellConnectWithoutServer()  {
        boolean ok=false;
        boolean result=true;
        try {
            Shell crossdataSh = new Shell(false);
            result = crossdataSh.connect();
        }catch (Exception e){
            fail("testShellConnectWithoutServer failed.");
        }
        Assert.assertEquals(result,ok, "testShellConnectWithoutServer failed.");
    }

    @Test
    public void testRemoveResultsHandler()  {
        boolean ok=true;
        try {
            Shell shell = new Shell(false);
            shell.removeResultsHandler("queryId25");
        }catch (Exception e){
            fail("testRemoveResultsHandler failed.");
        }
        Assert.assertEquals(true, ok, "testRemoveResultsHandler failed.");
    }

    @Test
    public void testShellDisConnectWithoutServer()  {
        boolean ok=true;
        Shell crossdataSh = new Shell(false);
        try {
            crossdataSh.closeConsole();
        } catch (Exception e) {
            fail("An error happened in sh");
        }
        assertEquals(true, ok, "testShellDisConnectWithoutServer failed.");
    }

    @Test
    public void testUpdatePrompt()  {
        boolean ok=true;
        try {
            Shell shell = new Shell(false);
            QueryResult result = QueryResult.createQueryResult(new ResultSet());
            result.setCurrentCatalog("catalogTest");
            shell.updatePrompt(result);
        }catch(Exception e){
            fail("testUpdatePrompt failed.");
        }
        assertEquals(true, ok, "testUpdatePrompt failed.");
    }

    @Test
    public void testPrintln()  {
        boolean ok=true;
        Shell crossdataSh = new Shell(false);
        try {
            crossdataSh.println("test");
        }catch (Exception e){
            fail("An error happened in sh");
        }
        assertEquals(ok, true);
    }

    @Test
    public void testExecuteScript()  {
        boolean ok=true;
        Shell shell = new Shell(false);
        try {
            shell.executeScript("/path/script.xdql");
        }catch (Exception e) {
            fail("testUpdatePrompt failed.");
        }
        assertEquals(true, ok, "testUpdatePrompt failed.");

    }

}
