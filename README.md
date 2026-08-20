# i40-android

Port aplikacji diagnostycznej OBD-II [`i40-check`](https://github.com/Iakirmon/ios-obd2-ble-diagnostics)
z iOS na radio z Androidem w Hyundaiu i40 (2015, 2.0 GDI).

Radio wstaje z zapłonem, więc **komplet przejazdów powstaje sam**.
Każda liczba pochodzi z pomiaru albo z nazwanego modelu. Nieudany odczyt mówi „—", nigdy „0".

Kod powyżej transportu powstaje na atrapie odtwarzającej **zapis z tego auta z 2026-08-08**.
Prawdziwy Bluetooth wchodzi w etapie 9.

## Wymagania

- JDK 17
- Android SDK, `compileSdk` / `targetSdk` 34, `minSdk` 31

```bash
./gradlew ktlintCheck lint test
```

## Dokumenty

- `docs/spec/2026-08-14-i40-android-design.md` — projekt bazowy
- `docs/zrodla.md` — bibliografia; liczba bez źródła nie istnieje
- `AGENTS.md` — skrót zasad dla narzędzi, które nie czytają `.cursor/rules/`

Licencja: MIT.
