package de.visionera.androidbuch.amando5.kontakt;

import android.content.Context;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.text.StringCharacterIterator;

/**
 * @author Arno Becker, 2015 visionera GmbH
 */
public class TelefonnummernHelfer {
  
  private static final String TAG = "TelefonnummernHelfer";

  public static String ermittleEigeneMobilnummer(
      final Context context) {
    TelephonyManager telephonyManager = (TelephonyManager)
        context.getSystemService(Context.TELEPHONY_SERVICE);
    String tel = telephonyManager.getLine1Number();
    
    Log.d(TAG, "ermittleEigeneMobilnummer(): Eigene Telefonnummer: " + tel);

    return tel;
  }

  public static boolean compare(String a, String b) {
    return PhoneNumberUtils.compare(a, b);
  }

  /**
   * Entfernt alle Zeichen aus der Telefonnummer.
   * 
   * Beispiel: (+49) 228-2345678
   * Wird zu: 00492282345678
   * 
   * @param phoneNumber Original-Telefonnummer
   * @return bereinigte Telefonnummer
   */
  public static String bereinigeTelefonnummer(String phoneNumber) {
    
    if (phoneNumber == null) {
      return phoneNumber;
    }
    
    phoneNumber = phoneNumber.replaceFirst("\\+", "00");
    StringCharacterIterator sci = new StringCharacterIterator(
        phoneNumber);
    char c;
    StringBuilder sb = new StringBuilder();        
    while ((c = sci.current()) != StringCharacterIterator.DONE) {
      if (Character.isDigit(c)) {
        sb.append(c);
      }
      sci.next();
    }
    Log.d(TAG, "bereinigeTelefonnummer(): Telefonnummer bereinigt: "
        + sb.toString());
    return sb.toString();
  }
}
