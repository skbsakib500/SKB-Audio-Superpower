#include "WavReader.h"

#include <cstring>
#include <fstream>

namespace skb {

namespace {

uint16_t rdU16(std::ifstream& f) {
    uint8_t b[2];
    f.read(reinterpret_cast<char*>(b), 2);
    return static_cast<uint16_t>(b[0]) | (static_cast<uint16_t>(b[1]) << 8);
}

uint32_t rdU32(std::ifstream& f) {
    uint8_t b[4];
    f.read(reinterpret_cast<char*>(b), 4);
    return static_cast<uint32_t>(b[0])
         | (static_cast<uint32_t>(b[1]) << 8)
         | (static_cast<uint32_t>(b[2]) << 16)
         | (static_cast<uint32_t>(b[3]) << 24);
}

} // namespace

bool WavReader::load(const std::string& path, WavData& out, std::string& error) {
    std::ifstream f(path, std::ios::binary);
    if (!f) { error = "cannot open file"; return false; }

    char riff[4];
    f.read(riff, 4);
    if (!f || std::memcmp(riff, "RIFF", 4) != 0) { error = "not a RIFF file"; return false; }
    (void)rdU32(f); // file size

    char wave[4];
    f.read(wave, 4);
    if (!f || std::memcmp(wave, "WAVE", 4) != 0) { error = "not a WAVE file"; return false; }

    uint16_t audioFormat = 0;
    uint16_t numChannels = 0;
    uint32_t sampleRate = 0;
    uint16_t bitsPerSample = 0;
    std::vector<uint8_t> dataBytes;
    bool haveFmt = false;
    bool haveData = false;

    while (f && !haveData) {
        char id[4];
        f.read(id, 4);
        if (!f) break;
        uint32_t size = rdU32(f);
        if (!f) break;

        if (std::memcmp(id, "fmt ", 4) == 0) {
            audioFormat = rdU16(f);
            numChannels = rdU16(f);
            sampleRate = rdU32(f);
            (void)rdU32(f);              // byte rate
            (void)rdU16(f);              // block align
            bitsPerSample = rdU16(f);

            uint32_t remaining = size > 16 ? size - 16 : 0;
            if (remaining >= 22 && audioFormat == 0xFFFE) {
                (void)rdU16(f);          // cbSize
                (void)rdU16(f);          // valid bits
                (void)rdU32(f);          // channel mask
                uint16_t realFormat = rdU16(f);
                char skip[14];
                f.read(skip, 14);
                audioFormat = realFormat;
            } else if (remaining > 0) {
                f.seekg(remaining, std::ios::cur);
            }
            haveFmt = true;
        } else if (std::memcmp(id, "data", 4) == 0) {
            dataBytes.resize(size);
            f.read(reinterpret_cast<char*>(dataBytes.data()), size);
            haveData = true;
        } else {
            f.seekg(size, std::ios::cur);
        }
    }

    if (!haveFmt) { error = "missing fmt chunk"; return false; }
    if (!haveData) { error = "missing data chunk"; return false; }
    if (numChannels == 0) { error = "invalid channel count"; return false; }
    if (bitsPerSample == 0 || bitsPerSample % 8 != 0) { error = "invalid bit depth"; return false; }

    const size_t bytesPerSample = bitsPerSample / 8;
    const size_t totalSamples = dataBytes.size() / bytesPerSample;
    out.samples.resize(totalSamples);

    const uint8_t* raw = dataBytes.data();

    if (audioFormat == 1) {          // PCM integer
        if (bitsPerSample == 16) {
            for (size_t i = 0; i < totalSamples; ++i) {
                int16_t v;
                std::memcpy(&v, raw + i * 2, 2);
                out.samples[i] = v / 32768.0f;
            }
        } else if (bitsPerSample == 24) {
            for (size_t i = 0; i < totalSamples; ++i) {
                const uint8_t* b = raw + i * 3;
                int32_t v = static_cast<int32_t>(b[0])
                          | (static_cast<int32_t>(b[1]) << 8)
                          | (static_cast<int32_t>(b[2]) << 16);
                if (v & 0x800000) v |= 0xFF000000;
                out.samples[i] = v / 8388608.0f;
            }
        } else if (bitsPerSample == 32) {
            for (size_t i = 0; i < totalSamples; ++i) {
                int32_t v;
                std::memcpy(&v, raw + i * 4, 4);
                out.samples[i] = v / 2147483648.0f;
            }
        } else if (bitsPerSample == 8) {
            for (size_t i = 0; i < totalSamples; ++i) {
                out.samples[i] = (static_cast<int>(raw[i]) - 128) / 128.0f;
            }
        } else {
            error = "unsupported PCM bit depth";
            return false;
        }
    } else if (audioFormat == 3) {   // IEEE float
        if (bitsPerSample == 32) {
            for (size_t i = 0; i < totalSamples; ++i) {
                float v;
                std::memcpy(&v, raw + i * 4, 4);
                out.samples[i] = v;
            }
        } else if (bitsPerSample == 64) {
            for (size_t i = 0; i < totalSamples; ++i) {
                double v;
                std::memcpy(&v, raw + i * 8, 8);
                out.samples[i] = static_cast<float>(v);
            }
        } else {
            error = "unsupported float bit depth";
            return false;
        }
    } else {
        error = "unsupported WAV codec (only PCM / IEEE float)";
        return false;
    }

    out.sampleRate = sampleRate;
    out.channels = numChannels;
    out.bitsPerSample = bitsPerSample;
    out.frameCount = out.channels > 0 ? (totalSamples / out.channels) : 0;
    return true;
}

} // namespace skb
