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

import scala.concurrent.duration.DurationInt

import org.scalatest.FunSuiteLike

import com.stratio.meta.server.config.ActorReceiveUtils
import com.stratio.meta2.server.actors.CoordinatorActor

import akka.actor.actorRef2Scala

//class CoordinatorActorTest extends ActorReceiveUtils with FunSuiteLike with MockFactory{
class CoordinatorActorTest extends ActorReceiveUtils with FunSuiteLike {

		test("Basic Coordinator Mock test") {
		  within(5000 millis){
	  		val coordinatorActor=system.actorOf(CoordinatorActor.props(),"CoordinatorActor") 
	  		coordinatorActor! "Hola"
	  		//expectMsg("Hola") // bounded to 1 second
	  		//expectMsg("Hola") // bounded to the remainder of the 1 second

	  		//val m = mock[IConnector]
	  		//(m.getConnectorName _).expects().returning("My New Connector")
	  		//assert(m.getConnectorName().equals("My New Connector"))
	  		}
		}

}