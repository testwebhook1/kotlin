/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_GC_COMMON_GC_SCHEDULER_H
#define RUNTIME_GC_COMMON_GC_SCHEDULER_H

#include <atomic>
#include <cinttypes>
#include <cstddef>
#include <functional>

#include "CompilerConstants.hpp"
#include "Types.h"
#include "Utils.hpp"

namespace kotlin {
namespace gc {

namespace internal {

inline bool useGCTimer() noexcept {
#if KONAN_NO_THREADS
    return false;
#else
    // With aggressive mode we use safepoint counting to drive GC.
    return !compiler::gcAggressive();
#endif
}

} // namespace internal

struct GCSchedulerConfig {
    std::atomic<size_t> threshold = 100000; // Roughly 1 safepoint per 10ms (on a subset of examples on one particular machine).
    std::atomic<size_t> allocationThresholdBytes = 10 * 1024 * 1024; // 10MiB by default.
    std::atomic<uint64_t> cooldownThresholdNs = 200 * 1000 * 1000; // 200 milliseconds by default.
    std::atomic<bool> autoTune = false;
    std::atomic<uint64_t> regularGcIntervalUs = 200 * 1000; // 200 milliseconds by default.

    GCSchedulerConfig() noexcept {
        if (compiler::gcAggressive()) {
            // TODO: Make it even more aggressive and run on a subset of backend.native tests.
            threshold = 1000;
            allocationThresholdBytes = 10000;
            cooldownThresholdNs = 0;
        }
    }
};

class GCSchedulerThreadData;

class GCSchedulerData {
public:
    virtual ~GCSchedulerData() = default;

    // Called by different mutator threads.
    virtual void OnSafePoint(GCSchedulerThreadData& threadData) noexcept = 0;

    // Always called by the GC thread.
    virtual void OnPerformFullGC() noexcept = 0;

    // Can only be called once.
    virtual void SetScheduleGC(std::function<void()> scheduleGC) noexcept = 0;
};

class GCSchedulerThreadData {
public:
    static constexpr size_t kFunctionEpilogueWeight = 1;
    static constexpr size_t kLoopBodyWeight = 1;
    static constexpr size_t kExceptionUnwindWeight = 1;

    explicit GCSchedulerThreadData(GCSchedulerConfig& config, GCSchedulerData& gcData) noexcept : config_(config), gcData_(gcData) {
        ClearCountersAndUpdateThresholds();
    }

    // Should be called on encountering a safepoint.
    void OnSafePointRegular(size_t weight) noexcept {
        if (!internal::useGCTimer()) {
            safePointsCounter_ += weight;
            if (safePointsCounter_ < safePointsCounterThreshold_) {
                return;
            }
            OnSafePointSlowPath();
        }
    }

    // Should be called on encountering a safepoint placed by the allocator.
    // TODO: Should this even be a safepoint (i.e. a place, where we suspend)?
    void OnSafePointAllocation(size_t size) noexcept {
        allocatedBytes_ += size;
        if (allocatedBytes_ < allocatedBytesThreshold_) {
            return;
        }
        OnSafePointSlowPath();
    }

    void OnStoppedForGC() noexcept { ClearCountersAndUpdateThresholds(); }

    size_t allocatedBytes() const noexcept { return allocatedBytes_; }

    size_t safePointsCounter() const noexcept { return safePointsCounter_; }

private:
    void OnSafePointSlowPath() noexcept {
        gcData_.OnSafePoint(*this);
        ClearCountersAndUpdateThresholds();
    }

    void ClearCountersAndUpdateThresholds() noexcept {
        allocatedBytes_ = 0;
        safePointsCounter_ = 0;

        allocatedBytesThreshold_ = config_.allocationThresholdBytes;
        safePointsCounterThreshold_ = config_.threshold;
    }

    GCSchedulerConfig& config_;
    GCSchedulerData& gcData_;

    size_t allocatedBytes_ = 0;
    size_t allocatedBytesThreshold_ = 0;
    size_t safePointsCounter_ = 0;
    size_t safePointsCounterThreshold_ = 0;
};

namespace internal {

KStdUniquePtr<GCSchedulerData> MakeGCSchedulerDataWithTimer(GCSchedulerConfig& config) noexcept;
KStdUniquePtr<GCSchedulerData> MakeGCSchedulerDataWithoutTimer(
        GCSchedulerConfig& config, std::function<uint64_t()> currentTimeCallbackNs) noexcept;
KStdUniquePtr<GCSchedulerData> MakeGCSchedulerData(GCSchedulerConfig& config) noexcept;

} // namespace internal

class GCScheduler : private Pinned {
public:
    GCScheduler() noexcept : gcData_(internal::MakeGCSchedulerData(config_)) {}

    GCSchedulerConfig& config() noexcept { return config_; }
    GCSchedulerData& gcData() noexcept { return *gcData_; }

private:
    GCSchedulerConfig config_;
    KStdUniquePtr<GCSchedulerData> gcData_;
};

} // namespace gc
} // namespace kotlin

#endif // RUNTIME_GC_COMMON_GC_SCHEDULER_H
