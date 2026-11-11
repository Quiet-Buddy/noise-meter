package com.quietbuddy.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietbuddy.core.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private var finishing = false

    /** Marks onboarding as done, then calls [onDone]. Used whatever the permission result was. */
    fun finish(onDone: () -> Unit) {
        if (finishing) return
        finishing = true
        viewModelScope.launch {
            settingsRepository.update { it.copy(onboardingDone = true) }
            onDone()
        }
    }
}
