# Boshi AI Keyboard v0.3 (Android preview)

**No manual dictionary download or import is required.** This build packages the GPL Boshiamy character table into the APK at build time. It works offline after installation.

## How to use
1. Download the **latest successful** `Boshi-debug.apk` from [Releases](https://github.com/yichincho/Boshi/releases/tag/preview).
2. Install the APK, open the **Boshi AI Keyboard** settings, and check the displayed number of loaded mappings. It should be well over 10,000.
3. Open **System → Languages & input → On-screen keyboard**, enable Boshi, then select it as the input method.
4. Type Latin Boshiamy codes and select an offline phrase candidate. The optional AI action needs a user-provided Gemini or NVIDIA API key.
5. If desired, use the remaining **Rime YAML / TSV** import button to override the built-in dictionary.

## Dictionary source and license
- Upstream: https://github.com/jdh8/ibus-boshiamy
- Original table: https://github.com/jdh8/ibus-boshiamy/blob/master/boshiamy.txt
- Header states LGPL for the file and GPL for the table; treat redistribution conservatively under GPL terms. Keep upstream author attribution and corresponding source accessible. Build workflow fetches this source and generates an ASCII-letter-code subset in `app/src/main/assets/boshiamy.tsv`.
- Main original application source is available in this repository under MIT. The combined distributed application/dictionary package is subject to applicable GPL/copyleft conditions. See upstream licenses before redistributing modified builds.

## Limitations
This is an experimental **native Android keyboard**, not a full port of Trime/Rime. Its bundled dictionary currently excludes codes that include punctuation or other non-letter symbols. It supports offline multi-character segmentation, not true Rime contextual prediction. On-device tests have not yet been performed. API keys are stored in app-private preferences without hardware-backed encryption; do not use sensitive text with cloud AI mode.

## Build
GitHub Actions builds Android debug APK with Android SDK 35 and Java 17, validates the downloaded dictionary (more than 10,000 entries), then releases a directly downloadable APK at:
https://github.com/yichincho/Boshi/releases/download/preview/Boshi-debug.apk

Other source files: `CodeDecoder.java` reads the bundled asset by default, `DictionaryImporter.java` supports optional custom table import, and `SettingsActivity.java` shows the mapping count and AI controls.
