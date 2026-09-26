package de.eulenfunk.gonzowa;
import android.app.Activity; import android.os.Bundle; import android.os.Handler; import android.widget.*; import android.content.Intent; import android.provider.Settings;
public class MainActivity extends Activity {
  TextView st; Handler h = new Handler();
  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    android.widget.LinearLayout l = new android.widget.LinearLayout(this);
    l.setOrientation(android.widget.LinearLayout.VERTICAL);
    st = new TextView(this);
    st.setText("GONZO-WA\n\n1) DIENST STARTEN druecken\n2) Tap-Dienst aktivieren\n");
    st.setTextSize(16); st.setPadding(40,80,40,40);
    Button start = new Button(this); start.setText("DIENST STARTEN");
    l.addView(st); l.addView(start);
    setContentView(l);
    Runnable refresh = new Runnable() { @Override public void run() {
      st.setText("Dienst: LAEUFT (Statusleiste beachten)\nTap-Dienst: " + (TapService.aktiv ? "AN" : "AUS - jetzt aktivieren!"));
      h.postDelayed(this, 2000);
    }};
    h.post(refresh);
    start.setOnClickListener(v -> {
      startForegroundService(new Intent(this, PollerService.class));
      if (!TapService.aktiv) startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    });
  }
}