package com.schloesser.masterthesis.presentation.selectSession

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.schloesser.masterthesis.R
import com.schloesser.masterthesis.entity.ClassSession
import kotlinx.android.synthetic.main.item_session.view.*

open class SelectSessionAdapter(context: Context, private var callback: (session: ClassSession) -> Unit) : RecyclerView.Adapter<SelectSessionAdapter.ViewHolder>() {

    private var data = ArrayList<ClassSession>()

    private fun getItem(position: Int) = data.getOrNull(position)

    fun setData(data: List<ClassSession>) {
        this.data.clear()
        this.data.addAll(data)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bindItem(item)
        }
    }

    private val inflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(inflater.inflate(R.layout.item_session, parent, false))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItem(session: ClassSession) {
            session.apply {
                itemView.txvName.text = name
                itemView.setOnClickListener { callback(this) }
            }
        }
    }
}