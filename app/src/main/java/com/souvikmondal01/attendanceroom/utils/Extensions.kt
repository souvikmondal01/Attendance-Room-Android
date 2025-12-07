package com.souvikmondal01.attendanceroom.utils

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
import com.souvikmondal01.attendanceroom.R
import com.souvikmondal01.attendanceroom.utils.Constant.TAG

fun Activity.toast(msg: String) =
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

fun Fragment.toast(msg: Any?) =
    msg?.let {
        Toast.makeText(requireContext(), msg.toString(), Toast.LENGTH_SHORT).show()
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

fun logD(msg: Any) = Log.d(TAG, msg.toString())

fun Fragment.clipBoard(text: String) {
    val clipBoard =
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

fun ImageView.glideCircle(context: Context, url: Any?) {
    url?.let {
        Glide.with(context).load(it.toString()).placeholder(R.drawable.outline_account_circle)
            .circleCrop().into(this)
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