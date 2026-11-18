package com.phacsi.lab13animaciones

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.phacsi.lab13animaciones.ui.theme.Lab13AnimacionesTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Lab13AnimacionesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    JuegoGlobo()
                }
            }
        }
    }
}

@Composable
fun JuegoGlobo() {
    var health by remember { mutableStateOf(100) }
    var score by remember { mutableStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }

    // Posición vertical del globo (0 = arriba, 1 = centro, 2 = abajo)
    var balloonSlot by remember { mutableStateOf(1) }

    // Lista de pinchos activos: cada uno con su X, carril y si ya fue esquivado
    var spikes by remember { mutableStateOf(listOf<Spike>()) }
    var spikeSpeed by remember { mutableStateOf(4f) }

    // Animación de posición del globo
    val balloonOffsetY by animateDpAsState(
        targetValue = (balloonSlot * 120).dp,
        animationSpec = tween(durationMillis = 150),
        label = "balloonY"
    )

    // Color del globo según vida
    val balloonColor by animateColorAsState(
        targetValue = when {
            health <= 30 -> Color(0xFFE53935)
            health <= 60 -> Color(0xFFFFEB3B)
            else -> Color(0xFF42A5F5)
        },
        label = "balloonColor"
    )

    // Bucle principal del juego: genera pinchos y los mueve
    LaunchedEffect(started, gameOver) {
        if (!started || gameOver) return@LaunchedEffect

        while (!gameOver) {
            // Generar un pincho aleatorio en cualquier carril
            val nuevoPincho = Spike(
                x = 1000f,
                slot = Random.nextInt(0, 3),
                id = Random.nextInt()
            )
            spikes = spikes + nuevoPincho

            // Esperar un tiempo aleatorio antes del siguiente pincho
            delay(Random.nextLong(600, 1200))

            // Si el juego terminó durante la espera, salir
            if (gameOver || !started) break
        }
    }

    // Bucle de movimiento de pinchos
    LaunchedEffect(started, gameOver) {
        if (!started || gameOver) return@LaunchedEffect

        while (!gameOver) {
            delay(16) // ~60 FPS
            spikes = spikes.map { spike ->
                spike.copy(x = spike.x - spikeSpeed)
            }.filter { it.x > -100f }
        }
    }

    // Detección de colisiones
    LaunchedEffect(spikes, balloonSlot, gameOver) {
        if (!started || gameOver) return@LaunchedEffect

        val globoX = 60f
        val globoY = balloonSlot

        spikes.forEach { spike ->
            if (!spike.hit && spike.x in (globoX - 40f)..(globoX + 40f) && spike.slot == globoY) {
                // Colisión
                health = (health - 25).coerceAtLeast(0)
                if (health <= 0) gameOver = true
                spikes = spikes.map { if (it.id == spike.id) it.copy(hit = true) else it }
            }
        }

        // Sumar puntos por pinchos esquivados
        spikes.forEach { spike ->
            if (!spike.hit && !spike.scored && spike.x < globoX - 50f) {
                score += 1
                spikeSpeed += 0.2f
                spikes = spikes.map { if (it.id == spike.id) it.copy(scored = true) else it }
            }
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Puntos: $score", style = MaterialTheme.typography.titleMedium)
            Text("Vida: $health", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .background(Color(0xFF0D1B2A), shape = RoundedCornerShape(16.dp))
        ) {
            // Líneas de carril
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(1.dp)
                    .offset(y = 120.dp)
                    .background(Color(0xFF1B263B))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(1.dp)
                    .offset(y = 240.dp)
                    .background(Color(0xFF1B263B))
            )

            // Pinchos (cada uno en su carril, sin overlap visual)
            spikes.forEach { spike ->
                if (!spike.hit) {
                    Box(
                        modifier = Modifier
                            .offset(x = spike.x.dp, y = (spike.slot * 120 + 20).dp)
                            .size(40.dp)
                            .background(Color(0xFFFF7043), shape = RoundedCornerShape(4.dp))
                    )
                }
            }

            // Globo
            Box(
                modifier = Modifier
                    .offset(x = 60.dp, y = balloonOffsetY + 20.dp)
                    .size(60.dp)
                    .background(balloonColor, shape = CircleShape)
            )

            // Inicio
            if (!started && !gameOver) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Protege el Globo",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Text(
                            "Esquiva los pinchos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            started = true
                            spikes = emptyList()
                            spikeSpeed = 4f
                            health = 100
                            score = 0
                            balloonSlot = 1
                        }) {
                            Text("Jugar")
                        }
                    }
                }
            }

            // Game Over
            if (gameOver) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "¡GAME OVER!",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                        Text(
                            "Puntos: $score",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            health = 100
                            score = 0
                            gameOver = false
                            started = true
                            spikeSpeed = 4f
                            spikes = emptyList()
                            balloonSlot = 1
                        }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Button(
                onClick = { if (balloonSlot > 0) balloonSlot -= 1 },
                enabled = !gameOver && started
            ) {
                Text("⬆ Subir")
            }
            Button(
                onClick = { if (balloonSlot < 2) balloonSlot += 1 },
                enabled = !gameOver && started
            ) {
                Text("⬇ Bajar")
            }
        }
    }
}

// Modelo de pincho
data class Spike(
    val x: Float,
    val slot: Int,
    val id: Int,
    val hit: Boolean = false,
    val scored: Boolean = false
)