package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.assistant.core.AssistantState
import com.example.assistant.core.OrbTheme
import kotlin.math.*
import kotlin.random.Random

/**
 * High-performance Canvas-based 3D Glowing, Pulsing Orb with integrated companion avatar
 * serving as the primary visual interface for the assistant's real-time state.
 */
@Composable
fun MayaOrb(
    state: AssistantState,
    orbTheme: OrbTheme = OrbTheme.CYBER_CYAN,
    audioLevel: Float = 0f,
    customization: com.example.assistant.ultimate.OrbCustomization? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "maya_3d_orb_anim")

    // Primary 3D rotation angle
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.THINKING -> 1800
                    AssistantState.EXECUTING -> 1400
                    AssistantState.LISTENING -> 3600
                    AssistantState.SPEAKING -> 2800
                    AssistantState.ERROR -> 1200
                    else -> 7000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot_3d"
    )

    // Secondary counter-rotation for gyroscopic resonance
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.THINKING -> 2400
                    AssistantState.EXECUTING -> 1800
                    else -> 8500
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rot_3d"
    )

    // Organic breathing & pulsing cycle
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.LISTENING -> 550
                    AssistantState.SPEAKING -> 750
                    AssistantState.THINKING -> 400
                    AssistantState.ERROR -> 300
                    else -> 2200
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_3d"
    )

    // Expanding coronal shockwave ring
    val shockwaveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) 1200 else 2600,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shockwave"
    )

    // Internal plasma wave shimmer
    val plasmaPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "plasma"
    )

    // Dynamic state theme color resolution
    val targetColor = when (state) {
        AssistantState.IDLE -> orbTheme.coreColor
        AssistantState.LISTENING -> Color(0xFF10B981) // Emerald Green
        AssistantState.THINKING -> Color(0xFFF59E0B) // Solar Amber
        AssistantState.EXECUTING -> Color(0xFF6366F1) // Quantum Indigo
        AssistantState.SPEAKING -> Color(0xFF00F0FF) // Cyber Cyan
        AssistantState.ERROR -> Color(0xFFEF4444) // Crimson Alert
        AssistantState.OFFLINE -> Color(0xFF64748B) // Slate Gray
    }

    val animatedCoreColor by animateColorAsState(targetValue = targetColor, animationSpec = tween(400), label = "core_col")
    val animatedGlowColor by animateColorAsState(targetValue = targetColor.copy(alpha = 0.45f), animationSpec = tween(400), label = "glow_col")

    // 3D Spherical Particle Field with Z-depth
    val particles = remember {
        List(42) {
            Spherical3DParticle(
                theta = Random.nextFloat() * (2 * PI).toFloat(),
                phi = (Random.nextFloat() - 0.5f) * PI.toFloat(),
                speed = 0.4f + Random.nextFloat() * 0.9f,
                radius = 1.6f + Random.nextFloat() * 2.4f,
                distanceFactor = 1.15f + Random.nextFloat() * 0.75f,
                baseAlpha = 0.35f + Random.nextFloat() * 0.65f
            )
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val baseScale = customization?.sizeScale ?: 1f
    val reactiveScale = (breathingPulse + (audioLevel.coerceIn(0f, 1f) * 0.28f)) * baseScale

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // 1. BACKGROUND 3D CANVAS: Coronal glow, 3D orbital rings, and background particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.42f
            val currentRadius = baseRadius * reactiveScale

            // 1. Outer Deep Atmospheric Corona
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedGlowColor.copy(alpha = 0.48f),
                        animatedGlowColor.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * 2.35f
                ),
                radius = currentRadius * 2.35f,
                center = center
            )

            // 2. Expanding Coronal Energy Shockwave
            val shockwaveRadius = currentRadius * (1.0f + shockwaveProgress * 0.9f)
            val shockwaveAlpha = (1f - shockwaveProgress).coerceIn(0f, 1f) * 0.65f
            drawCircle(
                color = animatedCoreColor.copy(alpha = shockwaveAlpha),
                radius = shockwaveRadius,
                center = center,
                style = Stroke(
                    width = 2.5f * (1f - shockwaveProgress),
                    cap = StrokeCap.Round
                )
            )

            // 3. Gyroscopic 3D Tilted Orbital Rings
            // Ring A (Tilted 35 deg, rotating clockwise)
            drawTilted3DEllipse(
                center = center,
                radiusX = currentRadius * 1.55f,
                radiusY = currentRadius * 0.75f,
                tiltAngleDeg = 35f,
                rotationAngleDeg = rotationAngle,
                color = animatedCoreColor.copy(alpha = 0.75f),
                strokeWidth = 2.4f,
                dashPattern = floatArrayOf(28f, 16f)
            )

            // Ring B (Tilted -40 deg, rotating counter-clockwise)
            drawTilted3DEllipse(
                center = center,
                radiusX = currentRadius * 1.75f,
                radiusY = currentRadius * 0.65f,
                tiltAngleDeg = -40f,
                rotationAngleDeg = counterRotation,
                color = animatedCoreColor.copy(alpha = 0.5f),
                strokeWidth = 1.8f,
                dashPattern = floatArrayOf(40f, 24f)
            )

            // Ring C (Equatorial Gyroscope Reticle with Beacons)
            drawTilted3DEllipse(
                center = center,
                radiusX = currentRadius * 1.95f,
                radiusY = currentRadius * 0.5f,
                tiltAngleDeg = 75f,
                rotationAngleDeg = rotationAngle * 0.6f,
                color = animatedCoreColor.copy(alpha = 0.35f),
                strokeWidth = 1.2f,
                dashPattern = floatArrayOf(12f, 12f)
            )

            // Orbital Node Beacons on outer gyro
            val beaconCount = 4
            for (i in 0 until beaconCount) {
                val beaconAngle = Math.toRadians((rotationAngle * 0.6f + (i * (360f / beaconCount))).toDouble())
                val rx = currentRadius * 1.95f
                val ry = currentRadius * 0.5f
                val localX = (rx * cos(beaconAngle)).toFloat()
                val localY = (ry * sin(beaconAngle)).toFloat()

                // Rotate by tilt angle (75 deg)
                val tiltRad = Math.toRadians(75.0)
                val rotX = center.x + (localX * cos(tiltRad) - localY * sin(tiltRad)).toFloat()
                val rotY = center.y + (localX * sin(tiltRad) + localY * cos(tiltRad)).toFloat()

                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = Offset(rotX, rotY)
                )
                drawCircle(
                    color = animatedCoreColor,
                    radius = 5.5f,
                    center = Offset(rotX, rotY),
                    style = Stroke(width = 1.5f)
                )
            }

            // 4. Background 3D Particles (Z < 0 behind sphere)
            for (p in particles) {
                val currentTheta = p.theta + Math.toRadians((rotationAngle * p.speed).toDouble()).toFloat()
                val z = sin(currentTheta) * cos(p.phi)
                if (z < 0) {
                    val x = cos(currentTheta) * cos(p.phi)
                    val y = sin(p.phi)
                    val dist = currentRadius * p.distanceFactor
                    val px = center.x + x * dist
                    val py = center.y + y * dist
                    val depthAlpha = ((z + 1f) / 2f * p.baseAlpha * 0.4f).coerceIn(0.1f, 1f)
                    val depthRadius = (p.radius * (0.6f + (z + 1f) * 0.2f)).coerceAtLeast(1f)

                    drawCircle(
                        color = animatedCoreColor.copy(alpha = depthAlpha),
                        radius = depthRadius,
                        center = Offset(px, py)
                    )
                }
            }
        }

        // 2. CENTRAL 3D HOLOGRAPHIC ORB CORE WITH EMBEDDED AVATAR IMAGE
        Box(
            modifier = Modifier
                .size(116.dp)
                .scale(reactiveScale)
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    spotColor = animatedCoreColor,
                    ambientColor = animatedCoreColor
                )
                .clip(CircleShape)
                .border(
                    width = 2.5.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            animatedCoreColor,
                            Color.White,
                            animatedCoreColor,
                            animatedCoreColor.copy(alpha = 0.5f),
                            animatedCoreColor
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Companion Avatar Image Base
            Image(
                painter = painterResource(id = R.drawable.img_orb_core),
                contentDescription = "MAYA Companion Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Volumetric 3D Shading & Holographic Atmosphere on Avatar
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f
                val lightSource = Offset(
                    center.x - radius * 0.38f,
                    center.y - radius * 0.42f
                )

                // Atmospheric holographic tint
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            animatedCoreColor.copy(alpha = 0.15f),
                            animatedCoreColor.copy(alpha = 0.45f)
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )

                // Inner Plasma Vortex Currents (3D Curvature Lines)
                for (i in 0..3) {
                    val waveOffset = (plasmaPhase + i * (PI / 2f)).toFloat()
                    val waveScale = sin(waveOffset) * 0.22f
                    val sweepColor = if (i % 2 == 0) Color.White.copy(alpha = 0.35f) else animatedCoreColor.copy(alpha = 0.5f)

                    drawArc(
                        color = sweepColor,
                        startAngle = (rotationAngle * (1.2f + i * 0.25f) + i * 50f) % 360f,
                        sweepAngle = 130f,
                        useCenter = false,
                        topLeft = Offset(
                            center.x - radius * (0.85f + waveScale),
                            center.y - radius * (0.85f - waveScale)
                        ),
                        size = Size(
                            radius * (1.7f + waveScale * 2),
                            radius * (1.7f - waveScale * 2)
                        ),
                        style = Stroke(
                            width = 2.2f,
                            cap = StrokeCap.Round
                        )
                    )
                }

                // Fresnel Edge Rim-Lighting
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.75f to Color.Transparent,
                            0.92f to animatedCoreColor.copy(alpha = 0.65f),
                            1.0f to Color.White.copy(alpha = 0.9f)
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 3.5f)
                )

                // Primary Specular Highlight Flare (Upper-Left Reflection)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.75f),
                            Color.White.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = lightSource,
                        radius = radius * 0.4f
                    ),
                    radius = radius * 0.4f,
                    center = lightSource
                )
            }
        }

        // 3. FOREGROUND 3D CANVAS: Foreground particles (Z >= 0) and central audio ripple waves
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.42f
            val currentRadius = baseRadius * reactiveScale

            // Foreground 3D Particles with glowing halos
            for (p in particles) {
                val currentTheta = p.theta + Math.toRadians((rotationAngle * p.speed).toDouble()).toFloat()
                val z = sin(currentTheta) * cos(p.phi)
                if (z >= 0) {
                    val x = cos(currentTheta) * cos(p.phi)
                    val y = sin(p.phi)
                    val dist = currentRadius * p.distanceFactor
                    val px = center.x + x * dist
                    val py = center.y + y * dist
                    val depthAlpha = (p.baseAlpha * (0.6f + z * 0.4f)).coerceIn(0.2f, 1f)
                    val depthRadius = (p.radius * (1f + z * 0.5f)).coerceAtLeast(1f)

                    // Halo
                    drawCircle(
                        color = animatedCoreColor.copy(alpha = depthAlpha * 0.5f),
                        radius = depthRadius * 2.2f,
                        center = Offset(px, py)
                    )
                    // Bright Core
                    drawCircle(
                        color = Color.White.copy(alpha = depthAlpha),
                        radius = depthRadius,
                        center = Offset(px, py)
                    )
                }
            }

            // Audio reactive central focus ripples
            if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
                val rippleCount = 3
                for (r in 1..rippleCount) {
                    val rippleR = (currentRadius * 0.22f * r) + (audioLevel * currentRadius * 0.3f)
                    drawCircle(
                        color = Color.White.copy(alpha = (0.7f / r) * (0.5f + audioLevel * 0.5f)),
                        radius = rippleR,
                        center = center,
                        style = Stroke(width = 2f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

/**
 * Draws a 3D tilted ellipse with customizable perspective tilt, dash pattern, and rotation.
 */
private fun DrawScope.drawTilted3DEllipse(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    tiltAngleDeg: Float,
    rotationAngleDeg: Float,
    color: Color,
    strokeWidth: Float,
    dashPattern: FloatArray
) {
    rotate(degrees = tiltAngleDeg, pivot = center) {
        val sweepAngle = 360f
        drawArc(
            color = color,
            startAngle = rotationAngleDeg % 360f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radiusX, center.y - radiusY),
            size = Size(radiusX * 2, radiusY * 2),
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(dashPattern, 0f)
            )
        )
    }
}

/**
 * 3D spherical coordinate particle with orbital angular velocity and depth perception.
 */
private data class Spherical3DParticle(
    val theta: Float,
    val phi: Float,
    val speed: Float,
    val radius: Float,
    val distanceFactor: Float,
    val baseAlpha: Float
)
