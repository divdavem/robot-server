package com.ariatemplates.robotserver;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;

public class RobotServer {
    private static final String ROBOT_SERVER_SCRIPT;
    private static final String ROBOT_SERVER_INDEX;
    static {
        try {
            ROBOT_SERVER_SCRIPT = IOUtils.toString(RobotServer.class.getResource("robotServer.js"));
            ROBOT_SERVER_INDEX = IOUtils.toString(RobotServer.class.getResource("index.html"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final int DEFAULT_COLOR_TOLERANCE = 50;
    private static final Color CALIBRATION_COLOR = new Color(255, 0, 0);

    private final Robot robot;
    private final Server server;

    private static interface Method {
        JsonElement run(RobotServer executor, JsonReader parameters) throws Exception;
    }

    private static int nextInt(JsonReader parameters) throws Exception {
        return (int) parameters.nextDouble();
    }

    private static final Map<String, Method> methods;
    static {
        methods = new HashMap<String, RobotServer.Method>();
        methods.put("/robot/mouseMove", new Method() {
            public JsonElement run(RobotServer executor, JsonReader parameters) throws Exception {
                int x = nextInt(parameters);
                int y = nextInt(parameters);
                executor.robot.mouseMove(x, y);
                return null;
            }
        });

        methods.put("/robot/smoothMouseMove", new Method() {
            public JsonElement run(RobotServer executor, JsonReader parameters) throws Exception {
                int fromX = nextInt(parameters);
                int fromY = nextInt(parameters);
                int toX = nextInt(parameters);
                int toY = nextInt(parameters);
                int duration = nextInt(parameters);
                SmoothMouseMove.smoothMouseMove(executor.robot, fromX, fromY, toX, toY, duration);
                return null;
            }
        });

        methods.put("/robot/mousePress", new Method() {
            public JsonElement run(RobotServer executor, JsonReader parameters) throws Exception {
                int buttons = nextInt(parameters);
                executor.robot.mousePress(buttons);
                return null;
            }
        });

        methods.put("/robot/mouseRelease", new Method() {
            public JsonElement run(RobotServer executor, JsonReader parameters) throws Exception {
                int buttons = nextInt(parameters);
                executor.robot.mouseRelease(buttons);
                return null;
            }
        });

        methods.put("/robot/mouseWheel", new Method() {
            public JsonElement run(RobotServer executor, JsonReader parameters) throws Exception {
                int amount = nextInt(parameters);
                executor.robot.mouseWheel(amount);
                return null;
            }
        });

        methods.put("/robot/keyPress", new Method() {
            public JsonElement run(RobotServer executor, JsonReader parameters) throws Exception {
                int keyCode = nextInt(parameters);
                executor.robot.keyPress(keyCode);
                return null;
            }
        });

        methods.put("/robot/keyRelease", new Method() {
            public JsonElement run(RobotServer executor, JsonReader parameters) throws Exception {
                int keyCode = nextInt(parameters);
                executor.robot.keyRelease(keyCode);
                return null;
            }
        });

        methods.put("/robot/calibrate", new Method() {
            public JsonElement run(RobotServer executor, JsonReader parameters) throws Exception {
                int expectedWidth = nextInt(parameters);
                int expectedHeight = nextInt(parameters);
                Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                Rectangle rectangle = RectangleFinder.findRectangle(executor.robot, CALIBRATION_COLOR, screenRect, expectedWidth, expectedHeight,
                        DEFAULT_COLOR_TOLERANCE);
                if (rectangle == null) {
                    throw new RuntimeException("Calibration failed.");
                }
                JsonObject res = new JsonObject();
                res.addProperty("x", rectangle.x);
                res.addProperty("y", rectangle.y);
                return res;
            }
        });
    }

    private static void numLockStateWorkaround() {
        try {
            // Shift key is not kept pressed while using keyPress method
            // cf https://forums.oracle.com/thread/2232592
            // The workaround is to use the following line:
            Toolkit.getDefaultToolkit().setLockingKeyState(KeyEvent.VK_NUM_LOCK, false);
            System.out.println("Num lock state was successfully changed.");
        } catch (UnsupportedOperationException e) {
            System.out.println("Did not change num lock state: " + e);
        }
    }

    public RobotServer() throws AWTException {
        numLockStateWorkaround();
        this.robot = new Robot();
        this.server = new Server();
        server.setHandler(new RobotServerHandler());
    }

    public Server getServer() {
        return server;
    }

    class RobotServerHandler extends AbstractHandler {
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
            System.out.println("Serving " + target);
            if (!("GET".equals(baseRequest.getMethod()))) {
                response.sendError(405);
                return;
            }
            if ("/robot".equals(target)) {
                String content = ROBOT_SERVER_SCRIPT.replace("var SERVER_URL = '';", "var SERVER_URL = '" + request.getRequestURL() + "';");
                response.setStatus(200);
                response.setContentType("application/javascript");
                PrintWriter writer = response.getWriter();
                writer.write(content);
                writer.close();
                return;
            } else if ("/".equals(target)) {
                response.setStatus(200);
                response.setContentType("text/html");
                PrintWriter writer = response.getWriter();
                writer.write(ROBOT_SERVER_INDEX);
                writer.close();
                return;
            }
            Method method = methods.get(target);
            if (method == null) {
                response.sendError(404);
                return;
            }
            Integer id = Integer.valueOf(baseRequest.getParameter("id"), 10);
            String data = baseRequest.getParameter("data");
            response.setContentType("application/javascript");
            response.setStatus(200);
            boolean success = true;
            JsonElement result = null;
            try {
                JsonReader reader = new JsonReader(new StringReader(data));
                reader.beginArray();
                result = method.run(RobotServer.this, reader);
                reader.endArray();
                reader.close();
            } catch (Exception e) {
                success = false;
                result = new JsonPrimitive(e.getMessage() + " when executing " + target + " with data = " + data);
            }
            PrintWriter writer = response.getWriter();
            if (result != null) {
                Gson gson = new Gson();
                writer.write("SeleniumJavaRobot._response(" + id + "," + success + "," + gson.toJson(result) + ")");
            } else {
                writer.write("SeleniumJavaRobot._response(" + id + "," + success + ",null)");
            }
            writer.close();
        }
    }
}
