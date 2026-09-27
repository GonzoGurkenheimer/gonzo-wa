package de.eulenfunk.gonzowa;
import android.app.*; import android.content.Intent; import android.net.Uri; import android.os.*;
import org.json.*;
import java.io.*; import java.net.*; import java.nio.charset.StandardCharsets;
import java.util.*;
public class PollerService extends Service {
  static final String BASIS = "https://eulen-funk.de/zai";
  static final String KEY = "bfa59398c0e5004eb02bb6209d118d1c458b56a62af3cde4a993983b9108b574";
  static final String MEINE_VERSION = "2.00";
  Handler h = new Handler();
  long letzterUpdateCheck = 0;
  Runnable loop = new Runnable() { @Override public void run() { poll(); h.postDelayed(this, 15000); } };
  @Override public IBinder onBind(Intent i) { return null; }
  @Override public int onStartCommand(Intent i, int f, int id) {
    NotificationChannel ch = new NotificationChannel("gonzo", "Gonzo", NotificationManager.IMPORTANCE_LOW);
    ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
    Notification n = new Notification.Builder(this, "gonzo").setContentTitle("Gonzo-WA").setContentText("Dienst laeuft v" + MEINE_VERSION).setSmallIcon(android.R.drawable.ic_dialog_email).build();
    startForeground(1, n);
    h.removeCallbacksAndMessages(null); h.post(loop);
    return START_STICKY;
  }
  boolean istSendefenster() {
    Calendar c = Calendar.getInstance();
    int m = c.get(Calendar.MINUTE);
    return m < 2 || (m >= 30 && m < 32);
  }
  void poll() {
    long jetzt = System.currentTimeMillis();
    if (jetzt - letzterUpdateCheck > 1800000) { letzterUpdateCheck = jetzt; checkUpdate(); }
    if (!istSendefenster()) return;
    new Thread(() -> {
      try {
        JSONObject j = hole(BASIS + "/api/index.php?op=wa_holen");
        JSONArray arr = j.optJSONArray("auftraege");
        if (arr != null) for (int i = 0; i < arr.length(); i++) {
          JSONObject a = arr.getJSONObject(i);
          if (!"QUEUED".equals(a.optString("status"))) continue;
          String ziel = a.optString("ziel");
          String text = a.optString("text");
          String nurZiffern = ziel.replaceAll("[^0-9]", "");
          if (nurZiffern.length() < 5) { meldung(a.optString("wa_id"), "FEHLER"); continue; }
          meldung(a.optString("wa_id"), "GESENDET");
          String enc = URLEncoder.encode(text, "UTF-8");
          Intent wa = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + nurZiffern + "?text=" + enc));
          try { wa.setPackage("com.whatsapp.w4b"); } catch (Exception e) {}
          wa.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          try { startActivity(wa); } catch (Exception e1) {
            try { wa.setPackage("com.whatsapp"); startActivity(wa); } catch (Exception e2) { wa.setPackage(null); startActivity(wa); }
          }
          TapService.nurSenden = true; TapService.waId = a.optString("wa_id");
          return;
        }
      } catch (Exception e) {}
    }).start();
  }
  void checkUpdate() {
    new Thread(() -> {
      try {
        HttpURLConnection c = (HttpURLConnection) new URL(BASIS + "/version.txt").openConnection();
        c.setConnectTimeout(10000); c.setReadTimeout(10000);
        BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
        String server = r.readLine().trim(); r.close();
        if (server.compareTo(MEINE_VERSION) > 0) {
          File apkFile = new File(getExternalFilesDir(null), "gonzo-update.apk");
          URL u = new URL(BASIS + "/gonzo-latest.apk");
          HttpURLConnection c2 = (HttpURLConnection) u.openConnection();
          c2.setConnectTimeout(15000); c2.setReadTimeout(60000);
          InputStream in = c2.getInputStream();
          FileOutputStream out = new FileOutputStream(apkFile);
          byte[] buf = new byte[65536]; int len;
          while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
          out.close(); in.close();
          Intent i = new Intent(Intent.ACTION_VIEW);
          i.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
          i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(i);
        }
      } catch (Exception e) {}
    }).start();
  }
  static JSONObject hole(String url) throws Exception {
    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
    c.setRequestProperty("X-Gonzo-Key", KEY); c.setConnectTimeout(15000); c.setReadTimeout(20000);
    BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
    StringBuilder s = new StringBuilder(); String l; while ((l = r.readLine()) != null) s.append(l);
    r.close(); return new JSONObject(s.toString());
  }
  static void meldung(String waId, String status) {
    new Thread(() -> { try {
      JSONObject o = new JSONObject(); o.put("wa_id", waId); o.put("status", status);
      HttpURLConnection c = (HttpURLConnection) new URL(BASIS + "/api/index.php?op=wa_status").openConnection();
      c.setRequestMethod("POST"); c.setRequestProperty("X-Gonzo-Key", KEY);
      c.setRequestProperty("Content-Type", "application/json"); c.setDoOutput(true);
      OutputStream os = c.getOutputStream(); os.write(o.toString().getBytes(StandardCharsets.UTF_8)); os.close();
      c.getResponseCode();
    } catch (Exception e) {} }).start();
  }
}