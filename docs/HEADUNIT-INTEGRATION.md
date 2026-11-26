# Experimental 0.8: actual Android Auto receiver composition

Pinned Open Headunit source: https://github.com/andreknieriem/open-headunit/tree/7152adf13a1e5a55799da12f841c8016d1f5cfe0 (AGPL-3.0).
The CI script builds its application with DriveCore cockpit as an Android library module in the same APK. No external APK is used. The generated binary is experimental, not a validated release.

Manual USB/Wi-Fi projection only. No root, shell, self-ADB, OEM-app disabling, automatic boot, default USB handler, or home launcher takeover in this experiment. These restrictions do not establish complete compatibility or security; audit and bench test are required. Native wireless operation may require manufacturer-supported Bluetooth and Wi-Fi access. Platform MediaCodec used for projection video; separate source-built FFmpeg audio decoder remains. No CarPlay receiver or TDT video implementation.

License: preserve upstream AGPL license and attribution. Any distribution of a combined derivative must comply with the AGPL and other third-party licenses, including supplying the complete corresponding source and build instructions. This experimental branch does not change the licensing of the original DriveCore branches or establish rights to redistribute code without its copyright holders' authorization. Do not publish or deploy as a release until license and security review. Source archives generated with the experimental artifact; no private signing key is uploaded.

Do not install this experimental debug build over the existing Native 0.7 before verification of package, signing, permissions and actual parked-device tests. Keep 6.9 and 0.7 intact.
