package de.visionera.androidbuch.amando5.gui;

import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

public class StartseiteTest extends
    ActivityInstrumentationTestCase2<Startseite> { // (1)

  private Solo mSimulator; // (2)

  public StartseiteTest() {
    // (3)
    super(Startseite.class);
  }

  public void setUp() throws Exception {
    mSimulator = // (4)
        new Solo(getInstrumentation(), getActivity());
  }

  public void testGeokontaktAnzeigen()
      throws Exception {
    mSimulator.clickOnButton("GeokontakteHurensöhne"); // (5)
    mSimulator.assertCurrentActivity(        // (6)
        "GeoKontakteAuflisten erwartet",
        GeoKontakteAuflisten.class);

    // Kontakt auswählen
    mSimulator.clickOnText("Berthold Schmitz"); // (7)
    assertTrue(
        "Kontaktmaske nicht angezeigt",
        mSimulator.searchText("Name:")); // (8)
    assertTrue(
        "Falscher Kontakt",
        mSimulator.searchText("Berthold Schmitz"));
    mSimulator.goBack(); // (9)
    mSimulator.goBack();
    assertTrue(
        "Text auf Startseite nicht ok",
        mSimulator.searchText( // (10)
            "Sie haben folgende Optionen .*"));
  }
}
