package de.eulenfunk.gonzowa;
import android.app.*; import android.content.Intent; import android.net.Uri; import android.os.*;
import org.json.*;
import java.io.*; import java.net.*; import java.nio.charset.StandardCharsets;
public class PollerService extends Service {
  static final String BASIS = "https://eulen-funk.de/zai";
  static final String KEY = "bfa59398c0e5004eb02bb6209d118d1c458b56a62af3cde4a993983b9108b574";
  Handler h = new Handler();
  int fehler = 0;
  Runnable loop = new Runnable() { @Override public void run() { poll(); h.postDelayed(this, 60000); } };
  @Override public IBinder onBind(Intent i) { return null; }
  @Override public int onStartCommand(Intent i, int f, int id) {
    NotificationChannel ch = new NotificationChannel("gonzo", "Gonzo", NotificationManager.IMPORTANCE_LOW);
    ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
    Notification n = new Notification.Builder(this, "gonzo").setContentTitle("Gonzo-WA").setContentText("Dienst laeuft").setSmallIcon(android.R.drawable.ic_dialog_email).build();
    startForeground(1, n);
    h.removeCallbacksAndMessages(null); h.post(loop);
    return START_STICKY;
  }
  void poll() {
    new Thread(() -> {
      try {
        JSONObject j = hole(BASIS + "/api/index.php?op=wa_holen");
        fehler = 0;
        JSONArray arr = j.optJSONArray("auftraege");
        if (arr != null) for (int i = 0; i < arr.length(); i++) {
          JSONObject a = arr.getJSONObject(i);
          if (!"QUEUED".equals(a.optString("status"))) continue;
          String ziel = a.optString("ziel");
          String nurZiffern = ziel.replaceAll("[^0-9]", "");
          if (nurZiffern.length() < 5) {
            // Keine Telefonnummer (z.B. Mail) -> als FEHLER quittieren, Queue frei machen
            meldung(a.optString("wa_id"), "FEHLER");
            continue;
          }
          TapService.auftrag = nurZiffern + "|" + a.optString("text") + "|" + a.optString("wa_id");
          TapService.schritt = 0;
          // Direkt-Intent: WhatsApp Business zuerst, dann normal, dann wa.me
          Intent wa = new Intent(Intent.ACTION_VIEW,
            Uri.parse("https://api.whatsapp.com/send?phone=" + nurZiffern));
          wa.setPackage("com.whatsapp.w4b");
          wa.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          try { startActivity(wa); }
          catch (Exception e1) {
            try { wa.setPackage("com.whatsapp"); startActivity(wa); }
            catch (Exception e2) { wa.setPackage(null); startActivity(wa); }
          }
          return;
        }
      } catch (Exception e) { fehler++; if (fehler >= 3) { h.removeCallbacksAndMessages(null); h.postDelayed(loop, 20000); fehler = 0; } }
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