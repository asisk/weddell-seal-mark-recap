package weddellseal.markrecap.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import weddellseal.markrecap.models.HomeViewModel

data class SelectableItem(val title: String, var isSelected: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectDropdownObservers(
    viewModel: HomeViewModel,
    selectedOptions: String,
    onValueChange: (List<String>) -> Unit
) {
    // Observe the observers list from the ViewModel
    val options by viewModel.observers.collectAsState() // Collecting the list of observers
    var expanded by remember { mutableStateOf(false) }

    var selectedItems by remember {
        mutableStateOf(options.map { option ->
            SelectableItem(option, option in selectedOptions)
        })
    }

    // Fetch observers only if the list is empty or data is considered stale
    LaunchedEffect(Unit) {
        if (viewModel.observers.value.isEmpty()) {
            viewModel.fetchObservers()
        }
    }

    LaunchedEffect(options) {
        selectedItems = options.map { option ->
            SelectableItem(option, option in selectedOptions)
        }
    }

    // Function to update item selection
    fun updateItemSelection(item: SelectableItem, isSelected: Boolean) {
        val updatedItems = selectedItems.map {
            if (it.title == item.title) it.copy(isSelected = isSelected) else it
        }
        selectedItems = updatedItems

        // Convert selected items back to a list of strings
        onValueChange(updatedItems.filter { it.isSelected }.map { it.title })
    }

    // Display selected items as a comma-separated string
    val selectedText = selectedItems.filter { it.isSelected }.joinToString { it.title }

    Column {
        // Dropdown trigger
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                readOnly = true,
                value = selectedText.ifEmpty { "Select items" },
                onValueChange = {},
                label = { Text("Select items") },
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            // Dropdown menu items with checkboxes
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                selectedItems.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Row {
                                Checkbox(
                                    checked = item.isSelected,
                                    onCheckedChange = { isSelected ->
                                        updateItemSelection(item, isSelected)
                                    }
                                )
                                Text(text = item.title)
                            }
                        },
                        onClick = {
                            updateItemSelection(item, !item.isSelected)
                        }
                    )
                }
            }
        }
    }
}
