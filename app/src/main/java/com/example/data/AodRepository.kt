package com.example.data

import kotlinx.coroutines.flow.Flow

class AodRepository(private val aodDao: AodDao) {
    val allLists: Flow<List<AodList>> = aodDao.getAllLists()
    val pinnedList: Flow<AodList?> = aodDao.getPinnedListFlow()

    fun getTasksForList(listId: Int): Flow<List<AodTask>> = aodDao.getTasksForList(listId)

    suspend fun insertList(list: AodList): Long = aodDao.insertList(list)

    suspend fun updateList(list: AodList) = aodDao.updateList(list)

    suspend fun deleteList(list: AodList) = aodDao.deleteList(list)

    suspend fun insertTask(task: AodTask): Long = aodDao.insertTask(task)

    suspend fun updateTask(task: AodTask) = aodDao.updateTask(task)

    suspend fun deleteTask(task: AodTask) = aodDao.deleteTask(task)

    suspend fun pinList(listId: Int) = aodDao.pinList(listId)
}
