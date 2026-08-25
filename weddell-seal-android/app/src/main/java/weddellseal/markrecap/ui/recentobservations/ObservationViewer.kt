package weddellseal.markrecap.ui.recentobservations

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import weddellseal.markrecap.R
import weddellseal.markrecap.ui.tagretag.TagRetagViewModel
import weddellseal.markrecap.ui.utils.scaffoldContentInsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObservationViewer(
    navController: NavHostController,
    tagRetagViewModel: TagRetagViewModel
) {

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Text(
                        text = "Tag/Retag Viewer",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 36.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigateUp()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_ios_new),
                            contentDescription = "Back",
                            modifier = Modifier.size(48.dp)
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .scaffoldContentInsets(innerPadding)
        ) {
            ObservationTabbedCards(tagRetagViewModel)
        }
    }
}
