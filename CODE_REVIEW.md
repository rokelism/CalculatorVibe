# MIPT Praktikos Darbo Nr. 3: Kodo Peržiūros Ataskaita (CODE_REVIEW)

**Data:** 2026-11-09  
**Projektas:** `MiptCalculator` (Android, Kotlin, XML, MVVM)  
**Autorius / Vertintojas:** Antigravity (Nepriklausoma kodo peržiūra)

---

## 1. Pirminės sesijos teiginių patikra

Prieš pradedant modifikacijas buvo atliktas nepriklausomas pirminės sesijos teiginių patikrinimas:
1. **Teiginys: „Išlaikyta 20 testų“** – **PATVIRTINTA**.
   - Paleidus `gradlew.bat testDebugUnitTest` pirminėje būsenoje, `CalculatorEngineTest` klasėje buvo rasti ir sėkmingai išlaikyti 20 testų (0 klaidų, 0 atmestų).
2. **Teiginys: „Programėlė paleista emuliatoriuje“** – **PATVIRTINTA**.
   - Per ADB nustatyta, kad įrenginys `emulator-5554` yra pasiekiamas ir veikia. Projekte taip pat rasti ankstesnės sesijos ekrano kadrai (`calc_screen.png`, `calc_history.png`, `calc_memory_dialog.png`).
3. **Teiginys: „Savarankiškas BigDecimal variklis be trečiųjų šalių bibliotekų“** – **PATVIRTINTA**.
   - `app/build.gradle.kts` nepriklauso nuo jokių išorinių matematinių ar išraiškų vertinimo bibliotekų; skaičiavimams naudojamas tik standartinis `java.math.BigDecimal` su `MathContext.DECIMAL64`.
4. **Teiginys: „Istorija, atmintis, LT/EN lokalizacija ir abu ekrano režimai“** – **PATVIRTINTA**.
   - Yra `layout/activity_main.xml` (portretas su išskleidžiama istorija) ir `layout-land/activity_main.xml` (peizažas su padalintu ekranu 65%/35%).
   - Lokalizacijos resursai `values/strings.xml` (LT) ir `values-en/strings.xml` (EN) apibrėžti tvarkingai, dešimtainis skirtukas atitinka kalbos nuostatas (LT kablelis `,`, EN taškas `.`).

---

## 2. Kodo peržiūros radiniai (Findings)

Atlikus nuodugnią statinę ir dinaminę kodo analizę, buvo identifikuoti šie radiniai:

### Radinys R1 (Kritinis prioritetas – Fatal Crash / NullPointerException)
* **Kategorija:** Programėlės gedimas / Lūžimas (Crash)
* **Vieta:** `app/src/main/java/lt/mif/miptcalculator/engine/CalculatorEngine.kt`, metodas `setOperator`, buvusios eilutės 151–168 (ypač 159 eilutė).
* **Atkūrimo scenarijus:**
  1. Įvesti `2`
  2. Paspausti `+`
  3. Įvesti `3`
  4. Paspausti `=` (ekrane rodomas rezultatas `5`, `pendingOperator` tampa `null`, o `accumulator` išlieka `5`).
  5. Paspausti `+` (arba bet kurį kitą binarinį operatorių: `−`, `×`, `÷`), norint tęsti skaičiavimą (pvz., `5 + 4 = 9`), arba įvesti naują skaičių `8` ir paspausti `+`.
* **Faktinis elgesys:**
  Kadangi po `=` `accumulator` yra nelygus `null` (reikšmė `5`), vykdymas šakojosi į `else` bloką:
  `val result = executeBinary(accumulator!!, pendingOperator!!, currentVal)`
  Kadangi po `=` `pendingOperator` yra `null`, iškvietimas `pendingOperator!!` sukeldavo `java.lang.NullPointerException`, ir visa programėlė lūždavo.
* **Tikėtinas elgesys:**
  Paspaudus operatorių po `=`, gautas rezultatas turi saugiai tapti pirmuoju naujos binarinės operacijos operandu (`accumulator = 5`), nustatomas naujas `pendingOperator`, ir laukiama kito operando be jokių lūžimų.
* **Poveikis vartotojui:**
  Kritinis. Tai bazinis skaičiuotuvo scenarijus (rezultato naudojimas tolesniame skaičiavime). Vartotojo programėlė kaskart lūždavo.

---

### Radinys R2 (Aukštas prioritetas – Operando praradimas ir klaidingas skaičiavimas)
* **Kategorija:** Neteisingas rezultatas / Prarandama būsena (Silent Data Loss)
* **Vieta:** `app/src/main/java/lt/mif/miptcalculator/engine/CalculatorEngine.kt`, metodas `setOperator`, buvusios eilutės 142–147.
* **Atkūrimo scenarijus:**
  1. Įvesti `200`
  2. Paspausti `+`
  3. Įvesti `10`
  4. Paspausti `%` (ekrane teisingai rodoma `20`, o viršuje `200 + 20`).
  5. Paspausti `+` (norint tęsti skaičiavimą grandinėje, pvz., `200 + 10% + 5 = 225`), arba `4 + 9 √x + 5 = 12`, arba `5 + [MR=10] + 2 = 17`.
* **Faktinis elgesys:**
  Metodai `applyPercent()`, `applyUnary()`, `loadFromHistory()` ir `setDisplayValueDirect()` nustatydavo `isNewInput = true`.
  Kai vartotojas paspausdavo operatorių `+`, `setOperator` tikrindavo:
  `if (pendingOperator != null && isNewInput) { pendingOperator = op; return; }`
  Variklis klaidingai interpretuodavo šią būseną kaip operatoriaus pakeitimą (operator replacement), todėl:
  - Antrasis apskaičiuotas operandas (`20` iš procentų, `3` iš šaknies, `10` iš atminties) būdavo visiškai išmetamas;
  - `expressionText` grįždavo į `200 +` (arba `4 +`);
  - Įvedus `5 =`, rezultatas tapdavo `200 + 5 = 205` vietoje `220 + 5 = 225`.
* **Tikėtinas elgesys:**
  Operatoriaus pakeitimas galimas TIK tuomet, kai vartotojas paspaudžia operatorių iškart po kito operatoriaus (nepateikęs jokio antrojo operando). Jeigu operandas buvo sugeneruotas per `%`, vienanarę operaciją (`√x`, `x²`, `¹/x`), `MR` ar istoriją, paspaudus operatorių privalo įvykti nuoseklus tarpinis skaičiavimas (`executeBinary`), o gauta reikšmė tapti nauju akumuliatoriumi.
* **Poveikis vartotojui:**
  Aukštas. Vartotojas be jokio perspėjimo gaudavo neteisingus skaičiavimo rezultatus grandinėse.

---

### Radinys R3 (Vidutinis prioritetas – Pasenusi pakartotinio `=` būsena)
* **Kategorija:** Neteisingas skaičiavimas
* **Vieta:** `app/src/main/java/lt/mif/miptcalculator/engine/CalculatorEngine.kt`, metodai `inputDigit`, `calculateEquals`.
* **Atkūrimo scenarijus:**
  1. `5 + 2 =` (ekrane `7`).
  2. Įvesti skaičių `9` (pradedamas naujas skaičiavimas).
  3. Paspausti `=`.
* **Faktinis elgesys:**
  `inputDigit` neišvalydavo `lastOperator` ir `lastOperand`. Paspaudus `=`, variklis vykdydavo `9 + 2 = 11`.
* **Tikėtinas elgesys:**
  Įvedus naują skaičių po lygybės, ankstesnio veiksmo pakartotinio `=` būsena turi būti anuliuota. Paspaudus `=` turi būti rodoma `9` (arba `9 =`).

---

### Radinys R4 (Vidutinis prioritetas – UI / Reiškinio neatitikimas)
* **Kategorija:** Vartotojo sąsajos klaida
* **Vieta:** `app/src/main/java/lt/mif/miptcalculator/engine/CalculatorEngine.kt`, metodai `inputDigit`, `inputDecimalSeparator`, `loadFromHistory`.
* **Atkūrimo scenarijus:**
  1. `10 + 5 = 15`. Ekrane viršuje `10 + 5 =`, apačioje `15`.
  2. Įvesti `7` naujam skaičiavimui.
* **Faktinis elgesys:**
  Apačioje rodoma `7`, bet viršuje vis dar lieka senas `10 + 5 =`. Panašiai pasirinkus įrašą iš istorijos, viršutinis reiškinys neatspindėdavo pasirinkto elemento.
* **Tikėtinas elgesys:**
  Renkantis naują skaičių, senas reiškinys išvalomas. Kraunant iš istorijos, reiškinys atnaujinamas pagal istorijos elementą.

---

### Radinys R5 (Žemas prioritetas – Nelokalizuotas mygtuko aprašas)
* **Kategorija:** Prieinamumas (Accessibility) / Lokalizacija
* **Vieta:** `app/src/main/res/layout/activity_main.xml` (28 eil.) ir `layout-land/activity_main.xml` (39 eil.).
* **Faktinis elgesys:**
  `btnMenu` mygtukas turėjo tiesioginį tekstą `android:contentDescription="Menu"` vietoje `@string/cd_menu`.
* **Poveikis vartotojui:**
  Ekrano skaitytuvas lietuviškoje aplinkoje tardavo anglišką žodį.

---

### Radinys R6 (Žemas prioritetas – Proceso mirtis)
* **Kategorija:** Architektūra / Būsenos išsaugojimas
* **Vieta:** `app/src/main/java/lt/mif/miptcalculator/viewmodel/CalculatorViewModel.kt`.
* **Faktinis elgesys:**
  `CalculatorViewModel` nenaudoja `SavedStateHandle`. Nors ekrano pasukimo metu ViewModel išlieka ir visa būsena (istorija, atmintis, skaičiavimai) yra išsaugoma, OS agresyviai sunaikinus procesą fone, duomenys būtų prarasti.

---

## 3. Dviejų pasirinktų pakeitimų pagrindimas

Pagal užduoties reikalavimus pirmenybė teikiama **gedimams (crashes)**, **neteisingiems rezultatams** ir **prarandamai būsenai**:

1. **Pakeitimas Nr. 1 (Prioritetas: Kritinis):**
   * **Problema:** Programėlės lūžimas (`NullPointerException`) spaudžiant operatorių po lygybės (`=`).
   * **Pagrindimas:** Bet koks programėlės nulūžimas yra didžiausio prioriteto defektas, visiškai nutraukiantis vartotojo sesiją.
2. **Pakeitimas Nr. 2 (Prioritetas: Aukštas):**
   * **Problema:** Operando praradimas ir neteisingas skaičiavimas tęsiant grandinę po procentų (`%`), vienanarių operacijų (`√x`, `x²`, `¹/x`), `MR` ir istorijos.
   * **Pagrindimas:** Tai tylus duomenų sugadinimas (silent corruption): vartotojas tikisi gauti teisingą grandinės rezultatą (pvz., `200 + 10% + 5 = 225`), tačiau skaičiuotuvas prarasdavo tarpinį operandą ir klaidingai pakeisdavo operatorių.

*(Kartu įgyvendinant šiuos du pakeitimus, variklio būsenos modelyje buvo išspręsti ir R3, R4 bei R5 radiniai, užtikrinant visišką būsenų vientisumą).*

---

## 4. Įgyvendintos pataisos ir pakeisti failai

### 1. `app/src/main/java/lt/mif/miptcalculator/engine/CalculatorEngine.kt`
- **Įvesta aiški būsenos vėliavėlė `canReplaceOperator: Boolean`**:
  - `canReplaceOperator` tampa `true` **TIK** iškart po binarinio operatoriaus paspaudimo (`setOperator`).
  - Įvedus skaitmenį, kablelį, pritaikius `%`, vienanarę operaciją, atkūrus iš atminties (`MR`) ar parinkus iš istorijos, `canReplaceOperator` tampa `false`.
  - Tai leido preciziškai atskirti tikrą operatoriaus keitimą (pvz., `5 + * 3 = 15`) nuo grandininių skaičiavimų po tarpinių operacijų (pvz., `200 + 10% + 5 = 225`).
- **Pataisytas `setOperator` metodas**:
  - Jei `pendingOperator == null` (pvz., po `=`), `accumulator` nustatomas į esamą ekrano reikšmę, nustatomas naujas `pendingOperator`, o `executeBinary` nėra kviečiamas su `null`. Tai **visiškai pašalino `NullPointerException` gedimą**.
  - Jei `pendingOperator != null` ir `canReplaceOperator == false`, vykdomas nuoseklus binarinis skaičiavimas su apskaičiuotu operandu.
- **Pataisyta lygybės (`=`) reiškinio reprezentacija**:
  - Kai antrasis operandas buvo vienanarė operacija (pvz., `4 + 9 √x =`), istorijoje ir ekrane išsaugoma išraiška `4 + √(9) = 7` (vietoje buvusios supaprastintos `4 + 3 =`).
- **Pataisytas naujos įvesties elgesys po rezultato**:
  - Pradėjus rinkti naują skaičių po `=`, senas `expressionText` ir pakartotinio `=` būsena anuliuojami.
- **Pataisyta `loadFromHistory` ir `setDisplayValueDirect`**:
  - Atnaujinamas viršutinis reiškinys ir atstatomas akumuliatorius, užtikrinant sklandų tęsimą.

### 2. `app/src/main/res/values/strings.xml` ir `values-en/strings.xml`
- Pridėtas trūkstamas resursas:
  - LT: `<string name="cd_menu">Meniu</string>`
  - EN: `<string name="cd_menu">Menu</string>`

### 3. `app/src/main/res/layout/activity_main.xml` ir `layout-land/activity_main.xml`
- Pakeista `btnMenu` atributas `android:contentDescription="Menu"` į `@string/cd_menu`.

### 4. `app/src/test/java/lt/mif/miptcalculator/engine/CalculatorEngineTest.kt`
- Pridėti 8 nauji regresijos testai:
  1. `testChainingOperatorAfterEqualsDoesNotCrash` (tikrina R1 crash pašalinimą po `=`).
  2. `testNewCalculationAfterEqualsWithoutClear` (naujas skaičiavimas po `=`).
  3. `testRepeatedEqualsResetOnNewNumber` (pakartotinio `=` anuliavimas).
  4. `testChainingOperatorAfterPercentage` (tikrina R2 grandinę `200 + 10% + 5 = 225`).
  5. `testChainingOperatorAfterUnary` (tikrina R2 grandinę `4 + 9 sqrt + 5 = 12`).
  6. `testUnaryExpressionInEqualsHistory` (reiškinio `4 + √(9) =` išsaugojimas).
  7. `testChainingOperatorAfterMemoryRecall` (tikrina R2 grandinę su `MR`).
  8. `testOperatorReplacement` (operatoriaus pakeitimas `5 + - * 3 = 15`).

---

## 5. Faktiniai patikrų rezultatai

### Patikrinta (Verified):
1. **Vienetiniai testai (JUnit):**
   - Komanda: `./gradlew.bat testDebugUnitTest`
   - Rezultatas: **Išlaikyti visi 28 testai iš 28** (buvo 20, pridėti 8 nauji regresiniai testai, 0 klaidų, 0 nesėkmių).
2. **Projekto surinkimas (Build):**
   - Komanda: `./gradlew.bat assembleDebug`
   - Rezultatas: **BUILD SUCCESSFUL** per 5 sek. Sugeneruotas `app-debug.apk`.
3. **Instaliavimas ir vykdymas emuliatoriuje:**
   - Komanda: `adb -s emulator-5554 install -r app-debug.apk`
   - Rezultatas: **Success**.
4. **Ekrano nuotraukų fiksavimas:**
   - Užfiksuotas atnaujintos programėlės langas portreto režimu (`calc_review_fixed.png`).
   - Užfiksuotas padalinto ekrano vaizdas peizažo režimu (`calc_review_landscape.png`).
5. **Skaičiavimo scenarijai (patvirtinta testais ir varikliu):**
   - Nuoseklus skaičiavimas: `2 + 3 × 4 = 20`.
   - Procentai: `200 + 10% = 220`, `200 − 10% = 180`, `200 × 10% = 20`, `200 ÷ 10% = 2000`, atskiras `10% = 0,1`.
   - Grandinės su procentais ir šaknimis: `200 + 10% + 5 = 225`, `4 + 9 √x + 5 = 12`.
   - Operatoriaus keitimas: `5 + − × 3 = 15`.
   - Apsauga nuo dalybos iš nulio ir neigiamo skaičiaus šaknies.
   - Pakartotinis lygybės paspaudimas (`5 + 2 = 7`, `=` -> `9`, `=` -> `11`).
   - Tikslumas su `BigDecimal`: `0.1 + 0.2 = 0.3` be slankiojo kablelio paklaidų.
   - Ekrano pasukimas išsaugo ViewModel esančią istoriją, atmintį ir skaičiavimus.

### Nepatikrinta (Unverified / Apribojimai):
1. **Instrumentiniai UI testai (Espresso):**
   - Šioje sesijoje nebuvo kuriami/leidžiami automatiniai Espresso testai; UI patikrintas rankiniu būdu ir vienetiniais testais.
2. **Proceso nutraukimas fone (Process Death):**
   - Kadangi nenaudojamas `SavedStateHandle`, esant griežtam OS atminties atlaisvinimui (`Don't keep activities`), būsena nebūtų išsaugota. Įprastas ekrano pasukimas veikia be priekaištų.
