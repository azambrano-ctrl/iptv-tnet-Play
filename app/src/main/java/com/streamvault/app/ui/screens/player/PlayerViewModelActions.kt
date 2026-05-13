package com.streamvault.app.ui.screens.player

import androidx.lifecycle.viewModelScope
import com.streamvault.domain.model.ContentType
import com.streamvault.domain.model.RecordingRecurrence
import com.streamvault.domain.model.RecordingRequest
import com.streamvault.domain.model.Result
import com.streamvault.domain.model.StreamInfo
import com.streamvault.domain.model.StreamType
import com.streamvault.domain.usecase.ScheduleRecordingCommand
import kotlinx.coroutines.launch

fun PlayerViewModel.startManualRecording() {
    val channel = currentChannel.value
    if (currentContentType != ContentType.LIVE || channel == null || currentProviderId <= 0) {
        showPlayerNotice(message = "Recording needs a valid live channel context.")
        return
    }
    viewModelScope.launch {
        val now = System.currentTimeMillis()
        val result = recordingManager.startManualRecording(
            RecordingRequest(
                providerId = currentProviderId,
                channelId = channel.id,
                channelName = channel.name,
                streamUrl = currentStreamUrl,
                scheduledStartMs = now,
                scheduledEndMs = currentProgram.value?.endTime ?: (now + 30 * 60_000L),
                programTitle = currentProgram.value?.title
            )
        )
        if (result is Result.Error) {
            showPlayerNotice(message = result.message, recoveryType = PlayerRecoveryType.SOURCE)
        } else {
            showPlayerNotice(message = "Recording started for ${channel.name}.")
        }
    }
}

fun PlayerViewModel.scheduleRecording() {
    scheduleRecordingInternal(RecordingRecurrence.NONE)
}

fun PlayerViewModel.scheduleDailyRecording() {
    scheduleRecordingInternal(RecordingRecurrence.DAILY)
}

fun PlayerViewModel.scheduleWeeklyRecording() {
    scheduleRecordingInternal(RecordingRecurrence.WEEKLY)
}

private fun PlayerViewModel.scheduleRecordingInternal(recurrence: RecordingRecurrence) {
    viewModelScope.launch {
        val result = scheduleRecordingUseCase(
            ScheduleRecordingCommand(
                contentType = currentContentType,
                providerId = currentProviderId,
                channel = currentChannel.value,
                streamUrl = currentStreamUrl,
                currentProgram = currentProgram.value,
                nextProgram = nextProgram.value,
                recurrence = recurrence
            )
        )
        if (result is Result.Error) {
            showPlayerNotice(message = result.message, recoveryType = PlayerRecoveryType.SOURCE)
        } else {
            val recurrenceLabel = when (recurrence) {
                RecordingRecurrence.NONE -> ""
                RecordingRecurrence.DAILY -> " daily"
                RecordingRecurrence.WEEKLY -> " weekly"
            }
            val scheduledItem = (result as? Result.Success)?.data
            val title = scheduledItem?.programTitle ?: "Recording"
            showPlayerNotice(message = "$title scheduled$recurrenceLabel.")
        }
    }
}

fun PlayerViewModel.stopCurrentRecording() {
    val recording = currentChannelRecording.value ?: return
    viewModelScope.launch {
        val result = recordingManager.stopRecording(recording.id)
        if (result is Result.Error) {
            showPlayerNotice(message = result.message)
        } else {
            showPlayerNotice(message = "Recording stopped.")
        }
    }
}

