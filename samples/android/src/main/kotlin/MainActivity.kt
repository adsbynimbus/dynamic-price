@file:OptIn(ExperimentalMaterial3Api::class)

package com.adsbynimbus.dynamicprice.sample

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.startup.Initializer
import com.adsbynimbus.Nimbus
import kotlin.time.measureTime

class AppInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val nimbusStartup = measureTime {
            Nimbus.initialize(context, BuildConfig.PUBLISHER_KEY, BuildConfig.API_KEY)
            Nimbus.testMode = true
        }

        Log.i("DynamicPrice", "Nimbus init time: $nimbusStartup")
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NimbusTheme {
                App(this)
            }
        }
    }
}

enum class AdTypes(val title: String, val screen: @Composable () -> Unit = {}) {
    AdLoader("AdLoader Banner", screen = ::AdLoader),
    AdLoaderWithVideo("AdLoader Banner w/ Video"),
    AdViewBanner("Banner"),
    AdViewBannerWithVideo("Banner w/ Video"),
    DynamicUnit("Dynamic Unit"),
    Interstitial("Interstitial"),
    RewardedVideo("Rewarded Video"),
}

@Composable
fun App(
    activity: ComponentActivity,
    appName: String = activity.applicationInfo.loadLabel(activity.packageManager).toString(),
    navController: NavHostController = rememberNavController(),
) {

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = backStackEntry?.destination?.route ?: appName

    Scaffold(
        topBar = {
            AppBar(
                currentScreen = currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = navController::navigateUp,
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = appName,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
        ) {
            composable(route = appName) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AdTypes.entries.forEach {
                        Button(
                            onClick = { navController.navigate(route = it.title) },
                            modifier = Modifier.fillMaxWidth(fraction = 0.5f),
                            content = { Text(it.title) },
                        )
                    }
                }
            }
            AdTypes.entries.forEach { adType ->
                composable(route = adType.title) { adType.screen() }
            }
        }
    }
}

@Composable
fun AppBar(
    currentScreen: String,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = { Text(currentScreen) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
        },
    )
}

@Composable
fun NimbusTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(
        primary = Color(0xFFDB6FA3),   /* Pink */
        secondary = Color(0xFF85D6DA), /* Teal */
        background = Color.Black,
        surface = Color.Black,
    ),
    content = content,
)
