package com.maumpeace.safeapp.ui.role

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.maumpeace.safeapp.R

class ChildFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // 새로고침을 위해 매번 뷰 생성
        return inflater.inflate(R.layout.fragment_child, container, false)
    }
}