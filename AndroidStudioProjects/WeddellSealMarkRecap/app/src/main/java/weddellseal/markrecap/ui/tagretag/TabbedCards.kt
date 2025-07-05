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
import weddellseal.markrecap.ui.lookup.SealLookupViewModel
import weddellseal.markrecap.ui.tagretag.dialogs.RemoveDialog

/**
 * TabbedCards responds to changes in the model for each seal.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabbedCards(
    viewModel: TagRetagModel,
    sealLookupViewModel: SealLookupViewModel,
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
                sealLookupViewModel,
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
            sealLookupViewModel,
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
                            if (uiState.isSaving) {
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
            if (uiState.isSaving) {
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
                        tabItems[selectedTabIndex].seal.notebookDataString,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 20.dp, top = 10.dp),
                    )

                    if (tabItems[selectedTabIndex].seal.wedCheckMatch != null) {
                        // SPENO
                        Text(
                            text = "Speno: ${tabItems[selectedTabIndex].seal.wedCheckMatch?.speNo}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 10.dp),
                        )

                        if (tabItems[selectedTabIndex].seal.wedCheckMatch?.comment?.isNotBlank() == true) {
                            // WEDCHECK COMMENT
                            Card(
                                modifier = Modifier.padding(top = 10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFE0B2),
                                ),
                            ) {
                                Text(
                                    text = tabItems[selectedTabIndex].seal.wedCheckMatch?.comment
                                        ?: "",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(8.dp),
                                    color = Color(0xFFF57C00)
                                )
                            }
                        }
                    }

                    // TRASH CAN
                    IconButton(
                        modifier = Modifier.padding(8.dp),
                        onClick = { showDeleteDialog.value = true },
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove Tab",
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }

                // CONTENT
                if (tabItems.isNotEmpty()) {
                    tabItems[selectedTabIndex].content()
                }

                // Show the dialog if showDialog is true
                if (showDeleteDialog.value) {
                    RemoveDialog(
                        onDismissRequest = { showDeleteDialog.value = false },
                        onConfirmation = {
                            if (tabItems.isNotEmpty()) {
                                // remove the current seal
                                viewModel.resetSeal(tabItems[selectedTabIndex].seal.name)
                                sealLookupViewModel.resetUiState()
                                sealLookupViewModel.resetLookupSeal()
                                showDeleteDialog.value = false
                            }
                        },
                    )
                }
            }
        }
    }
}

fun createTabItems(
    viewModel: TagRetagModel,
    sealLookupViewModel: SealLookupViewModel,
    primarySealState: Seal,
    pupOneSealState: Seal,
    pupTwoSealState: Seal,
): List<TabItem> {
    val items = mutableListOf<TabItem>()

    items.add(TabItem("Seal", primarySealState) {
        SealCard(
            viewModel,
            SealType.PRIMARY,
            primarySealState,
            sealLookupViewModel
        )
    })

    if (pupOneSealState.isStarted) {
        items.add(TabItem("Pup One", pupOneSealState) {
            SealCard(
                viewModel,
                SealType.PUPONE,
                pupOneSealState,
                sealLookupViewModel
            )
        })
    }

    if (pupTwoSealState.isStarted) {
        items.add(TabItem("Pup Two", pupTwoSealState) {
            SealCard(
                viewModel,
                SealType.PUPTWO,
                pupTwoSealState,
                sealLookupViewModel
            )
        })
    }

    return items
}