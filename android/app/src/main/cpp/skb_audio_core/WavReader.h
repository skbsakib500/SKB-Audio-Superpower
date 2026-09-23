#pragma once
#include <cstdint>
#include <string>
#include <vector>

namespace skb {

struct WavData {
    uint32_t sampleRate = 0;
    uint16_t channels = 0;
    uint16_t bitsPerSample = 0;
    std::vector<float> samples;   // interleaved, normalized to [-1, 1]
    uint64_t frameCount = 0;      // samples.size() / channels
};

class WavReader {
public:
    // Loads a WAV file. Supports PCM 8/16/24/32-bit int and IEEE 32/64-bit float.
    // Returns false on error and fills `error`.
    static bool load(const std::string& path, WavData& out, std::string& error);
};

} // namespace skb
