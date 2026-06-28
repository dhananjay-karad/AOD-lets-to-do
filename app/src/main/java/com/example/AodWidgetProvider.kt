package com.example

import android.app.PendingIntent
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.data.AodDatabase
import com.example.data.AodTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AodWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_TASK = "com.example.ACTION_TOGGLE_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_COMPLETED = "extra_task_completed"

        // Helper to trigger manual update of all active widgets
        fun triggerUpdate(context: Context) {
            val intent = Intent(context, AodWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, AodWidgetProvider::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Run update in background coroutine
        CoroutineScope(Dispatchers.IO).launch {
            val database = AodDatabase.getDatabase(context)
            val dao = database.aodDao()

            val sharedPrefs = context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE)
            val selectedListId = sharedPrefs.getInt("selected_list_id", -1)

            val allLists = try {
                dao.getAllLists().first()
            } catch (e: Exception) {
                emptyList()
            }

            // Determine which list to display on the widget
            val targetList = if (selectedListId != -1) {
                allLists.find { it.id == selectedListId }
            } else {
                null
            } ?: allLists.find { it.isPinned } ?: allLists.firstOrNull()

            val tasks = if (targetList != null) {
                try {
                    dao.getTasksForList(targetList.id).first()
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, targetList?.title, tasks)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_TASK) {
            val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
            val isCompleted = intent.getBooleanExtra(EXTRA_TASK_COMPLETED, false)

            if (taskId != -1) {
                CoroutineScope(Dispatchers.IO).launch {
                    val database = AodDatabase.getDatabase(context)
                    val dao = database.aodDao()

                    // Retrieve list of all tasks to find this specific task
                    val allTasksFlow = dao.getAllTasks()
                    val allTasks = try {
                        allTasksFlow.first()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    val taskToUpdate = allTasks.find { it.id == taskId }

                    if (taskToUpdate != null) {
                        // Toggle state
                        val updatedTask = taskToUpdate.copy(isCompleted = !isCompleted)
                        dao.updateTask(updatedTask)

                        // Trigger widget UI update
                        triggerUpdate(context)
                    }
                }
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        listTitle: String?,
        tasks: List<AodTask>
    ) {
        val views = RemoteViews(context.packageName, R.layout.aod_widget)

        // Set Widget Title
        if (!listTitle.isNullOrEmpty()) {
            views.setTextViewText(R.id.widget_title, listTitle.uppercase())
        } else {
            views.setTextViewText(R.id.widget_title, "NO PINNED LIST")
        }

        // Set up task rows
        val rowIds = arrayOf(
            R.id.widget_task_row_1,
            R.id.widget_task_row_2,
            R.id.widget_task_row_3,
            R.id.widget_task_row_4,
            R.id.widget_task_row_5
        )

        val checkIds = arrayOf(
            R.id.widget_task_check_1,
            R.id.widget_task_check_2,
            R.id.widget_task_check_3,
            R.id.widget_task_check_4,
            R.id.widget_task_check_5
        )

        val titleIds = arrayOf(
            R.id.widget_task_title_1,
            R.id.widget_task_title_2,
            R.id.widget_task_title_3,
            R.id.widget_task_title_4,
            R.id.widget_task_title_5
        )

        val priorityIds = arrayOf(
            R.id.widget_task_priority_1,
            R.id.widget_task_priority_2,
            R.id.widget_task_priority_3,
            R.id.widget_task_priority_4,
            R.id.widget_task_priority_5
        )

        if (tasks.isEmpty()) {
            views.setViewVisibility(R.id.widget_empty_text, View.VISIBLE)
            views.setViewVisibility(R.id.widget_tasks_container, View.GONE)
        } else {
            views.setViewVisibility(R.id.widget_empty_text, View.GONE)
            views.setViewVisibility(R.id.widget_tasks_container, View.VISIBLE)

            for (i in rowIds.indices) {
                if (i < tasks.size) {
                    val task = tasks[i]
                    views.setViewVisibility(rowIds[i], View.VISIBLE)
                    views.setTextViewText(titleIds[i], task.title)

                    // Text dimming / strikethrough styling emulation via alpha/color
                    if (task.isCompleted) {
                        views.setTextColor(titleIds[i], 0x66F5EFEB.toInt()) // Dimmed Cream
                        views.setImageViewResource(checkIds[i], R.drawable.ic_checkbox_checked)
                    } else {
                        views.setTextColor(titleIds[i], 0xFFF5EFEB.toInt()) // Solid Cream
                        views.setImageViewResource(checkIds[i], R.drawable.ic_checkbox_unchecked)
                    }

                    // Set priority dot drawable
                    val priorityDot = when (task.priority) {
                        0 -> R.drawable.priority_dot_low
                        1 -> R.drawable.priority_dot_medium
                        else -> R.drawable.priority_dot_high
                    }
                    views.setImageViewResource(priorityIds[i], priorityDot)

                    // Create toggle pending intent
                    val toggleIntent = Intent(context, AodWidgetProvider::class.java).apply {
                        action = ACTION_TOGGLE_TASK
                        putExtra(EXTRA_TASK_ID, task.id)
                        putExtra(EXTRA_TASK_COMPLETED, task.isCompleted)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        task.id,
                        toggleIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    views.setOnClickPendingIntent(rowIds[i], pendingIntent)
                } else {
                    views.setViewVisibility(rowIds[i], View.GONE)
                }
            }
        }

        // Add task button launches Main App
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_add_button, mainPendingIntent)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
