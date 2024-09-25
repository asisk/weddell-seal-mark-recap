package weddellseal.markrecap.ui.components

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
fun MultiSelectDropdown(
    items: List<SelectableItem>,
    label: String = "Select items",
    onSelectionChange: (List<SelectableItem>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf(items) }

    // Function to update item selection
    fun updateItemSelection(item: SelectableItem, isSelected: Boolean) {
        val updatedItems = selectedItems.map {
            if (it.title == item.title) it.copy(isSelected = isSelected) else it
        }
        selectedItems = updatedItems
        onSelectionChange(updatedItems)
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
                value = if (selectedText.isEmpty()) label else selectedText,
                onValueChange = {},
                label = { Text(label) },
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

            // Dropdown menu with checkboxes
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

@Composable
fun MultiSelectDropdownObservers(
    viewModel: HomeViewModel,
    options: List<String>,
    selectedOptions: String,
    onValueChange: (List<String>) -> Unit
) {

    LaunchedEffect(Unit) {
        viewModel.fetchObservers()
    }

    // Initialize items with selection based on provided selectedOptions
    var items by remember {
        mutableStateOf(options.map { option ->
            SelectableItem(option, option in selectedOptions)
        })
    }

    LaunchedEffect(options) {
        items = options.map { option ->
            SelectableItem(option, option in selectedOptions)
        }
    }

    MultiSelectDropdown(
        items = items,
        label = "Select items"
    ) { updatedItems ->
        items = updatedItems
        // Convert selected items back to a list of strings
        onValueChange(updatedItems.filter { it.isSelected }.map { it.title })
    }
}
