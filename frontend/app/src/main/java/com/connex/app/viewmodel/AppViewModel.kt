package com.connex.app.viewmodel

import androidx.lifecycle.ViewModel
import com.connex.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val auth: AuthRepository
) : ViewModel() {
    fun isAuthed(): Boolean = auth.isAuthed()
}
