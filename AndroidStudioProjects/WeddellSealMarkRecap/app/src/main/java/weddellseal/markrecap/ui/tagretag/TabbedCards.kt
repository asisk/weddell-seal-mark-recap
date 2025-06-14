package weddellseal.markrecap.ui.tagretag

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
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
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.domain.tagretag.data.WedCheckSeal
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
            pupTwoSeal,
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

                        val checkMarkColor = if (tabItem.seal.isValid) Color(0xFF4CAF50) else Color.LightGray

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                tabItem.title,
                                style = MaterialTheme.typography.headlineLarge,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Seal Ready for Save",
                                tint = checkMarkColor,
                                modifier = Modifier.size(16.dp)
                            )
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
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        showDeleteDialog.value = true
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove Tab",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // CONTENT
            Column(modifier = Modifier.fillMaxWidth()) {
                if (tabItems.isNotEmpty()) {
                    tabItems[selectedTabIndex].content()
                }
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