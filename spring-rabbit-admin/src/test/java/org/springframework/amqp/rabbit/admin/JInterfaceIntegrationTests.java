/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.amqp.rabbit.admin;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.erlang.connection.SingleConnectionFactory;
import org.springframework.erlang.core.ErlangTemplate;
import org.springframework.util.exec.Os;

import com.ericsson.otp.erlang.OtpAuthException;
import com.ericsson.otp.erlang.OtpConnection;
import com.ericsson.otp.erlang.OtpErlangBinary;
import com.ericsson.otp.erlang.OtpErlangExit;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpPeer;
import com.ericsson.otp.erlang.OtpSelf;


public class JInterfaceIntegrationTests {

	private static Log logger = LogFactory.getLog(JInterfaceIntegrationTests.class);

	private static int counter;

	@Test
	public void rawApi() {
		OtpConnection connection = null;
		try {
			OtpSelf self = new OtpSelf("rabbit-monitor");

			String hostName = "rabbit@" + getHostName();
			OtpPeer peer = new OtpPeer(hostName);
			connection = self.connect(peer);
			// connection.sendRPC("erlang","date", new OtpErlangList());
			// connection.sendRPC("rabbit_access_control", "list_vhosts", new
			// OtpErlangList());
			OtpErlangObject[] objectArray = { new OtpErlangBinary("/".getBytes()) };

			connection.sendRPC("rabbit_amqqueue", "info_all", new OtpErlangList(objectArray));

			// connection.sendRPC("rabbit_amqqueue", "stat_all", new
			// OtpErlangList());

			OtpErlangObject received = connection.receiveRPC();
			System.out.println(received);
			System.out.println(received.getClass());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OtpAuthException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OtpErlangExit e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (connection != null) {
				connection.close();
			}
		}

	}

	@Test
	public void otpTemplate() throws UnknownHostException {
		String selfNodeName = "rabbit-monitor";
		String peerNodeName = "rabbit@" + getHostName();

		// String home = System.getProperty("user.home");
		// System.out.println("home = " + home);
		// System.out.println("peerNodeName = " + peerNodeName);

		SingleConnectionFactory cf = new SingleConnectionFactory(selfNodeName, peerNodeName);

		cf.afterPropertiesSet();
		ErlangTemplate template = new ErlangTemplate(cf);
		template.afterPropertiesSet();

		// OtpErlangObject result = template.executeRpc("rabbit_amqqueue", "info_all", "/".getBytes());
		// System.out.println(result);
		// System.out.println(result.getClass());

		long number = (Long) template.executeAndConvertRpc("erlang", "abs", -161803399);
		Assert.assertEquals(161803399, number);

		cf.destroy();

	}

	private String getHostName() throws UnknownHostException {
		String hostName = InetAddress.getLocalHost().getHostName();
		if (Os.isFamily("windows")) {
			hostName = hostName.toUpperCase();
		}
		return hostName;
	}

	@Test
	public void rawOtpConnect() throws Exception {
		createConnection();
	}

	@Test
	public void stressTest() throws Exception {
		OtpConnection con = createConnection();
		boolean recycleConnection = false;
		for (int i = 0; i < 100; i++) {
			executeRpc(con, recycleConnection, "rabbit", "status");
			executeRpc(con, recycleConnection, "rabbit", "stop");
			executeRpc(con, recycleConnection, "rabbit", "status");
			executeRpc(con, recycleConnection, "rabbit", "start");
			executeRpc(con, recycleConnection, "rabbit", "status");
			if (i % 10 == 0) {
				logger.debug("i = " + i);
			}
		}
	}

	public OtpConnection createConnection() throws Exception {
		OtpSelf self = new OtpSelf("rabbit-monitor-" + counter++);
		OtpPeer peer = new OtpPeer("rabbit@" + getHostName());
		return self.connect(peer);
	}

	private void executeRpc(OtpConnection con, boolean recycleConnection, String module, String function)
			throws Exception, UnknownHostException {
		con.sendRPC(module, function, new OtpErlangList());
		con.receiveRPC();
		if (recycleConnection) {
			con.close();
			con = createConnection();
		}
	}

}
