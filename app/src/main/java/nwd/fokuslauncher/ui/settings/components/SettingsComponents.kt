package nwd.fokuslauncher.ui.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nwd.fokuslauncher.data.model.LauncherFontScale
import nwd.fokuslauncher.ui.components.LauncherIcon
import nwd.fokuslauncher.ui.theme.LocalLauncherFontScale
import nwd.fokuslauncher.ui.theme.LocalLauncherIconGlow
import nwd.fokuslauncher.ui.theme.withoutLauncherTextGlow
import nwd.fokuslauncher.ui.util.clickableWithSystemSound
import nwd.fokuslauncher.ui.util.rememberBooleanChangeWithSystemSound
import nwd.fokuslauncher.ui.util.rememberClickWithSystemSound

private val SettingsPickerCorner = RoundedCornerShape(12.dp)

@Composable
internal fun SettingsAccordionPanel(
        title: String,
        expanded: Boolean,
        onExpandedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
        subtitle: String? = null,
        icon: ImageVector? = null,
        badgeText: String? = null,
        content: @Composable ColumnScope.() -> Unit
) {
    val rotationState by
            animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "accordion_chevron_rotation",
            )

    Column(
            modifier =
                    modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .background(
                                    color =
                                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                                    alpha = 0.35f
                                            ),
                                    shape = RoundedCornerShape(16.dp),
                            )
                            .border(
                                    width = 1.dp,
                                    color =
                                            if (expanded)
                                                    MaterialTheme.colorScheme.primary.copy(
                                                            alpha = 0.4f
                                                    )
                                            else
                                                    MaterialTheme.colorScheme.onSurface.copy(
                                                            alpha = 0.08f
                                                    ),
                                    shape = RoundedCornerShape(16.dp),
                            )
    ) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                        Modifier.fillMaxWidth()
                                .clickableWithSystemSound { onExpandedChange(!expanded) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (icon != null) {
                LauncherIcon(
                        imageVector = icon,
                        contentDescription = null,
                        tint =
                                if (expanded) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        iconSize = 22.dp,
                )
                Spacer(Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (!badgeText.isNullOrEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                                modifier =
                                        Modifier.background(
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .primaryContainer,
                                                        shape = CircleShape,
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
                if (!subtitle.isNullOrEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            LauncherIcon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.secondary,
                    iconSize = 24.dp,
                    modifier = Modifier.graphicsLayer { rotationZ = rotationState },
            )
        }

        AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                )
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> SettingsSelectionChipsRow(
        title: String,
        options: List<T>,
        selectedOption: T,
        onOptionSelected: (T) -> Unit,
        labelProvider: @Composable (T) -> String,
        modifier: Modifier = Modifier,
        subtitle: String? = null,
        enabled: Boolean = true,
        chipTextColor: ((T) -> Color)? = null,
        leadingIconProvider: (@Composable (T) -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp)) {
        Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color =
                        if (enabled) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f),
        )
        if (!subtitle.isNullOrEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 12.dp),
                modifier = Modifier.fillMaxWidth(),
        ) {
            items(options) { option ->
                val isSelected = option == selectedOption
                val customColor = chipTextColor?.invoke(option)
                FilterChip(
                        selected = isSelected,
                        onClick =
                                rememberClickWithSystemSound {
                                    if (enabled) onOptionSelected(option)
                                },
                        label = {
                            Text(
                                    text = labelProvider(option),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight =
                                            if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color =
                                            when {
                                                !enabled ->
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                                .copy(alpha = 0.38f)
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                customColor != null -> customColor
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                            )
                        },
                        leadingIcon = leadingIconProvider?.let { { it(option) } },
                        enabled = enabled,
                        shape = RoundedCornerShape(20.dp),
                        colors =
                                FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor =
                                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                                        alpha = 0.6f
                                                ),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                        border =
                                FilterChipDefaults.filterChipBorder(
                                        enabled = enabled,
                                        selected = isSelected,
                                        borderColor =
                                                MaterialTheme.colorScheme.outline.copy(
                                                        alpha = 0.25f
                                                ),
                                        selectedBorderColor = Color.Transparent,
                                ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsToggleRow(
        label: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        subtitle: String? = null,
        enabled: Boolean = true
) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                    Modifier.fillMaxWidth()
                            .clickableWithSystemSound(enabled = enabled) { onCheckedChange(!checked) }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color =
                            if (enabled) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f)
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(
                checked = checked,
                onCheckedChange = rememberBooleanChangeWithSystemSound(onCheckedChange),
                enabled = enabled,
        )
    }
}

@Composable
internal fun SettingsRow(
        label: String,
        modifier: Modifier = Modifier,
        subtitle: String? = null,
        horizontalPadding: Dp = 24.dp,
        verticalPadding: Dp = 12.dp,
        labelStyle: TextStyle = MaterialTheme.typography.bodyLarge,
        subtitleStyle: TextStyle = MaterialTheme.typography.labelSmall,
        labelColor: Color = MaterialTheme.colorScheme.onBackground,
        subtitleColor: Color = MaterialTheme.colorScheme.secondary,
        leading: (@Composable RowScope.() -> Unit)? = null,
        trailing: (@Composable RowScope.() -> Unit)? = null,
        onClick: (() -> Unit)? = null,
        clickableEnabled: Boolean = true,
) {
    val padded =
            Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = verticalPadding)
    val rowModifier =
            modifier.then(
                    if (onClick != null) {
                        padded.clickableWithSystemSound(
                                enabled = clickableEnabled,
                                onClick = onClick,
                        )
                    } else {
                        padded
                    }
            )
    Row(verticalAlignment = Alignment.Top, modifier = rowModifier) {
        leading?.invoke(this)
        if (leading != null) {
            Spacer(Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = labelStyle, color = labelColor)
            if (!subtitle.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(text = subtitle, style = subtitleStyle, color = subtitleColor)
            }
        }
        trailing?.invoke(this)
    }
}

@Composable
private fun settingsPickerMenuItemColors() =
        MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.onBackground,
                leadingIconColor = MaterialTheme.colorScheme.onBackground,
                trailingIconColor = MaterialTheme.colorScheme.onBackground,
        )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsReadOnlyExposedDropdown(
        expanded: Boolean,
        onExpandedChange: (Boolean) -> Unit,
        selectedDisplayText: String,
        fieldEnabled: Boolean = true,
        menuExpanded: Boolean = expanded,
        textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
        textFieldModifier: Modifier = Modifier,
        fieldTextColor: Color? = null,
        menuContent: @Composable ColumnScope.() -> Unit
) {
    val launcherGlowEnabled = LocalLauncherIconGlow.current.enabled
    // Match a standard single-line outlined field (~56dp); typography already scales with font
    // size — avoid multiplying shell dp by [LocalLauncherFontScale] or the bar becomes very tall.
    val fieldHeight = if (launcherGlowEnabled) 58.dp else 56.dp
    val fieldHorizontalPadding = PaddingValues(start = 16.dp, end = 4.dp)
    val selectedTextGlowPadding = if (launcherGlowEnabled) 2.dp else 0.dp
    val resolvedTextStyle =
            if (launcherGlowEnabled) textStyle else textStyle.withoutLauncherTextGlow()
    val outlineColor =
            when {
                !fieldEnabled -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f)
                menuExpanded -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f)
            }
    val outlineWidth = if (menuExpanded && fieldEnabled) 2.dp else 1.dp
    ExposedDropdownMenuBox(
            expanded = menuExpanded,
            onExpandedChange = onExpandedChange
    ) {
        // Plain Text (not OutlinedTextField) so soft text shadow / glow is not clipped to a rect.
        Box(
                modifier =
                        textFieldModifier
                                .menuAnchor(
                                        ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                        enabled = fieldEnabled
                                )
                                .fillMaxWidth()
        ) {
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .height(fieldHeight)
                                    .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            SettingsPickerCorner,
                                    )
                                    .border(
                                            BorderStroke(outlineWidth, outlineColor),
                                            SettingsPickerCorner,
                                    )
                                    .padding(fieldHorizontalPadding),
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                        text = selectedDisplayText,
                        modifier =
                                Modifier.weight(1f)
                                        .padding(vertical = selectedTextGlowPadding),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = resolvedTextStyle,
                        color =
                                when {
                                    fieldTextColor != null && fieldEnabled -> fieldTextColor
                                    fieldTextColor != null -> fieldTextColor.copy(alpha = 0.55f)
                                    !fieldEnabled ->
                                            MaterialTheme.colorScheme.onBackground.copy(
                                                    alpha = 0.55f
                                            )
                                    else -> Color.Unspecified
                                },
                )
                IconButton(
                        onClick = { onExpandedChange(!menuExpanded) },
                        enabled = fieldEnabled,
                ) {
                    LauncherIcon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            iconSize = 24.dp,
                            tint =
                                    if (fieldEnabled) MaterialTheme.colorScheme.onBackground
                                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                    )
                }
            }
        }
        ExposedDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { onExpandedChange(false) },
                shape = SettingsPickerCorner,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
                border =
                        BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f)
                        ),
        ) {
            menuContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsLabeledDropdown(
        title: String,
        subtitle: String? = null,
        expanded: Boolean,
        onExpandedChange: (Boolean) -> Unit,
        selectedDisplayText: String,
        fieldEnabled: Boolean = true,
        menuExpanded: Boolean = expanded,
        textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
        textFieldModifier: Modifier = Modifier,
        fieldTextColor: Color? = null,
        menuContent: @Composable ColumnScope.() -> Unit,
) {
    Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
            )
        }
        Spacer(Modifier.height(12.dp))
        SettingsReadOnlyExposedDropdown(
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                selectedDisplayText = selectedDisplayText,
                fieldEnabled = fieldEnabled,
                menuExpanded = menuExpanded,
                textStyle = textStyle,
                textFieldModifier = textFieldModifier,
                fieldTextColor = fieldTextColor,
                menuContent = menuContent,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> SettingsDropdown(
        title: String,
        subtitle: String? = null,
        options: List<T>,
        expanded: Boolean,
        onExpandedChange: (Boolean) -> Unit,
        selectedDisplayText: String,
        fieldEnabled: Boolean = true,
        menuExpanded: Boolean = expanded,
        textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
        textFieldModifier: Modifier = Modifier,
        fieldTextColor: Color? = null,
        menuItemTextColor: ((T) -> Color)? = null,
        itemContent: @Composable (T) -> Unit,
        onItemSelected: (T) -> Unit,
) {
    SettingsLabeledDropdown(
            title = title,
            subtitle = subtitle,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            selectedDisplayText = selectedDisplayText,
            fieldEnabled = fieldEnabled,
            menuExpanded = menuExpanded,
            textStyle = textStyle,
            textFieldModifier = textFieldModifier,
            fieldTextColor = fieldTextColor,
    ) {
        val menuGlowEnabled = LocalLauncherIconGlow.current.enabled
        val menuFontScale =
                LocalLauncherFontScale.current.coerceIn(LauncherFontScale.MIN, LauncherFontScale.MAX)
        val menuItemVerticalPadding =
                ((if (menuGlowEnabled) 14f else 8f) * menuFontScale).dp
        val menuItemHorizontalPadding = (16f * menuFontScale).dp
        val menuItemContentPadding =
                PaddingValues(
                        horizontal = menuItemHorizontalPadding,
                        vertical = menuItemVerticalPadding,
                )
        options.forEach { option ->
            val itemColors =
                    if (menuItemTextColor != null) {
                        val c = menuItemTextColor(option)
                        MenuDefaults.itemColors(
                                textColor = c,
                                leadingIconColor = c,
                                trailingIconColor = c,
                        )
                    } else {
                        settingsPickerMenuItemColors()
                    }
            DropdownMenuItem(
                    text = { itemContent(option) },
                    onClick =
                            rememberClickWithSystemSound {
                                if (!fieldEnabled) return@rememberClickWithSystemSound
                                onItemSelected(option)
                                onExpandedChange(false)
                            },
                    colors = itemColors,
                    contentPadding = menuItemContentPadding,
            )
        }
    }
}

@Composable
fun SettingsDivider() {
    Spacer(Modifier.height(10.dp))
    androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            thickness = 0.75.dp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.16f),
    )
    Spacer(Modifier.height(10.dp))
}
