#pragma once
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <vector>
#include "Fft.h"

namespace skb::analyzer {

/**
 * Fast spectral energy classifier: Low / Mid / High band split + centroid.
 * Used by the Smart Queue to sequence similar-feel tracks (e.g. build/drop).
 *
 * Bands (roughly musical):
 *   LOW   : 20  – 250 Hz
 *   MID   : 250 – 4000 Hz
 *   HIGH  : 4000 – 16000 Hz
 *
 * Output: normalized 0..1 energy per band + overall energy + centroid (Hz).
 * Offline one-shot; not realtime-safe.
 */
class EnergyDetector {

public:
    struct Result {
        float low = 0.0f;       // 0..1, fraction of total power
        float mid = 0.0f;
        float high = 0.0f;
        float overallRmsDb = -70.0f;
        float centroidHz = 0.0f;
        bool ok = false;
    };

    static Result analyze(const float* interleaved,
                          int64_t totalFrames,
                          int channels,
                          int sampleRate) {
        Result r;
        if (totalFrames < 2048 || channels <= 0 || sampleRate <= 0) return r;

        // Windowed FFTs, averaged. Use 50% overlap, cap total windows for speed.
        constexpr int N = 2048;
        constexpr int HOP = 1024;
        Fft fft(11);  // 2048-pt

        std::vector<float> window(N);
        for (int i = 0; i < N; ++i) {
            window[i] = 0.5f * (1.0f - std::cos(2.0f * float(M_PI) * i / (N - 1)));
        }

        // Cap number of windows to keep analysis < ~200ms on phone
        constexpr int MAX_WINDOWS = 512;

        std::vector<double> powerSpectrum(N / 2, 0.0);
        double rmsSum = 0.0;
        int64_t rmsCount = 0;

        int64_t start = 0;
        int windows = 0;
        std::vector<float> frame(N);

        while (start + N <= totalFrames && windows < MAX_WINDOWS) {
            // Build mono windowed frame
            for (int i = 0; i < N; ++i) {
                const float* f = interleaved + (start + i) * channels;
                float s = f[0];
                if (channels >= 2) s = 0.5f * (s + f[1]);
                frame[i] = s * window[i];
                rmsSum += static_cast<double>(s) * s;
                rmsCount += 1;
            }

            fft.forward(frame.data());

            std::vector<float> mag(N / 2);
            fft.magnitudes(mag.data());

            for (int k = 0; k < N / 2; ++k) {
                powerSpectrum[k] += static_cast<double>(mag[k]) * mag[k];
            }

            start += HOP;
            ++windows;
        }

        if (windows == 0 || rmsCount == 0) return r;

        // Normalize by #windows
        for (auto& v : powerSpectrum) v /= windows;

        const double freqPerBin = static_cast<double>(sampleRate) / N;

        double lowP = 0.0, midP = 0.0, highP = 0.0;
        double centroidNum = 0.0, centroidDen = 0.0;

        for (int k = 1; k < N / 2; ++k) {
            const double f = k * freqPerBin;
            const double p = powerSpectrum[k];
            if (f < 20.0) continue;
            if (f < 250.0)         lowP  += p;
            else if (f < 4000.0)   midP  += p;
            else if (f < 16000.0)  highP += p;
            else break;

            centroidNum += f * p;
            centroidDen += p;
        }

        const double total = lowP + midP + highP + 1e-12;
        r.low  = static_cast<float>(lowP  / total);
        r.mid  = static_cast<float>(midP  / total);
        r.high = static_cast<float>(highP / total);
        r.centroidHz = (centroidDen > 1e-12)
            ? static_cast<float>(centroidNum / centroidDen)
            : 0.0f;

        const double rms = std::sqrt(rmsSum / static_cast<double>(rmsCount));
        r.overallRmsDb = (rms > 1e-9)
            ? static_cast<float>(20.0 * std::log10(rms))
            : -70.0f;

        r.ok = true;
        return r;
    }
};

} // namespace skb::analyzer
