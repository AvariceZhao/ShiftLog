package com.clockin.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.clockin.app.R

object AppIcons {
    val Schedule: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_schedule)

    val History: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_history)

    val Settings: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_settings)

    val Calendar: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_calendar)

    val Export: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_export)

    val Add: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_add)

    val Star: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_star)

    val ChevronLeft: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_chevron_left)

    val ChevronRight: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_chevron_right)
}
