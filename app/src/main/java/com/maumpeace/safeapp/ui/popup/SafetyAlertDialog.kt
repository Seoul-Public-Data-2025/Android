package com.maumpeace.safeapp.ui.popup

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.maumpeace.safeapp.R

class SafetyAlertDialog(private val context: Context) : AlertDialog(context) {

    private var soundPool: SoundPool? = null
    private var sirenSoundId: Int = 0
    private var isFlashOn = false

    override fun show() {
        val view: View = LayoutInflater.from(context).inflate(R.layout.dialog_safety_alert, null)
        setView(view)

        val btnConfirm = view.findViewById<Button>(R.id.btn_confirm)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel)
        val tvTitle = view.findViewById<TextView>(R.id.tv_dialog_title)
        val tvMessage = view.findViewById<TextView>(R.id.tv_dialog_message)

        btnConfirm.setOnClickListener {
            triggerSiren()
            toggleFlashlight(true)
            triggerVibration()
            Toast.makeText(context, "신고가 진행되었습니다.", Toast.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({
                setupSoundPool()
            }, 100)
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        super.show()
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun setupSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                sirenSoundId = sampleId
                soundPool?.play(sirenSoundId, 1f, 1f, 0, -1, 1f)
            } else {
                Toast.makeText(context, "사이렌 로딩 실패", Toast.LENGTH_SHORT).show()
            }
        }

        sirenSoundId = soundPool?.load(context, R.raw.siren, 1) ?: 0
    }

    private fun triggerSiren() {
        soundPool?.play(sirenSoundId, 1f, 1f, 0, -1, 1f)
    }

    private fun stopSiren() {
        soundPool?.stop(sirenSoundId)
        soundPool?.release()
        soundPool = null
    }

    private fun toggleFlashlight(turnOn: Boolean) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            cameraManager.setTorchMode(cameraId, turnOn)
            isFlashOn = turnOn

            if (turnOn) {
                Handler(Looper.getMainLooper()).postDelayed({
                    cameraManager.setTorchMode(cameraId, true)
                }, 1000)
            }

        } catch (e: CameraAccessException) {
            e.printStackTrace()
            Toast.makeText(context, "후레쉬 제어에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun triggerVibration() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}