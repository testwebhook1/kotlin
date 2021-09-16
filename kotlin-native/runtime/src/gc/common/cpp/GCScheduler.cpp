/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCScheduler.hpp"

#include "CompilerConstants.hpp"
#include "KAssert.h"

using namespace kotlin;

gc::GCScheduler::GCData::GCData(gc::GCSchedulerConfig& config, CurrentTimeCallback currentTimeCallbackNs) noexcept :
    config_(config), currentTimeCallbackNs_(std::move(currentTimeCallbackNs)), timeOfLastGcNs_(currentTimeCallbackNs_()) {}

void gc::GCScheduler::GCData::OnSafePoint(ThreadData& threadData) noexcept {
    size_t allocatedBytes = threadData.allocatedBytes();
    if (allocatedBytes > config_.allocationThresholdBytes || currentTimeCallbackNs_() - timeOfLastGcNs_ >= config_.cooldownThresholdNs) {
        RuntimeAssert(static_cast<bool>(scheduleGC_), "scheduleGC_ cannot be empty");
        scheduleGC_();
    }
}

void gc::GCScheduler::GCData::SetScheduleGC(std::function<void()> scheduleGC) noexcept {
    RuntimeAssert(static_cast<bool>(scheduleGC), "scheduleGC cannot be empty");
    RuntimeAssert(!static_cast<bool>(scheduleGC_), "scheduleGC must not have been set");
    scheduleGC_ = std::move(scheduleGC);
    timer_ = ::make_unique<RepeatedTimer>(std::chrono::microseconds(config_.regularGcIntervalUs), [this]() {
        OnTimer();
        return std::chrono::microseconds(config_.regularGcIntervalUs);
    });
}

void gc::GCScheduler::GCData::OnTimer() noexcept {
    // TODO: Probably makes sense to check memory usage of the process.
    scheduleGC_();
}

void gc::GCScheduler::GCData::OnPerformFullGC() noexcept {
    timeOfLastGcNs_ = currentTimeCallbackNs_();
}
