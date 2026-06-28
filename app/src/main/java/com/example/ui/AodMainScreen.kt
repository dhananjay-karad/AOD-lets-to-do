package com.example.ui

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AodList
import com.example.data.AodTask
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

@Composable
fun AodMainScreen(viewModel: AodViewModel) {
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val pinnedList by viewModel.pinnedList.collectAsStateWithLifecycle()
    val selectedListId by viewModel.selectedListId.collectAsStateWithLifecycle()
    val selectedListTasks by viewModel.selectedListTasks.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Configuration & Editing Mode
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Minimalist Header Section
            TaskListHeader()

            // Content Area - Responsive side-by-side or stacked layout
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val isWide = maxWidth > 600.dp
                if (isWide) {
                    // Expanded screen (Tablets/Landscape) -> Side-by-side layout
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(modifier = Modifier.weight(0.4f)) {
                            ListManagementPanel(
                                lists = lists,
                                pinnedList = pinnedList,
                                selectedListId = selectedListId,
                                onSelectList = { viewModel.selectList(it) },
                                onPinList = { viewModel.pinList(it) },
                                onCreateList = { viewModel.createNewList(it) },
                                onDeleteList = { viewModel.deleteList(it) }
                            )
                        }

                        Box(modifier = Modifier.weight(0.6f)) {
                            TaskEditorPanel(
                                lists = lists,
                                selectedListId = selectedListId,
                                tasks = selectedListTasks,
                                onAddTask = { title, priority -> viewModel.addTask(title, priority) },
                                onToggleTask = { viewModel.toggleTaskCompletion(it) },
                                onDeleteTask = { viewModel.deleteTask(it) },
                                onUpdatePriority = { task, prio -> viewModel.updateTaskPriority(task, prio) }
                            )
                        }
                    }
                } else {
                    // Compact screen (Mobile portrait) -> Stacked layout with scrollable content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Quick lists row selector
                        HorizontalListSelector(
                            lists = lists,
                            pinnedList = pinnedList,
                            selectedListId = selectedListId,
                            onSelectList = { viewModel.selectList(it) },
                            onPinList = { viewModel.pinList(it) },
                            onCreateList = { viewModel.createNewList(it) },
                            onDeleteList = { viewModel.deleteList(it) }
                        )

                        // Active Task Editor
                        Box(modifier = Modifier.weight(1f)) {
                            TaskEditorPanel(
                                lists = lists,
                                selectedListId = selectedListId,
                                tasks = selectedListTasks,
                                onAddTask = { title, priority -> viewModel.addTask(title, priority) },
                                onToggleTask = { viewModel.toggleTaskCompletion(it) },
                                onDeleteTask = { viewModel.deleteTask(it) },
                                onUpdatePriority = { task, prio -> viewModel.updateTaskPriority(task, prio) }
                            )
                        }
                    }
                }
            }

        }
    }
}

// ==========================================
// COMPONENT 1: HEADER
// ==========================================
@Composable
fun TaskListHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Minimalist Tasks",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap any list to sync with your home screen widget",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==========================================
// COMPONENT 2: LIST SELECTOR FOR MOBILE
// ==========================================
@Composable
fun HorizontalListSelector(
    lists: List<AodList>,
    pinnedList: AodList?,
    selectedListId: Int?,
    onSelectList: (Int) -> Unit,
    onPinList: (Int) -> Unit,
    onCreateList: (String) -> Unit,
    onDeleteList: (AodList) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Task Lists",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create List", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New List")
            }
        }

        if (lists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(lists) { list ->
                    val isSelected = list.id == selectedListId
                    val isPinned = list.id == pinnedList?.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectList(list.id) }
                            .testTag("list_item_${list.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = list.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (isPinned) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Pinned to AOD",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "AOD PIN",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onPinList(list.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Pin to Always-on screen",
                                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                if (lists.size > 1) {
                                    IconButton(
                                        onClick = { onDeleteList(list) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete list",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateListDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title ->
                onCreateList(title)
                showCreateDialog = false
            }
        )
    }
}

// ==========================================
// COMPONENT 3: TABLET LIST PANEL
// ==========================================
@Composable
fun ListManagementPanel(
    lists: List<AodList>,
    pinnedList: AodList?,
    selectedListId: Int?,
    onSelectList: (Int) -> Unit,
    onPinList: (Int) -> Unit,
    onCreateList: (String) -> Unit,
    onDeleteList: (AodList) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Task Lists",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Create List")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (lists.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(lists) { list ->
                        val isSelected = list.id == selectedListId
                        val isPinned = list.id == pinnedList?.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectList(list.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 1.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = list.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (isPinned) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Pinned to AOD",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Pinned on Always-On Screen",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { onPinList(list.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Pin to AOD",
                                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }

                                    if (lists.size > 1) {
                                        IconButton(onClick = { onDeleteList(list) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete List",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateListDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title ->
                onCreateList(title)
                showCreateDialog = false
            }
        )
    }
}

// ==========================================
// COMPONENT 4: TASK EDITOR PANEL
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorPanel(
    lists: List<AodList>,
    selectedListId: Int?,
    tasks: List<AodTask>,
    onAddTask: (String, Int) -> Unit,
    onToggleTask: (AodTask) -> Unit,
    onDeleteTask: (AodTask) -> Unit,
    onUpdatePriority: (AodTask, Int) -> Unit
) {
    val currentList = lists.find { it.id == selectedListId }
    var newTaskText by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableIntStateOf(1) } // Default Medium

    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Panel Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentList?.title ?: "Tasks List",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${tasks.size} Items",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Task Creation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = newTaskText,
                    onValueChange = { newTaskText = it },
                    placeholder = { Text("➕ Add quick task...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_task_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        disabledContainerColor = MaterialTheme.colorScheme.background,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    trailingIcon = {
                        if (newTaskText.isNotEmpty()) {
                            IconButton(onClick = { newTaskText = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear text")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Priority Selection Toggle Buttons
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.background,
                                RoundedCornerShape(8.dp)
                            )
                            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .padding(2.dp)
                    ) {
                        listOf(0, 1, 2).forEach { priorityLevel ->
                            val color = when (priorityLevel) {
                                0 -> ElectricTeal
                                1 -> AmberNeon
                                else -> CyberRed
                            }
                            val isSelected = selectedPriority == priorityLevel
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) color.copy(alpha = 0.25f) else Color.Transparent)
                                    .clickable { selectedPriority = priorityLevel }
                                    .testTag("priority_selector_$priorityLevel"),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(8.dp)) {
                                    drawCircle(color = color)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (newTaskText.isNotBlank()) {
                            onAddTask(newTaskText.trim(), selectedPriority)
                            newTaskText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        .testTag("submit_task_button"),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Submit task")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tasks List
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Empty Tasks",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No tasks in this list yet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add tasks above to begin tracking them.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        val priorityColor = when (task.priority) {
                            0 -> ElectricTeal
                            1 -> AmberNeon
                            else -> CyberRed
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_card_${task.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.background
                            ),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Custom Tappable Checkbox with spring scale animation
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, priorityColor, CircleShape)
                                            .background(if (task.isCompleted) priorityColor else Color.Transparent)
                                            .clickable { onToggleTask(task) }
                                            .testTag("task_checkbox_${task.id}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (task.isCompleted) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Task Done",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground,
                                        fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Medium
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Cycle priority button
                                    IconButton(
                                        onClick = {
                                            val nextPriority = (task.priority + 1) % 3
                                            onUpdatePriority(task, nextPriority)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(priorityColor)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteTask(task) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete task",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT 5: CREATE LIST DIALOG
// ==========================================
@Composable
fun CreateListDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Create New List",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column {
                Text(
                    "Provide a short title for your task group:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Work targets, Travel prep...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_list_title_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onConfirm(title.trim()) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("create_list_confirm_button")
            ) {
                Text("Create", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

// ==========================================
// COMPONENT 6: INSTRUCTIONS FOOTER
// ==========================================
@Composable
fun InstructionsFooter() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Instructions",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "🔒 Always-On Lock Screen Instructions",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "1. Pinned lists with the 📌 icon will automatically sync with AOD Overlay mode.\n" +
                       "2. When AOD is active, the app displays over the system Lock Screen. Wake your phone to view instantly!\n" +
                       "3. Complete tasks directly from AOD, double tap the background, or swipe up to close.\n" +
                       "4. Burn-in protection is fully integrated and shifts the display pixels dynamically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

// ==========================================
// COMPONENT 7: FULLSCREEN AOD SCREEN OVERLAY
// ==========================================
@Composable
fun AodOverlayScreen(
    pinnedList: AodList?,
    pinnedTasks: List<AodTask>,
    aodBrightness: Float,
    showCompleted: Boolean,
    onClose: () -> Unit,
    onToggleTask: (AodTask) -> Unit,
    onSetBrightness: (Float) -> Unit,
    onToggleShowCompleted: () -> Unit
) {
    // Current time & date ticker
    var timeString by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("E, MMM d", Locale.getDefault())
        while (true) {
            val now = Date()
            timeString = timeFormat.format(now)
            dateString = dateFormat.format(now)
            delay(1000)
        }
    }

    // Dynamic Burn-In Protection Pixel Shifter!
    // Shifts the visual contents slightly every 20 seconds to protect OLED pixels.
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var lastShiftTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(20000) // 20 seconds
            offsetX = Random.nextInt(-20, 21).toFloat()
            offsetY = Random.nextInt(-20, 21).toFloat()
            lastShiftTime = System.currentTimeMillis()
        }
    }

    // Double-tap to exit handler
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Pitch black background for battery-saving OLED
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 350) {
                            onClose()
                        } else {
                            lastTapTime = now
                        }
                    }
                )
            }
    ) {
        // Dimmer simulation layer (translates AOD brightness setting)
        val dimAlpha = (1f - aodBrightness).coerceIn(0f, 0.95f)

        // OLED Content Layer with Burn-In Protection Offset Shift
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = offsetX.dp, y = offsetY.dp)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Simulated Status Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, bottom = 24.dp)
                        .alpha(0.6f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Carrier",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedGray,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "📶",
                            fontSize = 11.sp,
                            color = MutedGray
                        )
                        Text(
                            text = "🔋 85%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Header Clock and Date Section
                Text(
                    text = timeString.ifEmpty { "10:48" },
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 72.sp,
                        letterSpacing = (-1.5).sp
                    ),
                    color = AmberNeon, // Lavender
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = dateString.ifEmpty { "Tuesday, Oct 24" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = IceWhite,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.alpha(0.8f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .alpha(0.6f)
                ) {
                    Text(
                        text = "⛅ 18°C",
                        style = MaterialTheme.typography.bodySmall,
                        color = IceWhite
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Main Tasks Widget Container
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 340.dp)
                        .padding(horizontal = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MatteCharcoal // #211F26
                    ),
                    border = BorderStroke(1.dp, BorderGray), // #49454F
                    shape = RoundedCornerShape(24.dp) // rounded-3xl
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Card Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pinnedList?.title?.uppercase() ?: "QUICK TASKS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 1.5.sp
                                ),
                                color = AmberNeon, // Lavender
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null,
                                tint = MutedGray,
                                modifier = Modifier
                                    .size(18.dp)
                                    .alpha(0.4f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tasks items
                        val filteredTasks = if (showCompleted) {
                            pinnedTasks
                        } else {
                            pinnedTasks.filter { !it.isCompleted }
                        }

                        if (filteredTasks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✨ All tasks complete!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedGray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(filteredTasks, key = { it.id }) { task ->
                                    val priorityColor = when (task.priority) {
                                        0 -> ElectricTeal
                                        1 -> AmberNeon
                                        else -> CyberRed
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onToggleTask(task) }
                                            .testTag("aod_task_item_${task.id}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Custom Round Checkbox
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, AmberNeon, CircleShape)
                                                .background(if (task.isCompleted) AmberNeon else Color.Transparent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (task.isCompleted) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Done",
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = task.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                                ),
                                                color = if (task.isCompleted) IceWhite.copy(alpha = 0.4f) else IceWhite,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }

                                        // Subtle Priority Indicator
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(priorityColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Bottom Interactive / Guidance Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.alpha(0.6f)
                ) {
                    // Small swipe handle line
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(BorderGray, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "DOUBLE TAP BACKGROUND TO EXIT",
                        style = MaterialTheme.typography.bodySmall.copy(
                            letterSpacing = 1.5.sp,
                            fontSize = 9.sp
                        ),
                        color = MutedGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // PHYSICAL DIMMER OVERLAY
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimAlpha))
                .pointerInput(Unit) {} // Consume clicks so user can't click underneath easily when dimmed too much
        )

        // CONTROL PANEL AT THE VERY BOTTOM OF THE SCREEN
        // It remains interactive so users can toggle dimming and task completion
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF111111)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.5.dp, Color.DarkGray)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "AOD Overlay controls",
                        style = MaterialTheme.typography.labelLarge,
                        color = AmberNeon,
                        fontWeight = FontWeight.Bold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = IceWhite
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = showCompleted,
                            onCheckedChange = { onToggleShowCompleted() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AmberNeon,
                                checkedTrackColor = Color.DarkGray,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.Black
                            ),
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Brightness slider (low-light simulation)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔅",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Slider(
                        value = aodBrightness,
                        onValueChange = onSetBrightness,
                        valueRange = 0.05f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = AmberNeon,
                            activeTrackColor = AmberNeon,
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("aod_brightness_slider")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🔆",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}


