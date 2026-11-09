# MIPT praktikos darbas Nr. 3 – AI sesijų žurnalas

**Projektas:** MiptCalculator, vietinė Android skaičiuotuvo programėlė.
**GitHub saugykla:** [rokelism/CalculatorVibe](https://github.com/rokelism/CalculatorVibe), vartotojo sukurta ir naršyklėje patikrinta.
**Projekto aprašymas:** [README.md](README.md); failai bus atskiros saugyklos šaknyje.

Žurnalas sudarytas iš keturių tikrų Antigravity pokalbių išrašų. Vartotojo tekstai pateikti pilni, be dirbtinių trumpinimų; pokalbio techniniai metaduomenys nepridedami. Laikai pateikti Lietuvos laiku (Europe/Vilnius, šiomis datomis UTC+02:00). Modelių pavadinimai perkelti iš ankstesnio Antigravity žurnalo, nes pačiuose naudotuose teksto išrašuose modelio laukas nenurodytas. Ketvirto etapo tęsinys Codex aplinkoje pažymėtas atskirai.

## Sesija 1 – Programėlės kūrimas ir /grill-me

- Įrankis: Google Antigravity.
- Pokalbio ID: `2acee613-88c6-436b-b9ea-648f4c1efd00`.
- Modelis pagal ankstesnį žurnalą: Gemini 3.8 Flash (High).
- Pirmasis įrašas: 2026-11-09 10:49:29 +0200.
- Paskutinis išrašo įrašas: 2026-11-09 11:17:34 +0200.


### Atlikti darbai ir rezultatai

- Sugeneruotas Kotlin/XML Android projektas su savarankišku `CalculatorEngine`, `BigDecimal`, MVVM, LT/EN resursais, portreto ir peizažo maketais.
- Realizuotos bazinės ir vienanarės operacijos, kontekstiniai procentai, pakartotinis `=`, istorija ir atmintis.
- Sukurti pradiniai 20 vienetinių testų. Pagal sesijos ataskaitą visi išlaikyti, APK surinktas ir programėlė paleista emuliatoriuje.
- Po vartotojo pranešimo apie užsidarymą paleidimo metu pataisyta adapterio inicializavimo tvarka ir mygtukų aukštis. Paskutinė vartotojo žinutė: „viskas veikia“.
- Pagrindiniai failai: `app/src/main/java/lt/mif/miptcalculator/engine/`, `viewmodel/`, `ui/`, `app/src/main/res/`, `app/src/test/`, Gradle konfigūracija ir wrapper.
- Apribojimai: istorija ir būsena saugomos veikimo metu; proceso nutraukimui atkurti `SavedStateHandle` nenaudojamas; automatinių Espresso UI testų nėra.


### AI /grill-me atsakymas: planas ir tikslinimo klausimai

Puikiai, susipažinau su užduoties aprašymu ir abiem PDF pateiktais Windows skaičiuotuvo pavyzdžiais. 

Pagal susitarimą šiame etape **kodo dar negeneruoju**, pateikiu reikalavimų apibendrinimą, architektūrinį įgyvendinimo planą ir 5 tikslinius klausimus neaiškioms vietoms suderinti.

---

### 1. Reikalavimų santrauka

* **Programėlės pobūdis ir architektūra:**
  * Gryna vietinė Android programėlė (Kotlin, XML Layouts, MVVM šablonas su `ViewModel`).
  * Griežtai atskirtas skaičiavimo variklis (`CalculatorEngine`), parašytas nuo nulio (be 3rd party parserių, `eval` ar JavaScript) su grynaisiais JUnit testais.
  * Pilnas atsparumas ekrano pasukimui (būsena ir istorija išlieka gyvos).
* **Vartotojo sąsaja (UI) pagal PDF pavyzdį:**
  * Windows standartinio skaičiuotuvo stilius: šviesiai pilkas fonas, viršutinė veiksmo/išraiškos eilutė, didelis pagrindinis rezultatas dešinėje, 4 stulpelių mygtukų tinklelis, akcentuotas mėlynas `=` mygtukas, istorijos ikona viršutinėje juostoje.
  * Atminties eilutė (`MC`, `MR`, `M+`, `M-`, `MS`).
* **Aritmetinė logika:**
  * Nuoseklus vykdymas (*immediate execution* / akumuliatoriaus modelis, kaip Windows Standard režime): `2 + 3 × 4 = 20`.
  * Operacijos: sudėtis, atimtis, daugyba, dalyba, $\sqrt{x}$, $x^2$, $1/x$, $\%$, ženklo keitimas ($\pm$).
  * Pagalbiniai: `CE` (valo dabartinį įvedimą), `C` (valo visą skaičiavimą, bet ne istoriją), trynimas $\backspace$ (atsparus tuščiam laukui / likusiam minusui).
  * Kontekstinis procentas: `200 + 10 % = 220`, `200 × 10 % = 20`, atskiras `10 % = 0,1`.
  * Tikslus formatavimas (naudojant `BigDecimal` / kontroliuojamą apvalinimą, išvengiant `0,30000000000000004`).
  * Klaidų valdymas: dalyba iš 0, neigiama šaknis rodo lokalizuotą klaidą, neužlaužia programos, neįrašo klaidos į istoriją.
* **Istorija:**
  * Saugoma atmintyje (runtime), naujausi įrašai viršuje.
  * Rodo išraišką ir rezultatą. Įrašą paspaudus, jo rezultatas įkeliamas į skaičiuotuvą. Yra mygtukas istorijai išvalyti.
* **Lokalizacija:**
  * Lietuvių kalba (pagrindinė / numatytoji) ir pilnas anglų kalbos vertimas (`values/strings.xml` ir `values-en/strings.xml`).

---

### 2. Įgyvendinimo planas

1. **Projekto struktūra ir Gradle konfigūracija:**
   * Pilnas Android Studio projektas su `gradlew` wrapperiu, suderinamomis AndroidX priklausomybėmis, `minSdk 24`, `targetSdk 34/35`, Java 17 / Kotlin suderinamumu.
2. **Skaičiavimo variklis (`CalculatorEngine`):**
   * Atskiras Kotlin modulis be Android SDK priklausomybių.
   * `BigDecimal` pagrįsta akumuliatoriaus būsenų mašina: dvinariai veiksmai, vienanariai veiksmai, procentai, formatavimas, saugus klaidų gaudymas.
3. **Resursai ir stiliai:**
   * Spalvų paletė, mygtukų stiliai (su paspaudimo animacija/ripple), vektorinės piktogramos (istorija, trynimas $\backspace$, šiukšliadėžė).
   * Pilnas tekstų ir klaidų iškėlimas į `strings.xml` (LT ir EN).
4. **Vartotojo sąsajos maketas (XML):**
   * Pagrindinis skaičiuotuvo ekranas (`ConstraintLayout` + mygtukų tinklelis).
   * Integruotas istorijos skydelis (pritaikytas telefonui – patogiai išskleidžiamas portrait režime ir greta landscape režime).
5. **ViewModel ir būsenos valdymas:**
   * `CalculatorViewModel` suriša UI su `CalculatorEngine`, saugo istoriją, atmintį (`MemoryRegister`) ir išlaiko būseną ekrano pasukimo metu.
6. **Automatiniai vienetų testai (Unit Tests):**
   * `CalculatorEngineTest` su scenarijais: baziniai veiksmai, `2+3*4=20`, procentų taisyklės, `1/x`, `CE` vs `C`, dalyba iš nulio, neigiama šaknis, pasikartojantys veiksmai, slankiojo kablelio tikslumas.
7. **Patikra ir ataskaita:**
   * Projekto kompiliavimas ir testų vykdymas aplinkoje, aiškios instrukcijos paleidimui Android Studio.

---

### 3. Tiksliniai klausimai (reikalavimų suderinimas)

1. **Istorijos skydelio elgsena vertikaliame (portrait) ekrane:**
   PDF 2 puslapyje istorija matoma kaip skydelis. Kadangi telefone ekranas siauras, ar tinka realizacija:
   * **Portrait:** paspaudus istorijos piktogramą viršuje, atsidaro puslapis / uždengiantis skydelis (overlay / slide panel) su istorija ir šiukšliadėžės mygtuku;
   * **Landscape:** istorija rodoma dešinėje pusėje greta klaviatūros, kaip Windows lange?
2. **Atminties (MC, MR, M+, M-, MS) elgsena:**
   * Ar taikome Windows elgseną, kai esant tuščiai atminčiai `MC` ir `MR` mygtukai yra vizualiai neaktyvūs (*disabled* / pilkesni)?
   * Ar pakanka vieno atminties registro (saugo vieną einamąją reikšmę su pridėjimu/atėmimu), be atskiro kelių atminties reikšmių sąrašo lango (`M˅`)?
3. **Pakartotinis lygybės (`=`) paspaudimas:**
   Tikrame Windows skaičiuotuve pakartotinis `=` pakartoja paskutinį veiksmą su paskutiniu operandu (pvz., įvedus `5 + 2 =` gaunama `7`, dar kartą paspaudus `=` gaunama `9`, vėl paspaudus `=` – `11`). 
   * Ar įgyvendiname būtent šią elgseną, ar pageidaujate, kad pakartotinis `=` tiesiog nekeistų rezultato?
4. **Dešimtainis skirtukas:**
   Lietuviškoje lokalėje standartinis skirtukas yra kablelis (`,`), angliškoje – taškas (`.`).
   * Ar mygtuko užrašas ir skaičių atvaizdavimas turi dinamiškai prisitaikyti pagal pasirinktą kalbą (LT rodo `,`, EN rodo `.`), ar pageidaujate fiksuoto kablelio/taško?
5. **Vienanarių operacijų rodymas veiksmo eilutėje:**
   Pritaikius vienanarę funkciją (pvz., įvedus `9` ir paspaudus `√`), ar veiksmo eilutėje viršuje rodoma išraiška `√(9)` (kaip Windows skaičiuotuve), o pagrindiniame ekrane – `3`?

Laukiu jūsų atsakymų ir iškart pradėsiu pilną projekto generavimą.


### Vartotojo užklausa (2026-11-09 10:49:29 +0200, žingsnis 0)

````text
/grill-me

Padėk atlikti MIPT praktikos darbą Nr. 3: AI sugeneruoti veikiančią vietinę Android skaičiuotuvo programėlę. Pridedu užduoties PDF su dviem išvaizdos pavyzdžiais. Pirmiausia perskaityk tekstą ir apžiūrėk abu paveikslėlius.

Šis pirmas etapas yra reikalavimų tikslinimas ir planavimas. Dar negeneruok kodo. Trumpai apibendrink reikalavimus, pateik įgyvendinimo planą ir užduok iki 5 tikslinių klausimų apie tikras neaiškias vietas. Neklausk iš naujo dėl jau sutartų sprendimų. Jei /grill-me nėra tavo įrankio komanda, atlik čia aprašytą klausimų ir planavimo etapą įprastu pokalbiu.

Jau sutarta:
- Kuriame native Android programėlę, ne interneto svetainę ar WebView demonstraciją.
- Sąsaja lietuviška, pridėtas pilnas anglų kalbos vertimas. Kalba parenkama pagal Android sistemos kalbą, kitoms kalboms naudojama lietuvių kalba.
- Išvaizda turi atkartoti PDF pavaizduotą Windows standartinio skaičiuotuvo struktūrą: šviesiai pilkas fonas, rezultatas dešinėje, mažesnė veiksmo eilutė virš jo, keturi mygtukų stulpeliai, mėlynas lygybės mygtukas, istorijos mygtukas viršuje. Pritaikyk telefono ekranui, nekurk fiktyvaus Windows lango rėmelio.
- „Trupmenos skaičiavimas“ šiame darbe reiškia atvirkštinio skaičiaus funkciją 1/x, kaip PDF pavyzdyje.
- Techninis numatytasis pasirinkimas: Kotlin, XML išdėstymas ir atskiras nuo Android sąsajos skaičiavimo modulis. Jei aplinka pagrįstai reikalauja kito native Android sprendimo, paaiškink prieš generuodamas.

Programėlės reikalavimai:
1. Sudėtis, atimtis, daugyba, dalyba, kvadratinė šaknis, kėlimas kvadratu, procentai ir 1/x.
2. Mygtukai: %, CE, C, trinti paskutinį ženklą; 1/x, x², √x, ÷; 7,8,9,×; 4,5,6,−; 1,2,3,+; ±,0,dešimtainis skirtukas,=. Išlaikyk PDF mygtukų eiliškumą.
3. CE išvalo dabartinę įvestį, C išvalo visą dabartinį skaičiavimą. C savaime neištrina istorijos. Trinant paskutinį simbolį rezultatas nelieka tuščias ar vien minusas. Ženklo keitimas veikia ir įvedant, ir gavus rezultatą.
4. Procentai veikia kaip standartiniame skaičiuotuve: 200 + 10 % = 220; 200 − 10 % = 180; 200 × 10 % = 20; 200 ÷ 10 % = 2000; atskirai įvestas 10 % = 0,1.
5. Dvinarės operacijos vykdomos nuosekliai kaip standartiniame skaičiuotuve: 2 + 3 × 4 = 20. Aiškiai apibrėžk elgesį po lygybės, pakeitus operatorių, pakartotinai paspaudus lygybę ir po vienanarės funkcijos. Mygtukai neturi sukelti atsitiktinių senos operacijos pakartojimų.
6. Istorija saugoma programėlės veikimo metu. Įrašas turi išraišką ir rezultatą; įtraukiamos ir sėkmingos vienanarės operacijos. Naujausi įrašai viršuje. Istorijos skydelį galima atidaryti, uždaryti ir išvalyti; pasirinkus įrašą galima panaudoti jo rezultatą. Pradinė istorija tuščia. Pasukus ekraną išsaugok dabartinę įvestį ir istoriją, kol programėlės procesas gyvas. Ilgalaikis išsaugojimas po proceso uždarymo neprivalomas.
7. PDF matomus atminties valdiklius MC, MR, M+, M−, MS ir atminties peržiūrą įgyvendink prasmingai, jei jie įtraukti į sąsają; nepalik neveikiančių dekoratyvinių mygtukų. Atminties elgesį trumpai aprašyk baigiamoje suvestinėje.
8. Skaičiavimo modulis sukurtas nuo nulio. Nenaudok trečiųjų šalių išraiškų skaičiuoklių, eval, JavaScript variklio ar išorinio skaičiavimo API. Standartinės kalbos ir platformos matematikos priemonės leidžiamos. Skaičiavimams nereikia interneto.
9. Visi vartotojui matomi tekstai, klaidos, mygtukų pavadinimai ir prieinamumo aprašai yra Android string resursuose su pilnais LT ir EN rinkiniais. Atsižvelk į dešimtainį skirtuką ir venk vartotojui rodomų tekstų įrašymo tiesiai į Kotlin kodą.
10. Dalyba iš nulio, 1/0, neigiamo skaičiaus šaknis, neleistinas ar per didelis rezultatas turi pateikti aiškią lokalizuotą klaidą, o ne nulaužti programėlę ar rodyti NaN/Infinity. Po klaidos galima pradėti naują skaičiavimą. Neįrašyk klaidingų rezultatų į istoriją.
11. Rezultatai tvarkingai formatuojami, be akivaizdžių slankiojo kablelio artefaktų, pvz., 0,1 + 0,2 turi būti rodoma kaip 0,3. Ilgi skaičiai ir padidintas sistemos šriftas neturi uždengti valdiklių. Mygtukai turi būti patogūs paspausti ir turėti prieinamumo aprašus.

Darbo taisyklės:
- Visą kodą ir vėlesnius taisymus generuoji tu. Neprašyk manęs rankiniu būdu taisyti kodo Android Studio.
- Naudok patikrintas, tarpusavyje suderinamas Android įrankių versijas pagal realią darbo aplinką. Pateik pilną importuojamą projektą su Gradle konfigūracija ir wrapper failais, ne tik kodo fragmentus.
- Jei negali tiesiogiai rašyti projekto failų, pateik atsisiunčiamą projektą. Jei ir to negali, aiškiai nurodyk ribojimą prieš žadėdamas sukurtą projektą.
- Prieš generavimą atsakyk tik reikalavimų santrauka, planu ir klausimais. Po mano atsakymo tame pačiame pokalbyje sugeneruok visą projektą, pridėk prasmingus skaičiavimo modulio testus, patikrink tai, ką leidžia aplinka, ir pataisyk nustatytas kūrimo klaidas.
- Baigęs pateik projekto vietą arba atsisiuntimą, paleidimo Android Studio žingsnius, atliktų patikrų rezultatus ir aiškiai atskirk nepatikrintus dalykus. Jei neturi Android aplinkos, nesakyk, kad build ar paleidimas sėkmingas.
- Šioje sesijoje neatlik atskiros code review užduoties, nekurk galutinio README ir nevykdyk Git commit/push. Šie darbai bus kitose AI sesijose.
```
````

### Vartotojo užklausa (2026-11-09 10:53:18 +0200, žingsnis 8)

````text
Patvirtinu reikalavimų santrauką ir planą. Atsakymai į klausimus:

Istorija: tinka uždengiantis skydelis vertikaliame ekrane ir istorija dešinėje horizontaliame ekrane. Vertikalų skydelį turi būti lengva uždaryti ir grįžti prie skaičiavimo. Pasukus ekraną dabartinis skaičiavimas ir istorija turi išlikti.

Atmintis: kai atmintis tuščia, MC ir MR turi būti neaktyvūs. Pakanka vieno atminties registro su MC, MR, M+, M− ir MS. Jeigu pagal PDF rodysi M˅ mygtuką, jis turi atverti dabartinės atminties reikšmės peržiūrą, o ne likti dekoratyvinis.

Pakartotinis =: įgyvendink paskutinės operacijos kartojimą: 5 + 2 = → 7, dar kartą = → 9, dar kartą = → 11. Pradėjus naują skaičiavimą ar paspaudus C, ankstesnė kartojimo būsena turi būti išvalyta.

Dešimtainis skirtukas: pritaikyk pagal kalbą. Lietuviškai mygtukas ir skaičiai rodomi su kableliu, angliškai – su tašku. Skirtuko formatavimas neturi paveikti skaičiavimo tikslumo.

Vienanarės operacijos: taip, viršuje rodyk išraišką, pavyzdžiui, √(9), o pagrindiniame lauke rezultatą 3. Analogiškai atvaizduok kvadratą ir 1/x. Sėkmingas operacijas įtrauk į istoriją.

Papildomai patikrink visas procentų taisykles: 200 + 10 % = 220; 200 − 10 % = 180; 200 × 10 % = 20; 200 ÷ 10 % = 2000; atskiras 10 % = 0,1.

Dabar pradėk generuoti visą Android projektą pagal patvirtintą planą. Visus projekto failus ir pataisas atlik pats. Pateik pilną importuojamą projektą, įskaitant Gradle wrapper failus, o ne vien kodo fragmentus.

Pridėk skaičiavimo modulio testus. Jei aplinka leidžia, paleisk testus ir surink projektą; pateik tikrus rezultatus ir aiškiai pažymėk nepatikrintus dalykus.

Šioje sesijoje dar neatlik atskiros kodo peržiūros, nekurk galutinio README ir nevykdyk Git commit/push – tai bus atliekama atskirose AI sesijose.
````

### Vartotojo užklausa (2026-11-09 11:11:46 +0200, žingsnis 199)

````text
paspaudus run app jis atsidaro gal 1 sekunde ir uzsidaro
````

### Vartotojo užklausa (2026-11-09 11:17:34 +0200, žingsnis 260)

````text
viskas veikia
````

## Sesija 2 – Nepriklausoma kodo peržiūra ir dvi pataisos

- Įrankis: Google Antigravity.
- Pokalbio ID: `b75ff1f2-57e8-4761-8215-e763be41f786`.
- Modelis pagal ankstesnį žurnalą: Gemini 3.8 Flash (High).
- Pirmasis įrašas: 2026-11-09 11:20:38 +0200.
- Paskutinis išrašo įrašas: 2026-11-09 11:35:36 +0200.


### Atlikti darbai ir rezultatai

- Atlikta nepriklausoma peržiūra ir sukurtas [CODE_REVIEW.md](CODE_REVIEW.md).
- **R1, kritinis:** pataisytas `NullPointerException` spaudžiant operatorių po `=`. Naujas veiksmas pradedamas saugiai su gautu rezultatu, nekviečiant skaičiavimo su `pendingOperator == null`.
- **R2, aukštas:** pataisytas antrojo operando praradimas po procentų, vienanarių operacijų ir atminties reikšmės įkėlimo. Įvesta `canReplaceOperator`, atskirianti operatoriaus keitimą nuo grandininio skaičiavimo. Patikros: `200 + 10% + 5 = 225`, `4 + √(9) + 5 = 12`.
- Papildomai atnaujinta pakartotinio `=` būsena, reiškinio eilutė ir lokalizuotas meniu prieinamumo aprašas.
- Pakeisti `CalculatorEngine.kt`, `CalculatorEngineTest.kt`, LT/EN `strings.xml`, abu `activity_main.xml` maketai. Pridėti 8 regresiniai testai ir du peržiūros ekrano kadrai.
- Sesijos ataskaita nurodo 28/28 išlaikytus testus, sėkmingą `assembleDebug`, diegimą ir paleidimą emuliatoriuje. GitHub paruošimo metu papildomai perskaitytas išlikęs JUnit XML: **28 testai, 0 nesėkmių, 0 klaidų, 0 praleistų**, ataskaitos laikas `2026-11-09T09:33:44.179Z`. Testai šiame dokumentacijos etape nebuvo iš naujo vykdomi.
- Likęs apribojimas: būsena neatkuriama po proceso nutraukimo; nėra automatinių Espresso UI testų.


### Vartotojo užklausa (2026-11-09 11:20:38 +0200, žingsnis 0)

````text
Tai atskira MIPT praktikos darbo Nr. 3 kodo peržiūros sesija. Atlik nepriklausomą projekto peržiūrą ir įgyvendink du aukščiausio prioriteto pakeitimus.

Projekto aplankas:
C:/Users/Rokas/.gemini/antigravity/scratch/MiptCalculator

Pirmoje sesijoje sukurta Kotlin ir XML Android skaičiuotuvo programėlė su savarankišku BigDecimal skaičiavimo varikliu, istorija, atmintimi ir LT/EN lokalizacija. Ankstesnės sesijos suvestinėje nurodyta, kad išlaikyta 20 testų ir programėlė paleista emuliatoriuje. Šiuos teiginius patikrink pagal tikrus failus ir prieinamus patikrų rezultatus.

1. Peržiūrėk kodą prieš jį keisdamas.

Patikrink:

Skaičiavimų ir būsenų teisingumą, ypač veiksmų grandines, operatoriaus pakeitimą, pakartotinį = ir vienanares operacijas.

Procentus: 200+10%=220, 200−10%=180, 200×10%=20, 200÷10%=2000, atskiras 10%=0,1.

Nuoseklų skaičiavimą: 2+3×4=20.

CE, C, trynimą, ženklo keitimą ir naujo skaičiavimo pradėjimą po rezultato ar klaidos.

Dalybą iš nulio, neigiamo skaičiaus šaknį, skaičiavimo tikslumą ir dideles reikšmes.

Istoriją, atminties mygtukus ir būsenos išlikimą pasukus ekraną.

LT/EN vertimus, dešimtainį skirtuką, prieinamumą ir išdėstymą abiem ekrano kryptimis.

Ar skaičiavimo modulis nepriklauso nuo trečiųjų šalių skaičiavimo bibliotekų.

Ar esami testai tikrina svarbius vartotojo scenarijus.

2. Užfiksuok radinius ir pasirink du svarbiausius pakeitimus.

Kiekvienam radiniui nurodyk prioritetą, failą bei kodo vietą, konkretų atkūrimo scenarijų, tikėtiną ir faktinį elgesį, poveikį vartotojui. Pirmenybę teik gedimams, neteisingiems rezultatams ir prarandamai būsenai.

Neišgalvok problemų. Jeigu defektų mažiau nei du, likusį pakeitimą pagrįsk kaip konkretų patobulinimą ir taip jį įvardyk.

3. Pats įgyvendink abu pasirinktus pakeitimus.

Išsaugok pradinį radinių aprašymą. Pridėk prasmingas regresijos patikras, paleisk testus ir surink projektą, jei aplinka leidžia. Neprašyk manęs rankiniu būdu redaguoti kodo.

4. Sukurk CODE_REVIEW.md projekto šaknyje.

Įrašyk peržiūros radinius, dviejų pakeitimų pasirinkimo pagrindimą, įgyvendintas pataisas, pakeistus failus, faktinius patikrų rezultatus ir likusius apribojimus. Aiškiai atskirk patikrintus ir nepatikrintus dalykus.

Baigęs pateik trumpą dviejų pakeitimų suvestinę ir patikrų rezultatus. Šioje sesijoje nekurk galutinio README ir nevykdyk Git commit/push – tam skirtos kitos sesijos.
````

## Sesija 3 – Vartotojo dokumentacija

- Įrankis: Google Antigravity.
- Pokalbio ID: `a43f890f-cf69-4dbd-9057-6d9769264c02`.
- Modelis pagal ankstesnį žurnalą: Gemini 3.8 Flash (High).
- Pirmasis įrašas: 2026-11-09 11:40:28 +0200.
- Paskutinis išrašo įrašas: 2026-11-09 11:46:41 +0200.


### Atlikti darbai ir rezultatai

- Perskaitytas projektas ir peržiūros ataskaita. Sukurtas [README.md](README.md) su vartotojo instrukcijomis, faktine konfigūracija, skaičiavimo pavyzdžiais, istorijos, atminties, kalbų ir klaidų valdymo aprašais.
- Panaudotos keturios tikros nuotraukos: `calc_review_fixed.png`, `calc_review_landscape.png`, `calc_history.png`, `calc_memory_dialog.png`.
- Dokumentuoti antros sesijos patikrų rezultatai ir apribojimai; programėlės kodas šiame etape nekeistas.
- **Išrašo pastaba:** šio pokalbio pabaigoje vartotojas taip pat įklijavo ketvirto etapo GitHub užklausą. Po jos šiame išraše veiksmų neužfiksuota. Ta pati užduotis paskui pradėta atskirame ketvirtos sesijos pokalbyje. Žemiau palikta ir ši faktinė vartotojo žinutė.


### Vartotojo užklausa (2026-11-09 11:40:28 +0200, žingsnis 0)

````text
Tai atskira MIPT praktikos darbo Nr. 3 vartotojo dokumentacijos kūrimo sesija.

Projekto aplankas:
C:/Users/Rokas/.gemini/antigravity/scratch/MiptCalculator

Perskaityk dabartinį projektą ir jo šaknyje esantį CODE_REVIEW.md. Sukurk galutinį lietuvišką README.md projekto šaknyje, kad įkėlus projektą GitHub jį rodytų kaip pagrindinį aprašymą.

Dokumentuok realų programėlės veikimą, o ne vien ankstesnės sesijos suvestinę.

README turi apimti:

1. **Pavadinimą ir paskirtį:** MIPT praktikos darbo Nr. 3 vietinė Android skaičiuotuvo programėlė, sukurta naudojant AI.

2. **Paleidimą:** pagal tikrą projekto konfigūraciją nurodyk reikalingas priemones ir aiškius atidarymo, sinchronizavimo, surinkimo bei paleidimo per Android Studio žingsnius.

3. **Naudojimą:** paaiškink aritmetines operacijas, √x, x², 1/x, procentus, ±, CE, C ir paskutinio ženklo trynimą. Aiškiai atskirk CE nuo C. Aprašyk nuoseklų operacijų vykdymą ir pakartotinį =.

4. **Pavyzdžius:** įtrauk bazinius skaičiavimus, procentus ir veiksmų grandines. Rezultatus patikrink pagal įgyvendintą logiką bei testus.

5. **Istoriją ir atmintį:** kaip atidaryti, panaudoti ir išvalyti istoriją, kiek laiko ji išlieka, kaip veikia MC, MR, M+, M−, MS ir M˅.

6. **Kalbas ir ekraną:** LT/EN kalbos parinkimą, dešimtainį skirtuką, vertikalų ir horizontalų išdėstymą, būsenos išlikimą pasukus ekraną.

7. **Klaidų valdymą:** dalybą iš nulio, neigiamo skaičiaus šaknį ir kaip tęsti darbą po klaidos.

8. **Trumpą techninę informaciją:** savarankišką CalculatorEngine, BigDecimal naudojimą, pagrindinių projekto failų paskirtį ir vertimų resursų vietą.

9. **Patikras ir apribojimus:** remkis tikrais testais bei CODE_REVIEW.md. Peržiūros suvestinėje nurodyti 28 sėkmingi testai, sėkmingas surinkimas ir patikra emuliatoriuje; dokumentacijoje aiškiai priskirk šiuos rezultatus atliktai kodo peržiūrai, jei pats jų nekartojai.

10. **AI darbo eigą:** atskiros kūrimo, peržiūros, dokumentacijos ir GitHub sesijos. Pridėk nuorodą į CODE_REVIEW.md. Nuorodą į sesijų žurnalą pridėk tik jei failas jau egzistuoja; kitu atveju pažymėk, kad jis bus pridėtas GitHub etape.

Jei projekte yra tinkamų tikrų ekrano nuotraukų, gali jas įtraukti naudodamas teisingus santykinius kelius. Nekurk netikrų paveikslėlių, neveikiančių nuorodų, GitHub adresų ar neegzistuojančių patikrų rezultatų.

Šioje sesijoje nekeisk programėlės kodo ir nevykdyk Git commit/push. Sukurk README.md pats, o baigęs pateik jo vietą ir trumpą dokumentacijos turinio suvestinę.
````

### Vartotojo užklausa (2026-11-09 11:46:41 +0200, žingsnis 58)

````text
Tai atskira MIPT praktikos darbo Nr. 3 GitHub publikavimo ir sesijų žurnalo parengimo sesija.

Projekto aplankas:
C:/Users/Rokas/.gemini/antigravity/scratch/MiptCalculator

Ankstesnėse atskirose AI sesijose:
1. Sugeneruota Android skaičiuotuvo programėlė.
2. Atlikta kodo peržiūra, įgyvendinti du aukščiausio prioriteto pataisymai ir sukurtas CODE_REVIEW.md.
3. Sukurtas lietuviškas README.md su tikromis ekrano nuotraukomis.

Šioje sesijoje atlik šiuos darbus:

**1. Patikrink galutinio projekto failus.**

Perskaityk README.md ir CODE_REVIEW.md. Patikrink, kad projekte yra šaltiniai, resursai, Gradle konfigūracija, wrapper failai, testai ir visos README naudojamos ekrano nuotraukos. Pataisyk neveikiančius dokumentacijos kelius, jei tokių rasi. Programėlės kodo nekeisk.

**2. Sukurk SESSION_LOG.md projekto šaknyje.**

Naudok būtent šį pavadinimą, nes jis jau nurodytas README.

Žurnale kiekvienai iš keturių sesijų pateik:
- Paskirtį, tikrą datą, įrankį ir modelį, jei žinomas.
- Visą faktiškai siųstą vartotojo užklausą ir papildomus vartotojo nurodymus.
- Atliktus darbus, sukurtus ar pakeistus failus.
- Patikrų rezultatus ir likusius apribojimus.

Pirmoje sesijoje užfiksuok /grill-me klausimus bei vartotojo atsakymus. Antroje – abu konkrečius pataisymus ir patikrų rezultatus. Trečioje – README sukūrimą. Ketvirtoje – šią užklausą ir tikrus GitHub veiksmus.

Remkis pateiktais pokalbių išrašais ir tikrais failais. Neišgalvok užklausų, datų, modelių ar atliktų veiksmų. Jei ankstesnių sesijų duomenų nematai, paprašyk trūkstamų išrašų. Vien AI atsakymų suvestinės nepakeičia vartotojo užklausų.

README pridėk veikiančią santykinę nuorodą į SESSION_LOG.md.

**3. Paruošk Git saugyklą ir atlik commit/push.**

Patikrink esamą Git būseną, šaką ir remote. Jei Git saugyklos nėra, inicijuok ją. Jei nėra GitHub paskirties, paprašyk mano saugyklos adreso arba naujos saugyklos pavadinimo ir viešumo pasirinkimo. Neišgalvok adreso ar Git autoriaus tapatybės.

Patikrink .gitignore. Neįkelk local.properties, build aplankų, vietinių .gradle duomenų, prieigos raktų, slaptažodžių ar pasirašymo raktų. Įtrauk reikalingus projekto šaltinius, Gradle wrapper, dokumentaciją, sesijų žurnalą ir naudojamas ekrano nuotraukas.

Neatšauk vartotojo pakeitimų, neperrašyk nuotolinės istorijos ir nenaudok force push. Sukurk aiškų commit ir atlik push. Jei reikia prisijungimo, nurodyk konkretų mano veiksmą.

**4. Patikrink publikavimą ir užbaik žurnalą.**

Patikrink, kad commit pasiekė GitHub ir README rodomas su veikiančiomis nuorodomis bei nuotraukomis, jei turi prieigą tai patikrinti.

Po sėkmingo pirmojo push papildyk SESSION_LOG.md tikra GitHub nuoroda, įkelto programėlės commit identifikatoriumi ir faktiniais rezultatais. Žurnalo papildymą išsaugok atskiru dokumentacijos commit ir taip pat atlik push.

Galutinio žurnalo commit identifikatorių pateik atsakyme – nereikia jo įrašyti į patį žurnalą.

Baigęs pateik:
- Tikrą GitHub projekto nuorodą.
- Galutinio commit identifikatorių ir push rezultatą.
- Galutinio SESSION_LOG.md vietą, kad galėčiau jį pateikti į Moodle.
- Aiškiai įvardytus likusius darbus, jei kas nors nepavyko.

Neskelbk neįvykusių veiksmų sėkmingais.
````

## Sesija 4 – GitHub paruošimas Antigravity aplinkoje

- Įrankis: Google Antigravity.
- Pokalbio ID: `c633f67f-c6cf-4652-99ce-de78c25054a8`.
- Modelis pagal ankstesnį žurnalą: Claude Sonnet 4.6 (Thinking).
- Pirmasis įrašas: 2026-11-09 11:46:49 +0200.
- Paskutinis išrašo įrašas: 2026-11-09 12:00:01 +0200.


### Atlikti darbai ir rezultatai iki Codex tęsinio

- Patikrintas failų komplektas, dokumentacijos ir keturių README nuotraukų buvimas.
- Parengtas pradinis sesijų žurnalas, papildyta `.gitignore` ir README nuoroda į žurnalą.
- Vietinėje saugykloje yra pradinis commit `4a4c0f5` („MIPT praktikos darbas Nr. 3: Android skaiciuotuvas“). Remote nenustatytas.
- Ankstesnis žurnalas turėjo sutrumpintas užklausas su `[...]`. Codex tęsinyje jos pakeistos pilnais tekstais iš šių keturių sesijų `USER_INPUT` įrašų; įtraukti ir pakartotiniai nurodymai.
- Antigravity sesija nebaigė GitHub publikavimo, kai vartotojui išseko modelių limitai. Tęsimas aiškiai užfiksuotas kitame skyriuje.


### Vartotojo užklausa (2026-11-09 11:46:49 +0200, žingsnis 0)

````text
Tai atskira MIPT praktikos darbo Nr. 3 GitHub publikavimo ir sesijų žurnalo parengimo sesija.

Projekto aplankas:
C:/Users/Rokas/.gemini/antigravity/scratch/MiptCalculator

Ankstesnėse atskirose AI sesijose:
1. Sugeneruota Android skaičiuotuvo programėlė.
2. Atlikta kodo peržiūra, įgyvendinti du aukščiausio prioriteto pataisymai ir sukurtas CODE_REVIEW.md.
3. Sukurtas lietuviškas README.md su tikromis ekrano nuotraukomis.

Šioje sesijoje atlik šiuos darbus:

**1. Patikrink galutinio projekto failus.**

Perskaityk README.md ir CODE_REVIEW.md. Patikrink, kad projekte yra šaltiniai, resursai, Gradle konfigūracija, wrapper failai, testai ir visos README naudojamos ekrano nuotraukos. Pataisyk neveikiančius dokumentacijos kelius, jei tokių rasi. Programėlės kodo nekeisk.

**2. Sukurk SESSION_LOG.md projekto šaknyje.**

Naudok būtent šį pavadinimą, nes jis jau nurodytas README.

Žurnale kiekvienai iš keturių sesijų pateik:
- Paskirtį, tikrą datą, įrankį ir modelį, jei žinomas.
- Visą faktiškai siųstą vartotojo užklausą ir papildomus vartotojo nurodymus.
- Atliktus darbus, sukurtus ar pakeistus failus.
- Patikrų rezultatus ir likusius apribojimus.

Pirmoje sesijoje užfiksuok /grill-me klausimus bei vartotojo atsakymus. Antroje – abu konkrečius pataisymus ir patikrų rezultatus. Trečioje – README sukūrimą. Ketvirtoje – šią užklausą ir tikrus GitHub veiksmus.

Remkis pateiktais pokalbių išrašais ir tikrais failais. Neišgalvok užklausų, datų, modelių ar atliktų veiksmų. Jei ankstesnių sesijų duomenų nematai, paprašyk trūkstamų išrašų. Vien AI atsakymų suvestinės nepakeičia vartotojo užklausų.

README pridėk veikiančią santykinę nuorodą į SESSION_LOG.md.

**3. Paruošk Git saugyklą ir atlik commit/push.**

Patikrink esamą Git būseną, šaką ir remote. Jei Git saugyklos nėra, inicijuok ją. Jei nėra GitHub paskirties, paprašyk mano saugyklos adreso arba naujos saugyklos pavadinimo ir viešumo pasirinkimo. Neišgalvok adreso ar Git autoriaus tapatybės.

Patikrink .gitignore. Neįkelk local.properties, build aplankų, vietinių .gradle duomenų, prieigos raktų, slaptažodžių ar pasirašymo raktų. Įtrauk reikalingus projekto šaltinius, Gradle wrapper, dokumentaciją, sesijų žurnalą ir naudojamas ekrano nuotraukas.

Neatšauk vartotojo pakeitimų, neperrašyk nuotolinės istorijos ir nenaudok force push. Sukurk aiškų commit ir atlik push. Jei reikia prisijungimo, nurodyk konkretų mano veiksmą.

**4. Patikrink publikavimą ir užbaik žurnalą.**

Patikrink, kad commit pasiekė GitHub ir README rodomas su veikiančiomis nuorodomis bei nuotraukomis, jei turi prieigą tai patikrinti.

Po sėkmingo pirmojo push papildyk SESSION_LOG.md tikra GitHub nuoroda, įkelto programėlės commit identifikatoriumi ir faktiniais rezultatais. Žurnalo papildymą išsaugok atskiru dokumentacijos commit ir taip pat atlik push.

Galutinio žurnalo commit identifikatorių pateik atsakyme – nereikia jo įrašyti į patį žurnalą.

Baigęs pateik:
- Tikrą GitHub projekto nuorodą.
- Galutinio commit identifikatorių ir push rezultatą.
- Galutinio SESSION_LOG.md vietą, kad galėčiau jį pateikti į Moodle.
- Aiškiai įvardytus likusius darbus, jei kas nors nepavyko.

Neskelbk neįvykusių veiksmų sėkmingais.
````

### Vartotojo užklausa (2026-11-09 11:48:54 +0200, žingsnis 14)

````text
Tai atskira MIPT praktikos darbo Nr. 3 GitHub publikavimo ir sesijų žurnalo parengimo sesija.

Projekto aplankas:
C:/Users/Rokas/.gemini/antigravity/scratch/MiptCalculator

Ankstesnėse atskirose AI sesijose:
1. Sugeneruota Android skaičiuotuvo programėlė.
2. Atlikta kodo peržiūra, įgyvendinti du aukščiausio prioriteto pataisymai ir sukurtas CODE_REVIEW.md.
3. Sukurtas lietuviškas README.md su tikromis ekrano nuotraukomis.

Šioje sesijoje atlik šiuos darbus:

**1. Patikrink galutinio projekto failus.**

Perskaityk README.md ir CODE_REVIEW.md. Patikrink, kad projekte yra šaltiniai, resursai, Gradle konfigūracija, wrapper failai, testai ir visos README naudojamos ekrano nuotraukos. Pataisyk neveikiančius dokumentacijos kelius, jei tokių rasi. Programėlės kodo nekeisk.

**2. Sukurk SESSION_LOG.md projekto šaknyje.**

Naudok būtent šį pavadinimą, nes jis jau nurodytas README.

Žurnale kiekvienai iš keturių sesijų pateik:
- Paskirtį, tikrą datą, įrankį ir modelį, jei žinomas.
- Visą faktiškai siųstą vartotojo užklausą ir papildomus vartotojo nurodymus.
- Atliktus darbus, sukurtus ar pakeistus failus.
- Patikrų rezultatus ir likusius apribojimus.

Pirmoje sesijoje užfiksuok /grill-me klausimus bei vartotojo atsakymus. Antroje – abu konkrečius pataisymus ir patikrų rezultatus. Trečioje – README sukūrimą. Ketvirtoje – šią užklausą ir tikrus GitHub veiksmus.

Remkis pateiktais pokalbių išrašais ir tikrais failais. Neišgalvok užklausų, datų, modelių ar atliktų veiksmų. Jei ankstesnių sesijų duomenų nematai, paprašyk trūkstamų išrašų. Vien AI atsakymų suvestinės nepakeičia vartotojo užklausų.

README pridėk veikiančią santykinę nuorodą į SESSION_LOG.md.

**3. Paruošk Git saugyklą ir atlik commit/push.**

Patikrink esamą Git būseną, šaką ir remote. Jei Git saugyklos nėra, inicijuok ją. Jei nėra GitHub paskirties, paprašyk mano saugyklos adreso arba naujos saugyklos pavadinimo ir viešumo pasirinkimo. Neišgalvok adreso ar Git autoriaus tapatybės.

Patikrink .gitignore. Neįkelk local.properties, build aplankų, vietinių .gradle duomenų, prieigos raktų, slaptažodžių ar pasirašymo raktų. Įtrauk reikalingus projekto šaltinius, Gradle wrapper, dokumentaciją, sesijų žurnalą ir naudojamas ekrano nuotraukas.

Neatšauk vartotojo pakeitimų, neperrašyk nuotolinės istorijos ir nenaudok force push. Sukurk aiškų commit ir atlik push. Jei reikia prisijungimo, nurodyk konkretų mano veiksmą.

**4. Patikrink publikavimą ir užbaik žurnalą.**

Patikrink, kad commit pasiekė GitHub ir README rodomas su veikiančiomis nuorodomis bei nuotraukomis, jei turi prieigą tai patikrinti.

Po sėkmingo pirmojo push papildyk SESSION_LOG.md tikra GitHub nuoroda, įkelto programėlės commit identifikatoriumi ir faktiniais rezultatais. Žurnalo papildymą išsaugok atskiru dokumentacijos commit ir taip pat atlik push.

Galutinio žurnalo commit identifikatorių pateik atsakyme – nereikia jo įrašyti į patį žurnalą.

Baigęs pateik:
- Tikrą GitHub projekto nuorodą.
- Galutinio commit identifikatorių ir push rezultatą.
- Galutinio SESSION_LOG.md vietą, kad galėčiau jį pateikti į Moodle.
- Aiškiai įvardytus likusius darbus, jei kas nors nepavyko.

Neskelbk neįvykusių veiksmų sėkmingais.
````

### Vartotojo užklausa (2026-11-09 12:00:01 +0200, žingsnis 102)

````text
Tai atskira MIPT praktikos darbo Nr. 3 GitHub publikavimo ir sesijų žurnalo parengimo sesija.

Projekto aplankas:
C:/Users/Rokas/.gemini/antigravity/scratch/MiptCalculator

Ankstesnėse atskirose AI sesijose:
1. Sugeneruota Android skaičiuotuvo programėlė.
2. Atlikta kodo peržiūra, įgyvendinti du aukščiausio prioriteto pataisymai ir sukurtas CODE_REVIEW.md.
3. Sukurtas lietuviškas README.md su tikromis ekrano nuotraukomis.

Šioje sesijoje atlik šiuos darbus:

**1. Patikrink galutinio projekto failus.**

Perskaityk README.md ir CODE_REVIEW.md. Patikrink, kad projekte yra šaltiniai, resursai, Gradle konfigūracija, wrapper failai, testai ir visos README naudojamos ekrano nuotraukos. Pataisyk neveikiančius dokumentacijos kelius, jei tokių rasi. Programėlės kodo nekeisk.

**2. Sukurk SESSION_LOG.md projekto šaknyje.**

Naudok būtent šį pavadinimą, nes jis jau nurodytas README.

Žurnale kiekvienai iš keturių sesijų pateik:
- Paskirtį, tikrą datą, įrankį ir modelį, jei žinomas.
- Visą faktiškai siųstą vartotojo užklausą ir papildomus vartotojo nurodymus.
- Atliktus darbus, sukurtus ar pakeistus failus.
- Patikrų rezultatus ir likusius apribojimus.

Pirmoje sesijoje užfiksuok /grill-me klausimus bei vartotojo atsakymus. Antroje – abu konkrečius pataisymus ir patikrų rezultatus. Trečioje – README sukūrimą. Ketvirtoje – šią užklausą ir tikrus GitHub veiksmus.

Remkis pateiktais pokalbių išrašais ir tikrais failais. Neišgalvok užklausų, datų, modelių ar atliktų veiksmų. Jei ankstesnių sesijų duomenų nematai, paprašyk trūkstamų išrašų. Vien AI atsakymų suvestinės nepakeičia vartotojo užklausų.

README pridėk veikiančią santykinę nuorodą į SESSION_LOG.md.

**3. Paruošk Git saugyklą ir atlik commit/push.**

Patikrink esamą Git būseną, šaką ir remote. Jei Git saugyklos nėra, inicijuok ją. Jei nėra GitHub paskirties, paprašyk mano saugyklos adreso arba naujos saugyklos pavadinimo ir viešumo pasirinkimo. Neišgalvok adreso ar Git autoriaus tapatybės.

Patikrink .gitignore. Neįkelk local.properties, build aplankų, vietinių .gradle duomenų, prieigos raktų, slaptažodžių ar pasirašymo raktų. Įtrauk reikalingus projekto šaltinius, Gradle wrapper, dokumentaciją, sesijų žurnalą ir naudojamas ekrano nuotraukas.

Neatšauk vartotojo pakeitimų, neperrašyk nuotolinės istorijos ir nenaudok force push. Sukurk aiškų commit ir atlik push. Jei reikia prisijungimo, nurodyk konkretų mano veiksmą.

**4. Patikrink publikavimą ir užbaik žurnalą.**

Patikrink, kad commit pasiekė GitHub ir README rodomas su veikiančiomis nuorodomis bei nuotraukomis, jei turi prieigą tai patikrinti.

Po sėkmingo pirmojo push papildyk SESSION_LOG.md tikra GitHub nuoroda, įkelto programėlės commit identifikatoriumi ir faktiniais rezultatais. Žurnalo papildymą išsaugok atskiru dokumentacijos commit ir taip pat atlik push.

Galutinio žurnalo commit identifikatorių pateik atsakyme – nereikia jo įrašyti į patį žurnalą.

Baigęs pateik:
- Tikrą GitHub projekto nuorodą.
- Galutinio commit identifikatorių ir push rezultatą.
- Galutinio SESSION_LOG.md vietą, kad galėčiau jį pateikti į Moodle.
- Aiškiai įvardytus likusius darbus, jei kas nors nepavyko.

Neskelbk neįvykusių veiksmų sėkmingais.
````

## Ketvirto etapo tęsinys – Codex

- Įrankis: Codex; šis pokalbis naudotas užklausų parengimui ir, išsekus Antigravity modelių limitams, GitHub etapo užbaigimui. Tai nėra nauja kodo generavimo ar kodo peržiūros sesija.
- Codex pokalbio ID: `01a1bf73-9dea-7b60-abb8-42ee4369c029`.
- Data: 2026-11-09.
- Vartotojo nurodymai:

````text
all the models ran out of tokens there
````

Paprašius nurodyti GitHub paskirtį, vartotojas atsakė:

````text
to the same one as last time
````

### Atlikti darbai

- Pagal ankstesnio darbo pokalbį ir vietinio WordCounter projekto remote nustatyta ta pati saugykla: `rokelism/WordCounter`.
- Parengtas įkėlimas į atskirą `MiptCalculator/` aplanką, išsaugant ankstesnį WordCounter projektą ir jo istoriją.
- Ištaisyta README teksto koduotė: sugadintos lietuviškos raidės ir matematiniai simboliai atkurti į UTF-8. Programėlės šaltiniai nekeisti.
- Sesijų žurnalas atkurtas iš pilnų originalių vartotojo užklausų, įskaitant pranešimą apie paleidimo gedimą ir vėlesnį patvirtinimą „viskas veikia“.
- Patikrinta, kad dokumentacijos vietinės nuorodos ir nuotraukos egzistuoja, o projekte nėra įkeliami vietiniai build duomenys ir `local.properties`.

### Naujausias vartotojo nurodymas ir paskirties pakeitimas

````text
i did finish what do you mean
````

````text
still nothing?
````

````text
i dont get what you want me to do, can you not just post it in a new repo?
````

Pagal paskutinį nurodymą ankstesnis planas papildyti `rokelism/WordCounter` pakeistas: projektas parengtas atskiros naujos `CalculatorVibe` saugyklos šaknyje. Ankstesnis WordCounter papildymo commit nebuvo įkeltas. GitHub puslapis atidarytas naršyklėje. Vartotojas prisijungė ir pats sukūrė `CalculatorVibe`; AI į šią saugyklą įkėlė parengtą projektą.

### Vartotojo pasirinkta nauja saugykla

Vartotojas pranešė:

    I created it and called it CalculatorVibe

Naršyklėje patikrinta nauja tuščia https://github.com/rokelism/CalculatorVibe saugykla. Projektą joje publikuoja AI per GitHub failų įkėlimo sąsają; tai aiškiai atskiriama nuo ankstesnio nesėkmingo git push bandymo.

### Galutinis GitHub publikavimo rezultatas

- Data: 2026-11-09.
- Saugyklą sukūrė vartotojas: [rokelism/CalculatorVibe](https://github.com/rokelism/CalculatorVibe). Saugykla vieša; šis viešumas patikrintas GitHub puslapyje ir nekeistas.
- AI įkėlė visus **58 projekto failus** į `main` šakos šaknį: programėlės šaltinius, Android resursus, testus, Gradle konfigūraciją ir wrapper, README, CODE_REVIEW, šį sesijų žurnalą bei ekrano nuotraukas.
- Visų programėlės failų publikavimo commit: [`2fedada60387d3cb4f5ead2b42b9d78dd102b9e7`](https://github.com/rokelism/CalculatorVibe/commit/2fedada60387d3cb4f5ead2b42b9d78dd102b9e7).
- Publikavimo patikra: iš GitHub parsisiųsta saugykla ir perskaitytas nuotolinės `main` šakos Git archyvas. Patikrintas tikslus 58 failų sąrašas ir kiekvieno failo turinys, lyginant su parengtu projektu; **trūkstamų, papildomų ar besiskiriančių failų nėra** (teksto failams leidžiami vien eilučių pabaigos skirtumai).
- Įkėlimas atliktas **per GitHub naršyklės failų įkėlimo sąsają**, kuri sukūrė tikrus commit pagrindinėje šakoje. Tiesioginė `git push` komanda į šią naują saugyklą agento aplinkoje nebuvo vykdoma sėkmingai, nes jai nebuvo prieinamas išsaugotas prisijungimas. Šis techninis skirtumas užfiksuotas atvirai.
- Ankstesnis planas papildyti WordCounter atšauktas pagal naujausią vartotojo nurodymą. **WordCounter saugykla nepakeista.**
- README per publikavimą pataisyta teksto koduotė ir patikslintas Gradle Java 25 kriterijus, atskiriant jį nuo programėlės Java 17 suderinamumo. Pridėta Linux/macOS wrapper paleidimo teisių pastaba. Programėlės kodas publikavimo metu nekeistas.
- Šis papildytas žurnalas ir README įkeliami atskiru galutiniu dokumentacijos commit. Jo identifikatorius pateikiamas GitHub istorijoje ir baigiamame atsakyme; žurnale nereikia nurodyti jo paties commit.
- Ankstesnis 28/28 testų rezultatas priklauso kodo peržiūros sesijai ir papildomai patikrintas iš išlikusios XML ataskaitos. Šiame publikavimo etape testai iš naujo nevykdyti, nes programėlės šaltiniai nepakeisti.

## Pateikimas į Moodle

Pateikti šį `SESSION_LOG.md` ir [GitHub projekto nuorodą](https://github.com/rokelism/CalculatorVibe). README bei CODE_REVIEW yra tos pačios viešos saugyklos šaknyje. Prieš pateikimą galima atidaryti nuorodą ir įsitikinti, kad vertintojui pasiekiami failai.
