package cn.toside.music.mobile.tv;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Base64;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** LAN text input: the TV shows a QR code, a phone on the same Wi-Fi opens it and submits text. */
public class TVRemoteInputModule extends ReactContextBaseJavaModule {
  private static final int MAX_BODY = 8 * 1024;
  private ServerSocket server;
  private String token;

  TVRemoteInputModule(ReactApplicationContext context) { super(context); }
  @Override public String getName() { return "TVRemoteInput"; }

  @ReactMethod public synchronized void start(String title, Promise promise) {
    try {
      stopServer();
      String ip = lanAddress();
      if (ip == null) { promise.reject("no_network", "电视未连接局域网"); return; }
      byte[] bytes = new byte[12];
      new SecureRandom().nextBytes(bytes);
      token = Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
      server = new ServerSocket(0);
      ServerSocket current = server;
      String currentToken = token;
      String safeTitle = title == null ? "" : title;
      Thread thread = new Thread(() -> serve(current, currentToken, safeTitle), "tv-remote-input");
      thread.setDaemon(true);
      thread.start();
      String url = "http://" + ip + ":" + server.getLocalPort() + "/" + token;
      WritableMap result = Arguments.createMap();
      result.putString("url", url);
      result.putString("qr", "data:image/png;base64," + qrPng(url));
      promise.resolve(result);
    } catch (Exception e) {
      stopServer();
      promise.reject("start_failed", e.getMessage());
    }
  }

  @ReactMethod public synchronized void stop() { stopServer(); }

  @ReactMethod public void addListener(String eventName) {}
  @ReactMethod public void removeListeners(double count) {}

  @Override public void invalidate() { stop(); super.invalidate(); }

  private void stopServer() {
    if (server != null) {
      try { server.close(); } catch (Exception ignored) {}
      server = null;
    }
    token = null;
  }

  private static String lanAddress() throws Exception {
    String fallback = null;
    for (NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
      if (!nif.isUp() || nif.isLoopback()) continue;
      for (InetAddress address : Collections.list(nif.getInetAddresses())) {
        if (!(address instanceof Inet4Address) || address.isLoopbackAddress()) continue;
        if (address.isSiteLocalAddress()) return address.getHostAddress();
        if (fallback == null) fallback = address.getHostAddress();
      }
    }
    return fallback;
  }

  private static String qrPng(String text) throws Exception {
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.MARGIN, 1);
    hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 360, 360, hints);
    int w = matrix.getWidth(), h = matrix.getHeight();
    int[] pixels = new int[w * h];
    for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) pixels[y * w + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
    Bitmap bitmap = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.RGB_565);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
  }

  private void serve(ServerSocket socket, String expectedToken, String title) {
    while (!socket.isClosed()) {
      try (Socket client = socket.accept()) {
        client.setSoTimeout(10000);
        handle(client, expectedToken, title);
      } catch (Exception ignored) {}
    }
  }

  private void handle(Socket client, String expectedToken, String title) throws Exception {
    InputStream in = client.getInputStream();
    String requestLine = readLine(in);
    if (requestLine == null) return;
    int contentLength = 0;
    for (String line; (line = readLine(in)) != null && !line.isEmpty();) {
      int colon = line.indexOf(':');
      if (colon > 0 && line.substring(0, colon).trim().equalsIgnoreCase("Content-Length")) {
        contentLength = Integer.parseInt(line.substring(colon + 1).trim());
      }
    }
    String[] parts = requestLine.split(" ");
    if (parts.length < 2) return;
    String method = parts[0], path = parts[1];
    OutputStream out = client.getOutputStream();
    if (!path.equals("/" + expectedToken)) { respond(out, 404, "text/plain", "Not Found"); return; }
    if (method.equals("GET")) { respond(out, 200, "text/html", page(title, null)); return; }
    if (!method.equals("POST") || contentLength <= 0 || contentLength > MAX_BODY) { respond(out, 400, "text/plain", "Bad Request"); return; }
    byte[] body = new byte[contentLength];
    int read = 0;
    while (read < contentLength) {
      int n = in.read(body, read, contentLength - read);
      if (n < 0) break;
      read += n;
    }
    String text = "";
    for (String pair : new String(body, 0, read, StandardCharsets.UTF_8).split("&")) {
      if (pair.startsWith("text=")) text = URLDecoder.decode(pair.substring(5), "UTF-8").trim();
    }
    if (text.isEmpty()) { respond(out, 200, "text/html", page(title, "内容为空，请重新输入")); return; }
    getReactApplicationContext()
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit("tvRemoteInput", text);
    respond(out, 200, "text/html", page(title, "已发送到电视"));
  }

  private static String readLine(InputStream in) throws Exception {
    ByteArrayOutputStream line = new ByteArrayOutputStream();
    for (int c; (c = in.read()) != -1;) {
      if (c == '\n') return line.toString("UTF-8").replace("\r", "");
      if (line.size() > MAX_BODY) throw new IllegalStateException("header too long");
      line.write(c);
    }
    return line.size() == 0 ? null : line.toString("UTF-8");
  }

  private static String escape(String s) {
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }

  private static String page(String title, String message) {
    return "<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
        + "<title>LX Music TV</title><style>body{font-family:sans-serif;margin:24px;background:#111;color:#eee}"
        + "textarea{width:100%;box-sizing:border-box;height:140px;font-size:16px;padding:10px;border-radius:8px}"
        + "button{width:100%;margin-top:14px;padding:14px;font-size:18px;border:0;border-radius:8px;background:#ffca4a;color:#111}"
        + "p{color:#ffca4a}</style></head><body><h2>" + escape(title) + "</h2>"
        + (message == null ? "" : "<p>" + escape(message) + "</p>")
        + "<form method=\"post\"><textarea name=\"text\" placeholder=\"粘贴歌单链接或 ID\" autofocus></textarea>"
        + "<button type=\"submit\">发送到电视</button></form></body></html>";
  }

  private static void respond(OutputStream out, int code, String type, String body) throws Exception {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    String status = code == 200 ? "OK" : code == 404 ? "Not Found" : "Bad Request";
    String head = "HTTP/1.1 " + code + " " + status + "\r\nContent-Type: " + type + "; charset=utf-8\r\nContent-Length: "
        + bytes.length + "\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n";
    out.write(head.getBytes(StandardCharsets.UTF_8));
    out.write(bytes);
    out.flush();
  }
}
