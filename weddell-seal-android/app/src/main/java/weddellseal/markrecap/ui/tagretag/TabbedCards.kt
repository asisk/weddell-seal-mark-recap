package weddellseal.markrecap.ui.tagretag

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import weddellseal.markrecap.R
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.domain.tagretag.data.SealType
import weddellseal.markrecap.domain.tagretag.data.TagEventType
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.tagretag.dialogs.RemoveDialog

/**
 * TabbedCards responds to changes in the model for each seal.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabbedCards(
    viewModel: TagRetagViewModel,
    homeViewModel: HomeViewModel,
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
                homeViewModel,
                primarySeal,
                pupOneSeal,
                pupTwoSeal,
            )
        )
    }

    // Update the tab items when the seals change
    LaunchedEffect(
        primarySeal,
        pupOneSeal,
        pupTwoSeal
    ) {
        tabItems = createTabItems(
            viewModel,
            homeViewModel,
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
                                Icon(
                                    painter = painterResource(
                                        if (tabItem.seal.isValid) R.drawable.ic_check else R.drawable.ic_error_outline,
                                    ),
                                    contentDescription = "Seal Valid",
                                    tint = iconColor,
                                    modifier = Modifier.size(32.dp),
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
                    .padding(start = 12.dp, end = 12.dp)
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
                    val shouldShowSpeno =
                        if (selectedSeal.tagEventType == TagEventType.NEW && !uiState.isSaveAttempted) false else true
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

                    // if is edit mode, only show the trash can for pups
                    // if not edit mode, show the trash can for all seals if the primary seal is started
                    val showTrashCan =
                        when {
                            uiState.isEditMode -> selectedSeal.sealType != SealType.PRIMARY
                            else -> primarySeal.isEntryStarted
                        }

                    if (showTrashCan) {
                        // TRASH CAN
                        IconButton(
                            modifier = Modifier.padding(10.dp),
                            onClick = { showDeleteDialog.value = true },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete_outline),
                                contentDescription = "Remove Tab",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(48.dp),
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }

                // CONTENT
                if (tabItems.isNotEmpty()) {
                    tabItems[selectedTabIndex].content()
                }

                // DELETE DIALOG
                if (showDeleteDialog.value) {
                    val deleteMessage = if (selectedSeal.sealType == SealType.PRIMARY) {
                        "This will remove data you've entered for all seals."
                    } else {
                        "This will remove data you've entered for\n${selectedSeal.notebookDataString}."
                    }

                    val buttonText = if (selectedSeal.sealType != SealType.PRIMARY) {
                        "Yes, remove pup."
                    } else {
                        "Yes, clear all data."
                    }

                    RemoveDialog(
                        onDismissRequest = { showDeleteDialog.value = false },
                        onConfirmation = {
                            if (tabItems.isNotEmpty()) {
                                // remove the seal
                                if (uiState.isEditMode &&
                                    (selectedSeal.sealType == SealType.PUPONE || selectedSeal.sealType == SealType.PUPTWO)
                                ) {
                                    viewModel.markPupRemoved(selectedSeal.sealType)
                                } else {
                                    viewModel.resetSeal(selectedSeal.sealType)
                                }

                                showDeleteDialog.value = false
                            }
                        },
                        text = deleteMessage,
                        buttonText = buttonText
                    )
                }
            }
        }
    }
}

fun createTabItems(
    viewModel: TagRetagViewModel,
    homeViewModel: HomeViewModel,
    primarySealState: Seal,
    pupOneSealState: Seal,
    pupTwoSealState: Seal,
): List<TabItem> {
    val items = mutableListOf<TabItem>()

    items.add(TabItem(SealType.PRIMARY.label, primarySealState) {
        SealCard(
            viewModel,
            homeViewModel,
            primarySealState
        )
    })

    if (primarySealState.hasPupOne && !pupOneSealState.markedRemoved) {
        items.add(TabItem(SealType.PUPONE.label, pupOneSealState) {
            SealCard(
                viewModel,
                homeViewModel,
                pupOneSealState
            )
        })
    }

    if (primarySealState.hasPupTwo && !pupTwoSealState.markedRemoved) {
        items.add(TabItem(SealType.PUPTWO.label, pupTwoSealState) {
            SealCard(
                viewModel,
                homeViewModel,
                pupTwoSealState
            )
        })
    }

    return items
}