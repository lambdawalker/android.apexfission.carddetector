package com.apexfission.android.carddetector.ui.capture

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.apexfission.android.carddetector.R

/** Localized copy; call defaultCardCaptureStrings().copy(...) to override individual instructions. */
data class CardCaptureStrings(
    val title: String,
    val front: String,
    val back: String,
    val frontInstruction: String,
    val backInstruction: String,
    val frontSaved: String,
    val turnOver: String,
    val continueToBack: String,
    val review: String,
    val reviewInstruction: String,
    val use: String,
    val retakeFront: String,
    val retakeBack: String,
    val find: String,
    val focus: String,
    val ready: String,
    val manual: String,
    val auto: String,
    val shutter: String,
    val exit: String,
    val flash: String,
    val stepFront: String,
    val stepBack: String,
    val complete: String,
    val done: String,
    val autoToggle: String = "Toggle automatic capture",
)

/** Component. */
@Composable
fun defaultCardCaptureStrings() = CardCaptureStrings(
    title = stringResource(R.string.card_capture_title),
    front = stringResource(R.string.card_capture_front),
    back = stringResource(R.string.card_capture_back),
    frontInstruction = stringResource(R.string.card_capture_front_instruction),
    backInstruction = stringResource(R.string.card_capture_back_instruction),
    frontSaved = stringResource(R.string.card_capture_front_saved),
    turnOver = stringResource(R.string.card_capture_turn_over),
    continueToBack = stringResource(R.string.card_capture_continue),
    review = stringResource(R.string.card_capture_review),
    reviewInstruction = stringResource(R.string.card_capture_review_instruction),
    use = stringResource(R.string.card_capture_use),
    retakeFront = stringResource(R.string.card_capture_retake_front),
    retakeBack = stringResource(R.string.card_capture_retake_back),
    find = stringResource(R.string.card_capture_find),
    focus = stringResource(R.string.card_capture_focus),
    ready = stringResource(R.string.card_capture_ready),
    manual = stringResource(R.string.card_capture_manual),
    auto = stringResource(R.string.card_capture_auto),
    shutter = stringResource(R.string.card_capture_shutter),
    exit = stringResource(R.string.card_capture_exit),
    flash = stringResource(R.string.card_capture_flash),
    stepFront = stringResource(R.string.card_capture_step_front),
    stepBack = stringResource(R.string.card_capture_step_back),
    complete = stringResource(R.string.card_capture_complete),
    done = stringResource(R.string.card_capture_done),
    autoToggle = stringResource(R.string.card_capture_auto_toggle),
)
