# Architecture — SKB Audio Superpower

    UI (Compose)
        ↓
    Playback Controller
        ↓
    Foreground Audio Service (mediaPlayback)
        ↓
    MediaSession
        ↓
    Native Audio Engine (Oboe -> AAudio -> OpenSL ES)
        ↓
    DSP Pipeline
        ↓
    Output Formatter
        ↓
    Audio Device

## Sample-rate reality
| Layer    | Meaning                              |
|----------|--------------------------------------|
| Source   | File native rate (44.1/96 kHz)       |
| DSP      | Internal float32 (up to 768 kHz)     |
| Hardware | Device output rate (<= 192 kHz)      |

UI shows all three honestly.
