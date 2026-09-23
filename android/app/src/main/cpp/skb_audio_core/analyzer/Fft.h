#pragma once
#include <cmath>
#include <vector>
#include <complex>
#include <algorithm>

namespace skb::analyzer {

/**
 * Iterative radix-2 FFT. Sized once (power of two), no allocation in process.
 * Realtime-safe: only scalar math + preallocated buffers.
 */
class Fft {
public:
    explicit Fft(int sizePow2 = 10) { setSize(sizePow2); }

    void setSize(int sizePow2) {
        size_ = 1 << sizePow2;
        cosTable_.resize(size_ / 2);
        sinTable_.resize(size_ / 2);
        for (int i = 0; i < size_ / 2; ++i) {
            const double theta = 2.0 * M_PI * i / size_;
            cosTable_[i] = static_cast<float>(std::cos(theta));
            sinTable_[i] = static_cast<float>(std::sin(theta));
        }
        re_.assign(size_, 0.0f);
        im_.assign(size_, 0.0f);
    }

    int size() const { return size_; }
    int bins() const { return size_ / 2; }

    void forward(const float* input) {
        const int n = size_;
        for (int i = 0; i < n; ++i) { re_[i] = input[i]; im_[i] = 0.0f; }

        for (int i = 1, j = 0; i < n; ++i) {
            int bit = n >> 1;
            for (; j & bit; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) {
                std::swap(re_[i], re_[j]);
                std::swap(im_[i], im_[j]);
            }
        }

        for (int len = 2; len <= n; len <<= 1) {
            const int step = n / len;
            const int half = len >> 1;
            for (int i = 0; i < n; i += len) {
                for (int j = 0; j < half; ++j) {
                    const int tIdx = j * step;
                    const float wr =  cosTable_[tIdx];
                    const float wi = -sinTable_[tIdx];
                    const int a = i + j;
                    const int b = i + j + half;
                    const float tr = re_[b] * wr - im_[b] * wi;
                    const float ti = re_[b] * wi + im_[b] * wr;
                    re_[b] = re_[a] - tr;
                    im_[b] = im_[a] - ti;
                    re_[a] += tr;
                    im_[a] += ti;
                }
            }
        }
    }

    void magnitudes(float* outMag) const {
        const int b = bins();
        for (int i = 0; i < b; ++i) {
            outMag[i] = std::sqrt(re_[i] * re_[i] + im_[i] * im_[i]) / size_;
        }
    }

private:
    int size_ = 1024;
    std::vector<float> cosTable_, sinTable_;
    std::vector<float> re_, im_;
};

} // namespace skb::analyzer
