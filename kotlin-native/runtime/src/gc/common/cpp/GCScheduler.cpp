/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCScheduler.hpp"

#include "CompilerConstants.hpp"
#include "KAssert.h"

using namespace kotlin;

namespace {

// TODO: We need a separate `GCData` for targets without threads.
class GCDataImpl : public gc::GCScheduler::GCData {
public:
    using CurrentTimeCallback = std::function<uint64_t()>;

    GCDataImpl(gc::GCSchedulerConfig& config, CurrentTimeCallback currentTimeCallbackNs) noexcept :
        config_(config), currentTimeCallbackNs_(std::move(currentTimeCallbackNs)), timeOfLastGcNs_(currentTimeCallbackNs_()) {}

    // May be called by different threads via `ThreadData`.
    void OnSafePoint(gc::GCScheduler::ThreadData& threadData) noexcept override {
        size_t allocatedBytes = threadData.allocatedBytes();
        if (allocatedBytes > config_.allocationThresholdBytes || currentTimeCallbackNs_() - timeOfLastGcNs_ >= config_.cooldownThresholdNs) {
            RuntimeAssert(static_cast<bool>(scheduleGC_), "scheduleGC_ cannot be empty");
            scheduleGC_();
        }
    }

    // Always called by the GC thread.
    void OnPerformFullGC() noexcept override {
        timeOfLastGcNs_ = currentTimeCallbackNs_();
    }

    // Can only be called once.
    void SetScheduleGC(std::function<void()> scheduleGC) noexcept override {
        RuntimeAssert(static_cast<bool>(scheduleGC), "scheduleGC cannot be empty");
        RuntimeAssert(!static_cast<bool>(scheduleGC_), "scheduleGC must not have been set");
        scheduleGC_ = std::move(scheduleGC);
        timer_ = ::make_unique<RepeatedTimer>(std::chrono::microseconds(config_.regularGcIntervalUs), [this]() {
            OnTimer();
            return std::chrono::microseconds(config_.regularGcIntervalUs);
        });
    }

private:
    void OnTimer() noexcept {
        // TODO: Probably makes sense to check memory usage of the process.
        scheduleGC_();
    }

    gc::GCSchedulerConfig& config_;
    CurrentTimeCallback currentTimeCallbackNs_;

    std::atomic<uint64_t> timeOfLastGcNs_;
    std::function<void()> scheduleGC_;
    KStdUniquePtr<RepeatedTimer> timer_;
};

}

// static
KStdUniquePtr<gc::GCScheduler::GCData> gc::GCScheduler::NewGCDataImpl(GCSchedulerConfig& config, std::function<uint64_t()> currentTimeCallbackNs) noexcept {
    return ::make_unique<GCDataImpl>(config, currentTimeCallbackNs);
}