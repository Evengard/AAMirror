package com.github.slashmax.aamirror;

import android.graphics.Point;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class MinitouchSocket {
    private static final String TAG = "MinitouchSocket";
    private static final String DEFAULT_SOCKET_NAME = "minitouch";

    private static MinitouchSocket instance;

    private LocalSocket m_SocketLocal;
    private Socket m_SocketTcp;
    private OutputStream m_Output;

    private int Version;
    private int MaxContact;
    private double MaxX;
    private double MaxY;
    private double MaxPressure;
    private int pid;

    private double m_ProjectionOffsetX;
    private double m_ProjectionOffsetY;
    private double m_ProjectionWidth;
    private double m_ProjectionHeight;
    private double m_TouchXScale;
    private double m_TouchYScale;

    private MinitouchSocket() {
        super();
    }

    static MinitouchSocket getInstance() {
        if (instance == null) {
            instance = new MinitouchSocket();
        }
        return instance;
    }

    boolean connect(boolean local) {
        Log.d(TAG, "connect");
        if (local) {
            return connectLocal();
        } else {
            return connectTcp();
        }
    }

    void disconnect() {
        Log.d(TAG, "disconnect");
        disconnectLocal();
        disconnectTcp();
    }

    boolean isConnected() {
        return isConnectedLocal() || isConnectedTcp();
    }

    private boolean connectLocal() {
        Log.d(TAG, "connectLocal");

        disconnectLocal();
        LocalSocket socket = new LocalSocket();
        try {
            socket.connect(new LocalSocketAddress(DEFAULT_SOCKET_NAME));
            if (inputReadParams(socket.getInputStream())) {
                m_Output = socket.getOutputStream();
                m_SocketLocal = socket;
            } else {
                socket.close();
            }
        } catch (Exception e) {
            Log.d(TAG, "connectLocal exception: " + e.toString());
            m_SocketLocal = null;
        }
        return isConnectedLocal();
    }

    private void disconnectLocal() {
        Log.d(TAG, "disconnectLocal");
        if (isConnectedLocal()) {
            try {
                m_SocketLocal.close();
            } catch (Exception e) {
                Log.d(TAG, "disconnectLocal exception: " + e.toString());
            }
            m_Output = null;
            m_SocketLocal = null;
        }
    }

    private boolean isConnectedLocal() {
        return (m_SocketLocal != null);
    }

    private boolean connectTcp() {
        Log.d(TAG, "connectTcp");

        disconnectTcp();
        try {
            InetAddress serverAddress = InetAddress.getByName("127.0.0.1");
            Socket socket = new Socket(serverAddress, 1111);
            if (inputReadParams(socket.getInputStream())) {
                m_Output = socket.getOutputStream();
                m_SocketTcp = socket;
            } else {
                socket.close();
            }
        } catch (Exception e) {
            Log.d(TAG, "connectTcp exception: " + e.toString());
            m_SocketTcp = null;
        }
        return isConnectedTcp();
    }

    private void disconnectTcp() {
        Log.d(TAG, "disconnectTcp");
        if (isConnectedTcp()) {
            try {
                m_SocketTcp.close();
            } catch (Exception e) {
                Log.d(TAG, "disconnectTcp exception: " + e.toString());
            }
            m_Output = null;
            m_SocketTcp = null;
        }
    }

    private boolean isConnectedTcp() {
        return (m_SocketTcp != null);
    }

    int getPid() {
        Log.d(TAG, "getPid: " + pid);
        return pid;
    }

    private boolean inputReadParams(InputStream stream) {
        Log.d(TAG, "inputReadParams");
        byte[] data_buffer = new byte[128];

        pid = 0;

        try {
            if (stream.read(data_buffer) == -1) {
                Log.d(TAG, "inputReadParams read error");
                return false;
            }
        } catch (Exception e) {
            Log.d(TAG, "inputReadParams read exception: " + e.toString());
            return false;
        }

        String data_string = new String(data_buffer);
        String[] lines = data_string.split("\n");

        if (lines.length < 3) {
            Log.d(TAG, "inputReadParams error: less then 3 lines");
            return false;
        }

        String[] version_line = lines[0].split(" ");
        if (version_line.length == 2) {
            Version = Integer.parseInt(version_line[1]);
        }
        String[] limits_line = lines[1].split(" ");
        if (limits_line.length == 5) {
            MaxContact = Integer.parseInt(limits_line[1]);
            MaxX = Integer.parseInt(limits_line[2]);
            MaxY = Integer.parseInt(limits_line[3]);
            MaxPressure = Integer.parseInt(limits_line[4]);
        }
        String[] pid_line = lines[2].split(" ");
        if (pid_line.length == 2) {
            pid = Integer.parseInt(pid_line[1]);
        }

        Log.d(TAG, "inputReadParams: pid " + pid);
        return true;
    }

    private boolean outputWrite(String command) {
        if (m_Output == null) {
            return false;
        }

        boolean ok = true;
        try {
            m_Output.write(command.getBytes());
        } catch (Exception e) {
            ok = false;
        }
        return ok;
    }

    private boolean validateBounds(double x, double y) {
        return (x >= 0.0 && x < MaxX && y >= 0.0 && y < MaxY);
    }

    void updateTouchTransformations(double screenWidth, double screenHeight, Point displaySize) {
        double displayWidth = displaySize.x;
        double displayHeight = displaySize.y;
        double factX = displayWidth / screenWidth;
        double factY = displayHeight / screenHeight;

        double fact = (factX < factY ? factX : factY);

        m_ProjectionWidth = fact * screenWidth;
        m_ProjectionHeight = fact * screenHeight;

        m_ProjectionOffsetX = (displayWidth - m_ProjectionWidth) / 2.0;
        m_ProjectionOffsetY = (displayHeight - m_ProjectionHeight) / 2.0;

        m_TouchXScale = MaxX / displayWidth;
        m_TouchYScale = MaxY / displayHeight;
    }

    boolean touchDown(int id, double x, double y, double pressure) {
        x = (m_ProjectionOffsetX + x * m_ProjectionWidth) * m_TouchXScale;
        y = (m_ProjectionOffsetY + y * m_ProjectionHeight) * m_TouchYScale;

        if (!validateBounds(x, y)) {
            return true;
        }

        pressure = pressure * MaxPressure;
        return outputWrite(String.format("d %d %d %d %d\n", id, (int) x, (int) y, (int) pressure));
    }

    boolean touchMove(int id, double x, double y, double pressure) {
        x = (m_ProjectionOffsetX + x * m_ProjectionWidth) * m_TouchXScale;
        y = (m_ProjectionOffsetY + y * m_ProjectionHeight) * m_TouchYScale;

        if (!validateBounds(x, y)) {
            return true;
        }

        pressure = pressure * MaxPressure;
        return outputWrite(String.format("m %d %d %d %d\n", id, (int) x, (int) y, (int) pressure));
    }

    boolean touchUp(int id) {
        return outputWrite(String.format("u %d\n", id));
    }

    boolean touchUpAll() {
        boolean ok = true;
        for (int i = 0; i < MaxContact; i++) {
            ok = ok && touchUp(i);
        }
        return ok;
    }

    boolean touchCommit() {
        return outputWrite("c\n");
    }

    public boolean touchReset() {
        return outputWrite("r\n");
    }
}
