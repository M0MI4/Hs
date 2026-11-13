package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTask
import com.example.data.model.ModApp
import com.example.data.repository.DownloadRepository
import com.example.data.repository.FirestoreSyncRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {

    val downloadRepo = DownloadRepository(application.applicationContext)
    val firestoreRepo = FirestoreSyncRepository(application.applicationContext)

    val tasks: StateFlow<List<DownloadTask>> = downloadRepo.tasks

    val activeDownloads: StateFlow<List<DownloadTask>> = tasks
        .map { list ->
            list.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun startDownload(app: ModApp, user: FirebaseUser? = null): String {
        val taskId = downloadRepo.startDownload(app)
        viewModelScope.launch {
            firestoreRepo.incrementAppDownloadCount(app.id)
            if (user != null) {
                try {
                    firestoreRepo.recordDownload(app.id, app.name, app.version)
                } catch (_: Exception) {}
            }
        }
        return taskId
    }

    fun pauseDownload(taskId: String) = downloadRepo.pauseDownload(taskId)

    fun resumeDownload(taskId: String) = downloadRepo.resumeDownload(taskId)

    fun cancelDownload(taskId: String) = downloadRepo.cancelDownload(taskId)

    fun installApk(context: Context, task: DownloadTask): Result<Unit> =
        downloadRepo.installApk(context, task)
}
