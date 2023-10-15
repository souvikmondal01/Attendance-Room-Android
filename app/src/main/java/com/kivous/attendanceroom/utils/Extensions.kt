package com.kivous.attendanceroom.utils

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.kivous.attendanceroom.R
import com.kivous.attendanceroom.utils.Constant.TAG

fun Activity.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Fragment.toast(msg: Any?) {
    msg?.let {
        Toast.makeText(requireContext(), msg.toString(), Toast.LENGTH_SHORT).show()
    }

}

fun View.gone() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.visible() {
    visibility = View.VISIBLE
}

fun logD(msg: Any) {
    Log.d(TAG, msg.toString())
}

fun Fragment.clipBoard(text: String) {
    val clipBoard: ClipboardManager =
        requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipBoard.text = text
}

fun Fragment.snackBar(text: String) {
    Snackbar.make(
        requireView(), text, Snackbar.LENGTH_SHORT
    ).show()
}

fun Fragment.hideKeyboard() {
    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(requireView().windowToken, 0)
}


fun Fragment.glideCircle(url: Any?, view: ImageView) {
    url?.let {
        Glide.with(requireContext()).load(url.toString()).placeholder(R.drawable.account_circle)
            .circleCrop().into(view)
    }
}

fun Fragment.glide(url: Any?, view: ImageView) {
    url?.let {
        Glide.with(requireContext()).load(url.toString()).centerCrop().into(view)
    }
}

fun Fragment.setImageViewTint(imageView: ImageView, attr: Int) {
    val typedValue = TypedValue()
    val theme = requireContext().theme
    theme.resolveAttribute(attr, typedValue, true)
    val colorOnSurface = typedValue.data
    val colorStateList = android.content.res.ColorStateList.valueOf(colorOnSurface)
    ImageViewCompat.setImageTintList(imageView, colorStateList)
}