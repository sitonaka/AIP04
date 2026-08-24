package com.example.aip04

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.aip04.ui.theme.AIP04Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AIP04Theme {
                PedometerScreen()
            }
        }
    }
}

@Composable
fun PedometerScreen() {
    val context = LocalContext.current
    var stepCount by remember { mutableStateOf(0) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val stepSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }
    val isSensorAvailable = stepSensor != null

    if (hasPermission && isSensorAvailable) {
        StepCounter(sensorManager, stepSensor, onStepChanged = { steps ->
            stepCount = steps
        })
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!isSensorAvailable) {
                Text(
                    text = "このデバイスには歩数計センサーが搭載されていません",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Text(text = "今日の歩数", fontSize = 24.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "$stepCount",
                    fontSize = 64.sp,
                    style = MaterialTheme.typography.headlineLarge
                )
            }
            if (!hasPermission) {
                Text(
                    text = "歩数計を利用するには権限が必要です",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun StepCounter(
    sensorManager: SensorManager,
    stepSensor: Sensor?,
    onStepChanged: (Int) -> Unit
) {
    // 初回の値を保存して差分を取るための変数
    var initialSteps by remember { mutableStateOf(-1f) }

    val sensorEventListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
                    val totalSteps = event.values[0]
                    if (initialSteps == -1f) {
                        initialSteps = totalSteps
                    }
                    val currentSteps = (totalSteps - initialSteps).toInt()
                    onStepChanged(currentSteps)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    DisposableEffect(stepSensor) {
        if (stepSensor != null) {
            sensorManager.registerListener(
                sensorEventListener,
                stepSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }
}