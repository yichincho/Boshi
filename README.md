# Boshi AI Keyboard (Android) · v0.2 preview

An experimental **Android Input Method Editor** (IME) for continuous shape-code composition and optional AI sentence candidates. It is a standalone proof of concept, not a fork of Trime and **not yet a fully compatible Boshiamy implementation**.

## Features
- Android keyboard with English code keys, candidate bar, backspace, space and enter.
- Imports a Rime **`.dict.yaml`** table using `word<TAB>code` directly, or a UTF-8 TSV table using `code<TAB>word`. The import converts to app-private `codes.tsv`. Codes containing punctuation or special Rime syntax are skipped by the current prototype. Users must supply dictionaries they are licensed to use.
- Offline continuous decoding uses dictionary dynamic programming (beam search), producing up to five phrase candidates. Unknown codes stay uncommitted; no copyrighted dictionary shipped.
- Optional Gemini and NVIDIA OpenAI-compatible API sentence rewriting, invoked **only when AI button is tapped**. No network access otherwise. API key stays in app-private preferences, but is **not hardware-backed encrypted** in this prototype. Do not use sensitive text with cloud mode.
- No cloud calls in password fields. Does not transmit passwords or raw key events.
- App preferences screen for choosing provider, model and API key.

## Build / direct APK
The workflow in `.github/workflows/android.yml` builds a **debug test APK** and publishes `Boshi-debug.apk` to GitHub Releases tagged `preview`. **This is a test build, not production signed.** Download from: https://github.com/yichincho/Boshi/releases/download/preview/Boshi-debug.apk (valid only after successful workflow).

## Install
Install the APK, open **Boshi AI Keyboard** settings, import a licensed Rime `.dict.yaml` or UTF-8 TSV code table, open Android Settings > System > Keyboard / Languages & input > On-screen keyboard, enable Boshi, and select it as current keyboard.

## Mapping TSV example (illustrative, not a Boshiamy table)
```tsv
ab\t你
cd\t好
ab\t妳
ef\t世界
```
Typing `abcd` yields `你好` and `妳好` when loaded. Each dictionary entry maps a code to one or more Unicode characters; all segmentations are searched, with a conservative maximum candidate count.

## Architecture and limitations
- `BoshiImeService.java`: Android IME, offline candidates, optional cloud action.
- `CodeDecoder.java`: beam search with segment boundaries and weighted local rankings.\n- `DictionaryImporter.java`: imports and normalizes local Rime YAML or TSV.\n- `res/drawable/ic_boshi.xml`: shrimp-themed vector launcher icon.
- `SettingsActivity.java`: config & SAF import.
- `AiClient.java`: HTTPS clients with JSON parsing, timeouts and no key logging.

This v0.1 doesn't include actual Rime/Trime, official Boshiamy keyboard layouts, licensed word tables, learning dictionaries, punctuation customization or a mature preedit UI. The default keyboard requires a code table import before Chinese conversion can function. AI responses can be inaccurate; user selects a candidate before committing.

License for original code in this repository: MIT. Third-party table licenses remain with their authors; nothing here grants redistribution permission for Boshiamy mapping data.

## One-tap open-source Boshiamy dictionary (v0.2+)
Open settings and tap **一鍵下載開源嘸蝦米字碼表（GPLv3+）**. After confirmation, the app fetches https://github.com/jdh8/ibus-boshiamy/blob/master/boshiamy.txt through HTTPS and converts the `BEGIN_TABLE` section (code, character, frequency) into the private TSV format used by the experimental decoder. You can still import your own TSV or Rime YAML. This does **not** bundle or redistribute the upstream dictionary. Its author states **GPLv3+**; preserve the license and attribution if redistributing the dictionary. Note: codes using punctuation are currently ignored by this prototype; full Boshiamy compatibility requires further keyboard support.
