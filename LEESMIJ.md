# Rechtop – Android-app

Willekeurige seintjes om je houding te checken, een knop "Even weg", en bijhouden hoe lang je aan je bureau zit. Kan automatisch aan en uit op vaste dagen en tijden.

## Bouwen: kies één van twee routes

### Route A: via GitHub (geen installatie nodig, kan ook op je werklaptop)

1. Maak een gratis account op github.com, als je die nog niet hebt.
2. Maak een nieuwe repository, bijvoorbeeld `rechtop`. Privé is prima.
3. Pak deze zip uit en sleep de **inhoud** van de map (dus `app`, `gradle`, `.github`, `gradlew` enzovoort) naar de pagina "uploading an existing file" van je repository. Klik op **Commit changes**.
   - Let op: de map `.github` is in Windows en macOS soms verborgen. Zorg dat hij wel mee-uploadt, anders start de bouw niet.
4. Ga naar het tabblad **Actions**. De bouw "Bouw APK" start vanzelf en duurt een paar minuten.
5. Klik op de afgeronde bouw en download onderaan bij **Artifacts** het bestand `RechtopZitten-apk`. Dat is een zip met `app-debug.apk` erin.

### Route B: met Android Studio (thuis)

1. Installeer Android Studio en open deze map met **File → Open**.
2. Wacht tot Android Studio klaar is met synchroniseren. De eerste keer haalt het van alles op.
3. Kies **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. Het bestand staat daarna in `app/build/outputs/apk/debug/app-debug.apk`.

Je kunt ook je telefoon met een USB-kabel aansluiten, USB-foutopsporing aanzetten en op de groene afspeelknop drukken. Dan installeert Android Studio de app direct.

## Installeren op je Galaxy A54

1. Zet `app-debug.apk` op je telefoon, bijvoorbeeld via Google Drive of door het naar jezelf te mailen.
2. Tik erop. Je telefoon vraagt of je installeren uit deze bron wilt toestaan. Sta dat toe voor die ene app, zoals Mijn bestanden of Drive.
3. Open **Rechtop** en sta meldingen toe.

## Eenmalig instellen

- **Batterij:** Instellingen → Apps → Rechtop → Batterij → **Onbeperkt**. Samsung zet apps anders in slaap en dan kunnen seintjes uitblijven.
- **Fit3 laten meetrillen:** Galaxy Wearable → Meldingen → zet Rechtop aan.
- **Tegel:** trek je snelle instellingen omlaag, tik op het potlood en sleep de tegel **Rechtop** erbij. Eén tik zet Even weg aan of uit.

## Hoe het werkt

- Seintjes komen op een willekeurig moment tussen je minimum en maximum. Android kan ze een paar minuten later afleveren om batterij te sparen.
- In de melding staan de knoppen **Zit goed** en **Even weg**.
- Een rustige, vaste melding laat zien hoe lang je al aaneen zit, of hoe lang je weg bent.
- Ben je minstens 2 minuten weg, dan telt dat als pauze en begint je zittijd opnieuw.
- Met vaste tijden aan gaat de app vanzelf aan en uit. Stop je hem zelf tijdens werktijd, dan blijft hij uit tot het einde van die werktijd.
