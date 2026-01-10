@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package com.occaecat.ztoeschedule.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CornerBasedShape

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp, top = 8.dp)
    )
}

typealias SegmentedListItemContent = @Composable (index: Int, count: Int) -> Unit
typealias SegmentedContainerItem = @Composable () -> Unit

@Composable
fun SegmentedListSection(
    items: List<SegmentedListItemContent>,
    modifier: Modifier = Modifier,
    gap: androidx.compose.ui.unit.Dp = ListItemDefaults.SegmentedGap,
    showDividers: Boolean = false,
    dividerInset: androidx.compose.ui.unit.Dp = 16.dp
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        val count = items.size
        items.forEachIndexed { index, item ->
            item(index, count)
            if (showDividers && index < count - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = dividerInset),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

@Composable
fun SegmentedContainer(
    items: List<SegmentedContainerItem>,
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = MaterialTheme.shapes.extraLarge,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    dividerInset: androidx.compose.ui.unit.Dp = 16.dp,
    dividerColor: Color = MaterialTheme.colorScheme.outlineVariant,
    border: BorderStroke? = null
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = border
    ) {
        Column {
            items.forEachIndexed { index, item ->
                item()
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = dividerInset),
                        color = dividerColor
                    )
                }
            }
        }
    }
}
