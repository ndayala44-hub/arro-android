package app.arro.platform

import app.arro.R
import app.arro.ui.str
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.sqrt

/** Camera permission state plus a function to ask for it. */
class CameraPermission(val granted: Boolean, val denied: Boolean, val request: () -> Unit)

@Composable
fun rememberCameraPermission(): CameraPermission {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        denied = !ok
    }
    return CameraPermission(granted, denied) { launcher.launch(Manifest.permission.CAMERA) }
}

fun hasCamera(context: Context, front: Boolean): Boolean =
    context.packageManager.hasSystemFeature(if (front) PackageManager.FEATURE_CAMERA_FRONT else PackageManager.FEATURE_CAMERA_ANY)

/**
 * Live camera preview bound to the screen's lifecycle. Pass [capture] to take photos and [analyzer] to read frames
 * (text, faces, QR codes). [torch] turns the flash light on.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    front: Boolean = false,
    capture: ImageCapture? = null,
    analyzer: ImageAnalysis.Analyzer? = null,
    torch: Boolean = false,
    onError: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(front, capture, analyzer) {
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            runCatching {
                val p = future.get()
                provider = p
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                val analysis = analyzer?.let { a ->
                    ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also { it.setAnalyzer(executor, a) }
                }
                p.unbindAll()
                val cases = listOfNotNull(preview, capture, analysis)
                camera = p.bindToLifecycle(owner, selector, *cases.toTypedArray())
            }.onFailure { onError(it.message ?: str(R.string.camera_the_camera_isn_t_available)) }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { provider?.unbindAll() }
        }
    }
    LaunchedEffect(camera, torch) {
        camera?.let { c -> if (c.cameraInfo.hasFlashUnit()) c.cameraControl.enableTorch(torch) }
    }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }
    AndroidView(factory = { previewView }, modifier = modifier)
}

/** Takes a photo into [file]. */
fun takePhoto(context: Context, capture: ImageCapture, file: File, onDone: (File) -> Unit, onError: (String) -> Unit) {
    val options = ImageCapture.OutputFileOptions.Builder(file).build()
    capture.takePicture(options, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) = onDone(file)
        override fun onError(exception: ImageCaptureException) = onError(exception.message ?: str(R.string.camera_couldn_t_take_the_photo))
    })
}

/** Photo picker that copies the chosen image to [target]. */
@Composable
fun rememberPhotoPicker(target: () -> File, onPicked: (File) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            val f = target()
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input -> f.outputStream().use { input.copyTo(it) } }
                onPicked(f)
            }
        }
    }
    return {
        launcher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

/** Simple on-device checks of an ID photo: brightness, glare and sharpness. */
data class PhotoQuality(val brightness: Float, val glare: Float, val sharpness: Float) {
    val wellLit: Boolean get() = brightness in 0.18f..0.92f
    val noGlare: Boolean get() = glare < 0.08f
    val sharp: Boolean get() = sharpness > 0.012f
    /** 0..1, used by the simulated review to decide whether a retake is needed. */
    val score: Float get() = listOf(wellLit, noGlare, sharp).count { it } / 3f

    companion object {
        suspend fun of(file: File): PhotoQuality = withContext(Dispatchers.Default) {
            val bmp = decodeSmall(file, 320) ?: return@withContext PhotoQuality(0f, 1f, 0f)
            val w = bmp.width; val h = bmp.height
            val px = IntArray(w * h)
            bmp.getPixels(px, 0, w, 0, 0, w, h)
            val lum = FloatArray(px.size) { i ->
                val c = px[i]
                (0.299f * ((c shr 16) and 0xff) + 0.587f * ((c shr 8) and 0xff) + 0.114f * (c and 0xff)) / 255f
            }
            val mean = lum.average().toFloat()
            val glare = lum.count { it > 0.97f }.toFloat() / lum.size
            var sum = 0.0; var sumSq = 0.0; var n = 0
            for (y in 1 until h - 1) for (x in 1 until w - 1) {
                val i = y * w + x
                val lap = 4 * lum[i] - lum[i - 1] - lum[i + 1] - lum[i - w] - lum[i + w]
                sum += lap; sumSq += lap * lap; n++
            }
            val variance = if (n == 0) 0.0 else sumSq / n - (sum / n) * (sum / n)
            PhotoQuality(mean, glare, sqrt(abs(variance)).toFloat())
        }

        fun decodeSmall(file: File, maxSide: Int): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0) return null
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxSide && bounds.outHeight / (sample * 2) >= maxSide) sample *= 2
            return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }
}

/** Opens the system contact picker for a phone number; no contacts permission is needed. */
@Composable
fun rememberContactPicker(onPicked: (name: String, phone: String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null,
            )?.use { c ->
                if (c.moveToFirst()) onPicked(c.getString(0) ?: "", c.getString(1) ?: "")
            }
        }
    }
    return {
        runCatching {
            launcher.launch(android.content.Intent(android.content.Intent.ACTION_PICK, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
        }
    }
}
