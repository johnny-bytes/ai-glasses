package com.schloesser.masterthesis.presentation.selectSession

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.schloesser.masterthesis.R
import com.schloesser.masterthesis.data.base.ApiFactory
import com.schloesser.masterthesis.data.response.GetSessionsResponse
import com.schloesser.masterthesis.entity.ClassSession
import com.schloesser.masterthesis.presentation.extension.gone
import com.schloesser.masterthesis.presentation.extension.visible
import kotlinx.android.synthetic.main.layout_select_session.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SelectSessionDialog(private val callback: (session: ClassSession) -> Unit) : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.SelectSessionDialog)
    }

    private lateinit var fragmentView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.setCanceledOnTouchOutside(true);
        fragmentView = inflater.inflate(R.layout.layout_select_session, container, false)
        return fragmentView
    }

    override fun onStart() {
        super.onStart()
        init()
        dialog!!.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun display(supportFragmentManager: FragmentManager) {
        val dialog = supportFragmentManager.findFragmentByTag("select_session_dialog")
        if (dialog != null) {
            (dialog as SelectSessionDialog).dismiss()
        }
        showNow(supportFragmentManager, "select_session_dialog")
    }

    private lateinit var adapter: SelectSessionAdapter

    private fun init() {
        fragmentView.recyclerView.layoutManager = LinearLayoutManager(activity)

        adapter = SelectSessionAdapter(context!!) { session ->
            dismiss()
            callback(session)
        }

        fragmentView.recyclerView.adapter = adapter

        fragmentView.loadingIndicator.visible()

        ApiFactory.getInstance(context!!).api.getSessions().enqueue(object : Callback<GetSessionsResponse> {

            override fun onFailure(call: Call<GetSessionsResponse>, t: Throwable) {
                fragmentView.loadingIndicator.gone()
                fragmentView.txvStatus.visible()
                fragmentView.txvStatus.text = "Error: $t"
                t.printStackTrace()
            }

            override fun onResponse(call: Call<GetSessionsResponse>, response: Response<GetSessionsResponse>) {
                fragmentView.loadingIndicator.gone()

                if (response.body()?.results?.size ?: 0 > 0) {
                    fragmentView.txvStatus.gone()
                    adapter.setData(response.body()?.results!!)
                } else {
                    fragmentView.txvStatus.visible()
                    fragmentView.txvStatus.text = "No sessions available."
                }
            }

        })
    }
}
