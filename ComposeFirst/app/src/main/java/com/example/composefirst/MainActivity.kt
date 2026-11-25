package com.example.composefirst

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.composefirst.first.tab_switching.TabSwitchingApp
import com.example.composefirst.second.home.HomeApp
import com.example.composefirst.ui.theme.ComposeFirstTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeFirstTheme {
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                    OutlinedTextFieldExample()
//                    StatementExample()
//                    AlertDialogExample()

//                    NavGraph()
//                    NestedListExample()

//                    NavBarNavigation()
//                TabSwitchingApp()

                HomeApp()



            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ComposeFirstTheme {
        Greeting("Android")
    }
}
