/*
 * Copyright 2014 Amadeus s.a.s.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ariatemplates.robotserver;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

public class Main {
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 7778;
        String usageString = String.format("Usage: robot-server [options]\nOptions:\n  --host <host> [default: %s]\n  --port <port> [default: %d]", host, port);

        for (int i = 0, l = args.length; i < l; i++) {
            String curParam = args[i];
            if ("--host".equalsIgnoreCase(curParam) && i + 1 < l) {
                i++;
                host = args[i];
            } else if ("--port".equalsIgnoreCase(curParam) && i + 1 < l) {
                i++;
                port = Integer.parseInt(args[i]);
            } else if ("--version".equalsIgnoreCase(curParam)) {
                System.out.println(Main.class.getPackage().getImplementationVersion());
                return;
            } else if ("--help".equalsIgnoreCase(curParam)) {
                System.out.println(usageString);
                return;
            } else {
                System.err.println("Unknown command line option: " + curParam);
                System.err.println(usageString);
                return;
            }
        }

        System.out.println("Starting robot-server on http://" + host + ":" + port + "/robot");
        RobotServer robotServer = new RobotServer();
        Server server = robotServer.getServer();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        connector.setHost(host);
        server.addConnector(connector);
        server.start();
    }
}
