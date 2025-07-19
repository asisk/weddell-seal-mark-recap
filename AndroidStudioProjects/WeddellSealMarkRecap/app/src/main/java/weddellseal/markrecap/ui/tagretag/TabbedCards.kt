package weddellseal.markrecap.ui.tagretag

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.domain.tagretag.data.SealType
import weddellseal.markrecap.ui.tagretag.dialogs.RemoveDialog

/**
 * TabbedCards responds to changes in the model for each seal.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabbedCards(
    viewModel: TagRetagModel,
    primarySeal: Seal,
    pupOneSeal: Seal,
    pupTwoSeal: Seal
) {
    val uiState by viewModel.uiState.collectAsState()

    val showDeleteDialog = remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var tabItems by remember {
        mutableStateOf(
            createTabItems(
                viewModel,
                primarySeal,
                pupOneSeal,
                pupTwoSeal,
            )
        )
    }

    // TODO, revisit whether this is necessary
    // Render the tabs list based on changes with number of relatives or pups started
    LaunchedEffect(
        primarySeal,
        pupOneSeal,
        pupTwoSeal
    ) {
        tabItems = createTabItems(
            viewModel,
            primarySeal,
            pupOneSeal,
            pupTwoSeal
        )

        // Ensure selectedTabIndex is within bounds after updating the list
        if (selectedTabIndex >= tabItems.size) {
            selectedTabIndex = tabItems.lastIndex.coerceAtLeast(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        val selectedSeal = tabItems[selectedTabIndex].seal

        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabItems.forEachIndexed { index, tabItem ->
                Tab(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                tabItem.title,
                                style = MaterialTheme.typography.headlineLarge,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            if (uiState.isSaveAttempted) {
                                val iconColor =
                                    if (tabItem.seal.isValid) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                val icon =
                                    if (tabItem.seal.isValid) Icons.Default.Check else Icons.Default.Error
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "Seal Valid",
                                    tint = iconColor,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    },
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .border(
                    border = BorderStroke(
                        width = 2.dp,
                        color = Color.LightGray // Use a solid color for the border
                    ),
                    shape = RoundedCornerShape(8.dp) // Add rounded corners here
                )
        ) {
            if (uiState.isSaveAttempted) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .pointerInput(Unit) {
                            detectTapGestures { /* consume touch events */ }
                        }
                        .zIndex(1f) // Force it to render above everything
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(0f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // NOTEBOOK STRING
                    Text(
                        selectedSeal.notebookDataString,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 20.dp, top = 10.dp),
                    )

                    // SPENO
                    // If the Tag Event Type is New, we don't display the Speno until the validation step
                    val shouldShowSpeno = if (selectedSeal.tagEventType == "New" && !uiState.isSaveAttempted) false else true
                    val spenoText = if (selectedSeal.hasWedCheckMatch) {
                        "Speno: ${selectedSeal.wedCheckMatch?.speNo}"
                    } else {
                        ""
                    }

                    if (shouldShowSpeno) {
                        Text(
                            text = spenoText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }

                    if (selectedSeal.wedCheckMatch?.comment?.isNotBlank() == true) {
                        // WEDCHECK COMMENT
                        Card(
                            modifier = Modifier.padding(top = 10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFE0B2),
                            ),
                        ) {
                            Text(
                                text = selectedSeal.wedCheckMatch.comment,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(8.dp),
                                color = Color(0xFFF57C00)
                            )
                        }
                    }

                    // TRASH CAN
                    IconButton(
                        modifier = Modifier.padding(10.dp),
                        onClick = { showDeleteDialog.value = true },
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove Tab",
                            tint = if (!primarySeal.isEntryStarted) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }

                // CONTENT
                if (tabItems.isNotEmpty()) {
                    tabItems[selectedTabIndex].content()
                }

                // DELETE DIALOG
                if (showDeleteDialog.value) {
                    val deleteMessage = if (selectedSeal.sealType == SealType.PRIMARY) {
                        "This will remove data you've entered for all seals. Are you sure?"
                    } else {
                        "This will remove data you've entered for $selectedSeal.name. Are you sure?"
                    }

                    RemoveDialog(
                        onDismissRequest = { showDeleteDialog.value = false },
                        onConfirmation = {
                            if (tabItems.isNotEmpty()) {
                                // remove the current seal
                                viewModel.resetSeal(selectedSeal.sealType)
                                showDeleteDialog.value = false
                            }
                        },
                        text = deleteMessage,
                        buttonText = "Yes, clear data."
                    )
                }
            }
        }
    }
}

fun createTabItems(
    viewModel: TagRetagModel,
    primarySealState: Seal,
    pupOneSealState: Seal,
    pupTwoSealState: Seal,
): List<TabItem> {
    val items = mutableListOf<TabItem>()

    items.add(TabItem(SealType.PRIMARY.label, primarySealState) {
        SealCard(
            viewModel,
            primarySealState
        )
    })

    if (primarySealState.hasPupOne) {
        items.add(TabItem(SealType.PUPONE.label, pupOneSealState) {
            SealCard(
                viewModel,
                pupOneSealState
            )
        })
    }

    if (primarySealState.hasPupTwo) {
        items.add(TabItem(SealType.PUPTWO.label, pupTwoSealState) {
            SealCard(
                viewModel,
                pupTwoSealState
            )
        })
    }

    return items
}