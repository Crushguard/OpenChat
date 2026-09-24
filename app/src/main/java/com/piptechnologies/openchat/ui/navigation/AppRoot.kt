package com.piptechnologies.openchat.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.piptechnologies.openchat.ui.components.BrandMark
import com.piptechnologies.openchat.ui.components.HonestyLine
import com.piptechnologies.openchat.ui.components.ScreenSurface
import com.piptechnologies.openchat.ui.theme.OcTheme

/** Root of the app UI. Until the navigation graph lands this renders the launch screen. */
@Composable
fun AppRoot() {
    ScreenSurface {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandMark(size = 88.dp, radius = 26.dp, iconSize = 40.dp)
            Spacer(Modifier.height(20.dp))
            Text(text = "OpenChat", style = OcTheme.type.display24, color = OcTheme.colors.inkStrong)
        }
        HonestyLine(text = "Free · No ads · No account", modifier = Modifier.padding(bottom = 34.dp))
    }
}
