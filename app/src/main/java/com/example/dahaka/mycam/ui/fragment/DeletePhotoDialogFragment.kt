package com.example.dahaka.mycam.ui.fragment

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import com.example.dahaka.mycam.R

class DeletePhotoDialogFragment : DialogFragment() {
    private val okListener: OkListener by lazy { context as OkListener }

    companion object {
        fun newInstance(): DeletePhotoDialogFragment {
            val frag = DeletePhotoDialogFragment()
            val args = Bundle()
            frag.arguments = args
            return frag
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertDialogBuilder = AlertDialog.Builder(requireContext())
                .setMessage(R.string.delete_photo)
                .setPositiveButton(R.string.yes, { _, _ ->
                    okListener.onOkButtonClicked()
                })
                .setNegativeButton(R.string.no, { dialog, _ ->
                    dialog?.dismiss()
                })
        return alertDialogBuilder.create()
    }

    interface OkListener {
        fun onOkButtonClicked()
    }
}