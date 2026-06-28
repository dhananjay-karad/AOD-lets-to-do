package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AodViewModel(
    application: Application,
    private val repository: AodRepository
) : AndroidViewModel(application) {

    // UI state for all available lists
    val lists: StateFlow<List<AodList>> = repository.allLists
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current pinned list
    val pinnedList: StateFlow<AodList?> = repository.pinnedList
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Active list ID being edited/viewed in the editor panel
    private val _selectedListId = MutableStateFlow<Int?>(null)
    val selectedListId: StateFlow<Int?> = _selectedListId.asStateFlow()

    // Tasks for the selected list in editor panel
    val selectedListTasks: StateFlow<List<AodTask>> = _selectedListId
        .flatMapLatest { listId ->
            if (listId != null) {
                repository.getTasksForList(listId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Tasks for the pinned list (which is shown on the AOD screen)
    val pinnedListTasks: StateFlow<List<AodTask>> = pinnedList
        .flatMapLatest { list ->
            if (list != null) {
                repository.getTasksForList(list.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // AOD screen active state
    private val _isAodActive = MutableStateFlow(false)
    val isAodActive: StateFlow<Boolean> = _isAodActive.asStateFlow()

    // Screen brightness parameter (specifically used for AOD low light simulation)
    private val _aodBrightness = MutableStateFlow(0.35f)
    val aodBrightness: StateFlow<Float> = _aodBrightness.asStateFlow()

    // Show completed tasks option
    private val _showCompletedInAod = MutableStateFlow(true)
    val showCompletedInAod: StateFlow<Boolean> = _showCompletedInAod.asStateFlow()

    init {
        // Automatically insert a default list if none exist
        viewModelScope.launch {
            lists.collect { listData ->
                if (listData.isEmpty()) {
                    createDefaultList()
                } else if (_selectedListId.value == null) {
                    // Pre-select the pinned list or first list
                    val pinned = listData.find { it.isPinned }
                    _selectedListId.value = pinned?.id ?: listData.firstOrNull()?.id
                }
            }
        }
    }

    private suspend fun createDefaultList() {
        val listId = repository.insertList(
            AodList(title = "🚀 Daily Rituals", isPinned = true)
        ).toInt()

        repository.insertTask(AodTask(listId = listId, title = "Drink a large glass of water", priority = 1))
        repository.insertTask(AodTask(listId = listId, title = "Take deep breaths & stretch (5 min)", priority = 2))
        repository.insertTask(AodTask(listId = listId, title = "Check calendar & set main target", priority = 2))
        repository.insertTask(AodTask(listId = listId, title = "Walk outdoors (10 min)", priority = 0))
        repository.insertTask(AodTask(listId = listId, title = "Write 3 things to accomplish", priority = 1))

        // Create a secondary list as well to show multi-list support
        val secondListId = repository.insertList(
            AodList(title = "🛒 Quick Groceries", isPinned = false)
        ).toInt()

        repository.insertTask(AodTask(listId = secondListId, title = "Organic bananas", priority = 1))
        repository.insertTask(AodTask(listId = secondListId, title = "Almond milk & dark chocolate", priority = 2))
        repository.insertTask(AodTask(listId = secondListId, title = "Fresh spinach & garlic", priority = 0))

        _selectedListId.value = listId
    }

    fun selectList(listId: Int) {
        _selectedListId.value = listId
    }

    fun createNewList(title: String) {
        viewModelScope.launch {
            val listId = repository.insertList(AodList(title = title))
            _selectedListId.value = listId.toInt()
        }
    }

    fun updateListTitle(list: AodList, newTitle: String) {
        viewModelScope.launch {
            repository.updateList(list.copy(title = newTitle))
        }
    }

    fun deleteList(list: AodList) {
        viewModelScope.launch {
            repository.deleteList(list)
            if (_selectedListId.value == list.id) {
                _selectedListId.value = lists.value.firstOrNull { it.id != list.id }?.id
            }
        }
    }

    fun pinList(listId: Int) {
        viewModelScope.launch {
            repository.pinList(listId)
        }
    }

    fun addTask(title: String, priority: Int) {
        val currentListId = _selectedListId.value ?: return
        viewModelScope.launch {
            repository.insertTask(
                AodTask(listId = currentListId, title = title, priority = priority)
            )
        }
    }

    fun toggleTaskCompletion(task: AodTask) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: AodTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun updateTaskPriority(task: AodTask, newPriority: Int) {
        viewModelScope.launch {
            repository.updateTask(task.copy(priority = newPriority))
        }
    }

    fun setAodActive(active: Boolean) {
        _isAodActive.value = active
    }

    fun setAodBrightness(brightness: Float) {
        _aodBrightness.value = brightness.coerceIn(0.05f, 1.0f)
    }

    fun toggleShowCompletedInAod() {
        _showCompletedInAod.value = !_showCompletedInAod.value
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AodViewModel::class.java)) {
                val database = AodDatabase.getDatabase(application)
                val repository = AodRepository(database.aodDao())
                @Suppress("UNCHECKED_CAST")
                return AodViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
