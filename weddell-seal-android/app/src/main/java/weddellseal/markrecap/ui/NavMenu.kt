package weddellseal.markrecap.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import weddellseal.markrecap.R
import weddellseal.markrecap.Screens

@Composable
fun NavMenu(navController: NavHostController) {
    var adminExpanded by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.6f)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(32.dp))

            NavigationDrawerItem(
                label = { Text("") },
                selected = false,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_home),
                        contentDescription = "Go to Home Screen",
                        modifier = Modifier.size(48.dp),
                    )
                },
                onClick = { navController.navigate(Screens.Home.route) }
            )

            Spacer(Modifier.height(32.dp))

            HorizontalDivider()

            Spacer(Modifier.height(32.dp))

            NavigationDrawerItem(
                label = {
                    Text(
                        "Seal Lookup", style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                selected = false,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = "Go to Seal Lookup Screen",
                        modifier = Modifier.size(36.dp),
                    )
                },
                onClick = { navController.navigate(Screens.SealLookupScreen.route) }
            )

            Spacer(Modifier.height(24.dp))

            NavigationDrawerItem(
                label = {
                    Text(
                        "Tag/Retag",
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                selected = false,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_post_add),
                        contentDescription = "Go to Tag/Retag Screen",
                        modifier = Modifier.size(36.dp)
                    )
                },
                onClick = { navController.navigate(Screens.TagRetag.route) }
            )

            Spacer(Modifier.height(24.dp))

            NavigationDrawerItem(
                label = {
                    Text(
                        "Census", style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                selected = false,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_checklist),
                        contentDescription = "Enter Census Mode",
                        modifier = Modifier.size(36.dp)
                    )
                },
                onClick = { navController.navigate(Screens.Census.route) }
            )

            Spacer(Modifier.height(24.dp))


            NavigationDrawerItem(
                label = {
                    Text(
                        "Recent Entries",
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                selected = false,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_dataset),
                        contentDescription = "Go to Recent Observations Screen",
                        modifier = Modifier.size(36.dp)
                    )
                },
                onClick = { navController.navigate(Screens.RecentObservations.route) }
            )

            Spacer(Modifier.height(24.dp))

            Spacer(Modifier.height(32.dp))

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { adminExpanded = !adminExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Admin Actions",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                Icon(
                    painter = painterResource(
                        if (adminExpanded) R.drawable.ic_arrow_drop_up else R.drawable.ic_arrow_drop_down,
                    ),
                    contentDescription = if (adminExpanded) "Collapse" else "Expand"
                )
            }

            if (adminExpanded) {
                NavigationDrawerItem(
                    label = {
                        Text(
                            "Data Management",
                            style = MaterialTheme.typography.displaySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    },
                    selected = false,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_admin_panel_settings),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    onClick = { navController.navigate(Screens.Admin.route) }
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}