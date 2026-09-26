package de.eulenfunk.gonzowa;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent; import android.view.accessibility.AccessibilityNodeInfo;
import android.os.*; import android.content.Intent;
import android.os.Bundle;
public class TapService extends AccessibilityService {
  public static boolean aktiv = false;
  public static String auftrag = null;
  public static int schritt = 0;
  long letzterTipp = 0;
  Handler h = new Handler();
  @Override protected void onServiceConnected() { aktiv = true; }
  @Override public boolean onUnbind(Intent i) { aktiv = false; return false; }
  @Override public void onAccessibilityEvent(AccessibilityEvent e) {
    if (auftrag == null) return;
    String pkg = "" + e.getPackageName();
    if (!pkg.contains("whatsapp")) return;
    long jetzt = System.currentTimeMillis();
    if (jetzt - letzterTipp < 2500) return;
    letzterTipp = jetzt;
    h.postDelayed(() -> arbeit(), 2000);
  }
  void arbeit() {
    if (auftrag == null) return;
    String[] teile = auftrag.split("\\|", 3);
    AccessibilityNodeInfo w = getRootInActiveWindow(); if (w == null) return;
    if (schritt == 0) {
      AccessibilityNodeInfo f = findeFeld(w);
      if (f != null) {
        Bundle b = new Bundle(); b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, teile[1]);
        f.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b);
        schritt = 1;
      } else { versucheMax++; if (versucheMax > 8) { gebeAuf("kein Textfeld"); } }
      return;
    }
    if (schritt == 1) {
      AccessibilityNodeInfo s = findeSenden(w);
      if (s != null) {
        s.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        schritt = 2;
        PollerService.meldung(teile[2], "GESENDET");
        h.postDelayed(() -> { PollerService.meldung(teile[2], "KOPIE_ZUGESTELLT"); auftrag = null; schritt = 0; versucheMax = 0; }, 90000);
      } else { versucheMax++; if (versucheMax > 8) { gebeAuf("kein Senden-Knopf"); } }
    }
  }
  int versucheMax = 0;
  void gebeAuf(String grund) {
    String[] teile = auftrag.split("\\|", 3);
    PollerService.meldung(teile[2], "FEHLER");
    auftrag = null; schritt = 0; versucheMax = 0;
  }
  AccessibilityNodeInfo findeFeld(AccessibilityNodeInfo n) {
    if (n == null) return null;
    if (n.isEditable() && n.getClassName() != null && n.getClassName().toString().contains("EditText")) return n;
    for (int i = 0; i < n.getChildCount(); i++) { AccessibilityNodeInfo r = findeFeld(n.getChild(i)); if (r != null) return r; }
    return null;
  }
  AccessibilityNodeInfo findeSenden(AccessibilityNodeInfo n) {
    if (n == null) return null;
    CharSequence d = n.getContentDescription();
    if (d != null) { String s = d.toString().toLowerCase();
      if (s.contains("senden") || s.contains("send") || s.contains("message")) return n; }
    CharSequence t = n.getText();
    if (t != null) { String s = t.toString().toLowerCase();
      if (s.contains("senden") || s.contains("send")) return n; }
    String id = n.getViewIdResourceName();
    if (id != null && id.contains("send")) return n;
    for (int i = 0; i < n.getChildCount(); i++) { AccessibilityNodeInfo r = findeSenden(n.getChild(i)); if (r != null) return r; }
    return null;
  }
  @Override public void onInterrupt() {}
}