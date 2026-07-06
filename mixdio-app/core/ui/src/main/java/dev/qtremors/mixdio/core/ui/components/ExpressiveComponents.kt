package dev.qtremors.mixdio.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class GroupPosition {
    Top, Middle, Bottom, Single
}

fun getGroupShape(position: GroupPosition): RoundedCornerShape {
    return when (position) {
        GroupPosition.Top -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
        GroupPosition.Middle -> RoundedCornerShape(8.dp)
        GroupPosition.Bottom -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        GroupPosition.Single -> RoundedCornerShape(24.dp)
    }
}

enum class ExpressiveButtonType {
    Filled, Tonal, Elevated, Outlined, Text, Hold
}

enum class ExpressiveButtonSize {
    Small, Medium, Large, ExtraLarge
}

data class ExpressiveToggleOption(
    val text: String? = null,
    val icon: ImageVector? = null,
    val weight: Float = 1f,
    val type: ExpressiveButtonType? = null,
    val selectedType: ExpressiveButtonType? = null,
    val isLoading: Boolean = false,
    val loadingProgress: Float? = null,
    val backgroundProgress: Float? = null,
    val containerColor: Color? = null,
    val contentColor: Color? = null,
    val enabled: Boolean = true
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: ExpressiveButtonType = ExpressiveButtonType.Filled,
    size: ExpressiveButtonSize = ExpressiveButtonSize.ExtraLarge,
    text: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingProgress: Float? = null,
    backgroundProgress: Float? = null,
    fillMaxWidth: Boolean = false,
    containerColor: Color? = null,
    contentColor: Color? = null,
    pillCornerRadius: Dp? = null,
    pressedCornerRadius: Dp? = null,
    selected: Boolean = false,
    onHoldComplete: (() -> Unit)? = null,
    holdDuration: Long = 1500L,
    enableAfterDelayMillis: Long = 0L,
    interactionSource: MutableInteractionSource? = null,
    isFirst: Boolean = true,
    isLast: Boolean = true,
    customShape: Shape? = null,
    content: @Composable (RowScope.() -> Unit)? = null
) {
    val coreInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by coreInteractionSource.collectIsPressedAsState()
    var isTapped by remember { mutableStateOf(false) }
    val visualPressed = isPressed || isTapped

    val curPressed by rememberUpdatedState(isPressed)
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    val delayAnim = remember(enableAfterDelayMillis) { Animatable(0f) }
    val isDelaying = enableAfterDelayMillis > 0 && delayAnim.value < 1f
    val bounceScale = remember { Animatable(1f) }

    LaunchedEffect(enableAfterDelayMillis) {
        if (enableAfterDelayMillis > 0) {
            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val p = (elapsed.toFloat() / enableAfterDelayMillis).coerceIn(0f, 1f)
                delayAnim.snapTo(p)
                if (p >= 1f) break
                delay(16)
            }
            bounceScale.animateTo(
                targetValue = 1f,
                initialVelocity = 2f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        } else {
            delayAnim.snapTo(1f)
        }
    }

    val resH = when (size) {
        ExpressiveButtonSize.Small -> 32.dp
        ExpressiveButtonSize.Medium -> 40.dp
        ExpressiveButtonSize.Large -> 48.dp
        ExpressiveButtonSize.ExtraLarge -> 64.dp
    }
    val resPillR = pillCornerRadius ?: (resH / 2)
    val resPressR = pressedCornerRadius ?: when (size) {
        ExpressiveButtonSize.Small -> 8.dp
        ExpressiveButtonSize.Medium -> 12.dp
        ExpressiveButtonSize.Large -> 16.dp
        ExpressiveButtonSize.ExtraLarge -> 24.dp
    }
    val resTextS = when (size) {
        ExpressiveButtonSize.Small -> MaterialTheme.typography.labelSmall
        ExpressiveButtonSize.Medium -> MaterialTheme.typography.labelMedium
        ExpressiveButtonSize.Large -> MaterialTheme.typography.labelLarge
        ExpressiveButtonSize.ExtraLarge -> MaterialTheme.typography.titleMedium
    }
    val resIconS = when (size) {
        ExpressiveButtonSize.Small -> 16.dp
        ExpressiveButtonSize.Medium -> 18.dp
        ExpressiveButtonSize.Large -> 20.dp
        ExpressiveButtonSize.ExtraLarge -> 24.dp
    }
    val resPadH = when (size) {
        ExpressiveButtonSize.Small -> 8.dp
        ExpressiveButtonSize.Medium -> 12.dp
        ExpressiveButtonSize.Large -> 20.dp
        ExpressiveButtonSize.ExtraLarge -> 24.dp
    }
    val resInnerR = when (size) {
        ExpressiveButtonSize.Small -> 4.dp
        ExpressiveButtonSize.Medium -> 6.dp
        ExpressiveButtonSize.Large -> 8.dp
        ExpressiveButtonSize.ExtraLarge -> 12.dp
    }

    val animInnerR by animateDpAsState(
        targetValue = if (visualPressed) resPressR else resInnerR,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "animInnerR"
    )

    val holdAnim = remember { Animatable(0f) }
    val isHoldAction = onHoldComplete != null || type == ExpressiveButtonType.Hold

    val targetProgress = if (isDelaying) delayAnim.value else maxOf(holdAnim.value, backgroundProgress ?: 0f)
    val animatedProgressState = animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = if (targetProgress < 0.05f || isDelaying) spring(stiffness = Spring.StiffnessLow) else spring(stiffness = Spring.StiffnessHigh),
        label = "smoothProgress"
    )
    
    val cScaleState = animateFloatAsState(
        targetValue = if (visualPressed && enabled) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "cScale"
    )

    val finalScale = cScaleState.value * bounceScale.value

    val animR by animateDpAsState(
        targetValue = if (!enabled || visualPressed) resPressR else resPillR,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "animR"
    )
    
    val resExPad = when (size) {
        ExpressiveButtonSize.Small -> 4.dp
        ExpressiveButtonSize.Medium -> 8.dp
        ExpressiveButtonSize.Large -> 12.dp
        ExpressiveButtonSize.ExtraLarge -> 16.dp
    }
    
    val exPad by animateDpAsState(
        targetValue = when {
            visualPressed -> resExPad
            selected -> resExPad / 2
            else -> 0.dp
        },
        label = "exPad"
    )

    LaunchedEffect(isPressed) {
        if (isPressed && enabled && !isLoading && !isDelaying) {
            try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
            if (isHoldAction) {
                val startTime = System.currentTimeMillis()
                while (true) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val p = (elapsed.toFloat() / holdDuration).coerceIn(0f, 1f)
                    holdAnim.snapTo(p)
                    if (p >= 1f) break
                    delay(16)
                }
                if (curPressed) {
                    try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                    onHoldComplete?.invoke()
                    holdAnim.snapTo(0f)
                }
            }
        } else if (!isPressed && isHoldAction) {
            holdAnim.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
        }
    }

    val fShape = customShape ?: RoundedCornerShape(
        topStart = if (isFirst) animR else animInnerR,
        bottomStart = if (isFirst) animR else animInnerR,
        topEnd = if (isLast) animR else animInnerR,
        bottomEnd = if (isLast) animR else animInnerR
    )
    val primary = MaterialTheme.colorScheme.primary
    
    val targetBg = containerColor ?: when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
        type == ExpressiveButtonType.Filled || type == ExpressiveButtonType.Hold -> if (isLoading || isDelaying) MaterialTheme.colorScheme.primaryContainer else primary
        type == ExpressiveButtonType.Tonal -> MaterialTheme.colorScheme.secondaryContainer
        type == ExpressiveButtonType.Elevated -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> Color.Transparent
    }
    val animatedBg by animateColorAsState(targetValue = targetBg, label = "bgAnim")

    val targetContent = contentColor ?: when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        type == ExpressiveButtonType.Filled || type == ExpressiveButtonType.Hold -> if (isLoading || isDelaying) primary else MaterialTheme.colorScheme.onPrimary
        type == ExpressiveButtonType.Tonal -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> primary
    }
    val animatedContentColor by animateColorAsState(targetValue = targetContent, label = "contentAnim")

    val progressBrush = remember(animatedContentColor) {
        Brush.horizontalGradient(
            listOf(animatedContentColor.copy(alpha = 0.12f), animatedContentColor.copy(alpha = 0.24f))
        )
    }

    Surface(
        onClick = {
            if (!enabled) {
                scope.launch {
                    val intensity = with(density) { 6.dp.toPx() }
                    shakeOffset.animateTo(intensity, spring(stiffness = 10000f))
                    shakeOffset.animateTo(-intensity, spring(stiffness = 10000f))
                    shakeOffset.animateTo(intensity / 2, spring(stiffness = 10000f))
                    shakeOffset.animateTo(-intensity / 2, spring(stiffness = 10000f))
                    shakeOffset.animateTo(0f, spring(stiffness = 10000f))
                }
            } else {
                scope.launch {
                    isTapped = true
                    delay(100)
                    isTapped = false
                }
                onClick()
            }
        },
        modifier = modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
            .height(resH)
            .graphicsLayer { translationX = shakeOffset.value },
        enabled = (enabled && !isLoading && !isDelaying) || !enabled,
        shape = fShape,
        color = animatedBg,
        contentColor = animatedContentColor,
        tonalElevation = if (type == ExpressiveButtonType.Elevated) 2.dp else 0.dp,
        shadowElevation = if (type == ExpressiveButtonType.Elevated) 2.dp else 0.dp,
        border = if (type == ExpressiveButtonType.Outlined) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        interactionSource = coreInteractionSource
    ) {
        Box(
            modifier = Modifier.then(if (fillMaxWidth) Modifier.fillMaxSize() else Modifier),
            contentAlignment = Alignment.Center
        ) {
            val p = animatedProgressState.value
            if (p > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            drawRect(
                                brush = progressBrush,
                                size = this.size.copy(width = this.size.width * p)
                            )
                        }
                )
            }

            Box(
                modifier = Modifier.padding(horizontal = resPadH + exPad),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isLoading || isDelaying,
                    transitionSpec = { fadeIn(spring(stiffness = Spring.StiffnessLow)) togetherWith fadeOut(spring(stiffness = Spring.StiffnessLow)) },
                    label = "cSwitch"
                ) { loadingOrDelay ->
                    if (loadingOrDelay) {
                        if (loadingProgress == null && !isDelaying) {
                            LoadingIndicator(
                                color = animatedContentColor,
                                modifier = Modifier.size(resH * 0.6f)
                            )
                        } else {
                            val prog = if (isDelaying) delayAnim.value else (loadingProgress ?: 0f)
                            Box(contentAlignment = Alignment.Center) {
                                CircularWavyProgressIndicator(
                                    progress = { prog.coerceIn(0f, 1f) },
                                    color = animatedContentColor,
                                    trackColor = animatedContentColor.copy(alpha = 0.12f),
                                    stroke = Stroke(width = with(density) { (resH * 0.06f).toPx() }),
                                    trackStroke = Stroke(width = with(density) { (resH * 0.06f).toPx() }),
                                    modifier = Modifier.size(resH * 0.7f)
                                )
                                if (isDelaying) {
                                    val secondsLeft = kotlin.math.ceil((1f - delayAnim.value) * (enableAfterDelayMillis / 1000f)).toInt()
                                    if (secondsLeft > 0) {
                                        Text(
                                            text = secondsLeft.toString(),
                                            style = resTextS,
                                            fontWeight = FontWeight.Black,
                                            color = animatedContentColor
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.graphicsLayer { 
                                scaleX = finalScale
                                scaleY = finalScale
                            }
                        ) {
                            if (content != null) {
                                content()
                            } else {
                                AnimatedVisibility(
                                    visible = icon != null,
                                    enter = fadeIn() + expandHorizontally(),
                                    exit = fadeOut() + shrinkHorizontally()
                                ) {
                                    icon?.let {
                                        Icon(
                                            imageVector = it,
                                            contentDescription = null,
                                            modifier = Modifier.size(resIconS)
                                        )
                                    }
                                }
                                AnimatedVisibility(
                                    visible = text != null,
                                    enter = fadeIn() + expandHorizontally(),
                                    exit = fadeOut() + shrinkHorizontally()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (icon != null) {
                                            Spacer(Modifier.width(resH * 0.15f))
                                        }
                                        Text(
                                            text = text ?: "",
                                            style = resTextS,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveToggleButtonGroup(
    options: List<ExpressiveToggleOption>,
    selectedIndices: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isMultiSelect: Boolean = false,
    size: ExpressiveButtonSize = ExpressiveButtonSize.Medium,
    isInsideContainer: Boolean = false,
    isShowingCheck: Boolean = true,
    showTextSelected: Boolean = false,
    isFillMaxWidth: Boolean = true
) {
    val resH = when (size) {
        ExpressiveButtonSize.Small -> 32.dp
        ExpressiveButtonSize.Medium -> 40.dp
        ExpressiveButtonSize.Large -> 48.dp
        ExpressiveButtonSize.ExtraLarge -> 56.dp
    }
    Row(
        modifier = modifier
            .height(resH)
            .then(if (isFillMaxWidth) Modifier.fillMaxWidth() else Modifier),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = selectedIndices.contains(index)
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scope = rememberCoroutineScope()
            var isTapped by remember { mutableStateOf(false) }

            // Dynamic weight shifting calculations
            val animatedWeight by animateFloatAsState(
                targetValue = when {
                    isPressed || isTapped -> if (showTextSelected) option.weight * 1.8f else option.weight * 1.4f
                    isSelected -> if (showTextSelected) option.weight * 1.6f else option.weight * 1.2f
                    else -> if (showTextSelected) option.weight * 0.85f else option.weight
                },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                label = "wAnim_$index"
            )

            // Fluid corner morphing
            val baseR = resH / 2
            val smallR = resH * 0.2f
            val startR by animateDpAsState(targetValue = if (isPressed || isTapped) baseR * 0.6f else if (isSelected || index == 0) baseR else smallR)
            val endR by animateDpAsState(targetValue = if (isPressed || isTapped) baseR * 0.6f else if (isSelected || index == options.size - 1) baseR else smallR)
            val currentType = if (isSelected) (option.selectedType ?: ExpressiveButtonType.Filled) else (option.type ?: ExpressiveButtonType.Tonal)

            val unselectedBg = if (isInsideContainer) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLow
            val unselectedContent = if (isInsideContainer) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

            ExpressiveButton(
                onClick = {
                    scope.launch {
                        isTapped = true
                        delay(100)
                        isTapped = false
                    }
                    onToggle(index)
                },
                modifier = Modifier.weight(animatedWeight),
                type = currentType,
                size = size,
                text = if (showTextSelected) {
                    if (isSelected || isPressed || isTapped) option.text else null
                } else option.text,
                icon = if (isSelected && isMultiSelect && isShowingCheck && option.icon == null) Icons.Default.Check else option.icon,
                isLoading = option.isLoading,
                loadingProgress = option.loadingProgress,
                backgroundProgress = option.backgroundProgress,
                containerColor = option.containerColor ?: if (isSelected) null else unselectedBg,
                contentColor = option.contentColor ?: if (isSelected) null else unselectedContent,
                enabled = option.enabled,
                selected = isSelected,
                interactionSource = interactionSource,
                customShape = RoundedCornerShape(topStart = startR, bottomStart = startR, topEnd = endR, bottomEnd = endR)
            )
        }
    }
}
