package weddellseal.markrecap.ui.tagretag

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.models.TagRetagModel
import weddellseal.markrecap.models.WedCheckViewModel

data class TabItem(
    val title: String,
    val sealName: String,
    val content: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabbedCards(
    viewModel: TagRetagModel,
    wedCheckViewModel: WedCheckViewModel
) {
    val showDeleteDialog = remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var tabItems by remember {
        mutableStateOf(
            createTabItems(viewModel, wedCheckViewModel)
        )
    }

    // Render the tabs list based on changes with number of relatives or pups started
    LaunchedEffect(
        viewModel.primarySeal.numRelatives,
        viewModel.pupOne.isStarted,
        viewModel.pupTwo.isStarted
    ) {
        tabItems = createTabItems(viewModel, wedCheckViewModel)

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
                        Text(
                            tabItem.title, style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.padding(horizontal = 20.dp) // Adjust padding here
                        )
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
                            viewModel.resetSeal(tabItems[selectedTabIndex].sealName)
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
    wedCheckViewModel: WedCheckViewModel
): List<TabItem> {
    val items = mutableListOf<TabItem>()
    items.add(TabItem("Seal", viewModel.primarySeal.name) {
        SealCard(
            viewModel,
            viewModel.primarySeal,
            wedCheckViewModel
        )
    })

    if (viewModel.pupOne.isStarted) {
        items.add(TabItem("Pup One", viewModel.pupOne.name) {
            SealCard(
                viewModel,
                viewModel.pupOne,
                wedCheckViewModel
            )
        })
    }

    if (viewModel.pupTwo.isStarted) {
        items.add(TabItem("Pup Two", viewModel.pupTwo.name) {
            SealCard(
                viewModel,
                viewModel.pupTwo,
                wedCheckViewModel
            )
        })
    }

    return items
}
