package de.eulenfunk.gonzowa;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent; import android.view.accessibility.AccessibilityNodeInfo;
import android.os.*; import android.content.Intent;
public class TapService extends AccessibilityService {
  public static boolean aktiv = false;
  public static boolean nurSenden = false;
  public static String waId = null;
  long letzterTipp = 0; int versuche = 0;
  Handler h = new Handler();
  @Override protected void onServiceConnected() { aktiv = true; }
  @Override public boolean onUnbind(Intent i) { aktiv = false; return false; }
  @Override public void onAccessibilityEvent(AccessibilityEvent e) {
    if (!nurSenden || waId == null) return;
    String pkg = "" + e.getPackageName();
    if (!pkg.contains("whatsapp")) return;
    long jetzt = System.currentTimeMillis();
    if (jetzt - letzterTipp < 2500) return;
    letzterTipp = jetzt;
    h.postDelayed(() -> sendenKlick(), 2000);
  }
  void sendenKlick() {
    AccessibilityNodeInfo w = getRootInActiveWindow(); if (w == null) return;
    AccessibilityNodeInfo s = findeSenden(w);
    if (s != null) {
      s.performAction(AccessibilityNodeInfo.ACTION_CLICK);
      PollerService.meldung(waId, "KOPIE_ZUGESTELLT");
      nurSenden = false; waId = null; versuche = 0;
    } else { versuche++; if (versuche > 10) { nurSenden = false; waId = null; versuche = 0; } }
  }
  AccessibilityNodeInfo findeSenden(AccessibilityNodeInfo n) {
    if (n == null) return null;
    CharSequence d = n.getContentDescription();
    if (d != null) { String s = d.toString().toLowerCase(); if (s.contains("senden") || s.contains("send")) return n; }
    String id = n.getViewIdResourceName();
    if (id != null && id.contains("send")) return n;
    for (int i = 0; i < n.getChildCount(); i++) { AccessibilityNodeInfo r = findeSenden(n.getChild(i)); if (r != null) return r; }
    return null;
  }
  @Override public void onInterrupt() {}
}