/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCScheduler.hpp"

#include "CompilerConstants.hpp"
#include "KAssert.h"
#include "Porting.h"
#include "RepeatedTimer.hpp"

using namespace kotlin;

namespace {

class GCSchedulerDataWithTimer : public gc::GCSchedulerData {
public:
    explicit GCSchedulerDataWithTimer(gc::GCSchedulerConfig& config) noexcept :
        config_(config) {}

    void OnSafePoint(gc::GCSchedulerThreadData& threadData) noexcept override {
        size_t allocatedBytes = threadData.allocatedBytes();
        if (allocatedBytes > config_.allocationThresholdBytes) {
            RuntimeAssert(static_cast<bool>(scheduleGC_), "scheduleGC_ cannot be empty");
            scheduleGC_();
        }
    }

    void OnPerformFullGC() noexcept override {}

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

    std::function<void()> scheduleGC_;
    KStdUniquePtr<RepeatedTimer> timer_;
};

class GCSchedulerDataWithoutTimer : public gc::GCSchedulerData {
public:
    using CurrentTimeCallback = std::function<uint64_t()>;

    GCSchedulerDataWithoutTimer(gc::GCSchedulerConfig& config, CurrentTimeCallback currentTimeCallbackNs) noexcept :
        config_(config), currentTimeCallbackNs_(std::move(currentTimeCallbackNs)), timeOfLastGcNs_(currentTimeCallbackNs_()) {}

    void OnSafePoint(gc::GCSchedulerThreadData& threadData) noexcept override {
        size_t allocatedBytes = threadData.allocatedBytes();
        if (allocatedBytes > config_.allocationThresholdBytes ||
            currentTimeCallbackNs_() - timeOfLastGcNs_ >= config_.cooldownThresholdNs) {
            RuntimeAssert(static_cast<bool>(scheduleGC_), "scheduleGC_ cannot be empty");
            scheduleGC_();
        }
    }

    void OnPerformFullGC() noexcept override { timeOfLastGcNs_ = currentTimeCallbackNs_(); }

    void SetScheduleGC(std::function<void()> scheduleGC) noexcept override {
        RuntimeAssert(static_cast<bool>(scheduleGC), "scheduleGC cannot be empty");
        RuntimeAssert(!static_cast<bool>(scheduleGC_), "scheduleGC must not have been set");
        scheduleGC_ = std::move(scheduleGC);
    }

private:
    gc::GCSchedulerConfig& config_;
    CurrentTimeCallback currentTimeCallbackNs_;

    std::atomic<uint64_t> timeOfLastGcNs_;
    std::function<void()> scheduleGC_;
};

} // namespace

KStdUniquePtr<gc::GCSchedulerData> gc::internal::MakeGCSchedulerDataWithTimer(GCSchedulerConfig& config) noexcept {
    return ::make_unique<GCSchedulerDataWithTimer>(config);
}

KStdUniquePtr<gc::GCSchedulerData> gc::internal::MakeGCSchedulerDataWithoutTimer(
        GCSchedulerConfig& config, std::function<uint64_t()> currentTimeCallbackNs) noexcept {
    return ::make_unique<GCSchedulerDataWithoutTimer>(config, std::move(currentTimeCallbackNs));
}

KStdUniquePtr<gc::GCSchedulerData> gc::internal::MakeGCSchedulerData(GCSchedulerConfig& config) noexcept {
    if (internal::useGCTimer()) {
        return MakeGCSchedulerDataWithTimer(config);
    } else {
        return MakeGCSchedulerDataWithoutTimer(config, []() { return konan::getTimeNanos(); });
    }
}